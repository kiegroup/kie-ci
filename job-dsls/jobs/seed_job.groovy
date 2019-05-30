import org.kie.jenkins.jobdsl.Constants

// definition of parameters

def javaToolEnv="KIE_JDK1_8"
def mvnToolEnv="KIE_MAVEN_3_5_4"
def mvnHome="${mvnToolEnv}_HOME"
def javaHome="${javaToolEnv}_HOME"
def mvnOpts="-Xms1g -Xmx3g"
def m2Dir="\$HOME/.m2/repository"
def kieMainBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def javadk=Constants.JDK_VERSION

// creation of folder
folder("KIE")
folder("KIE/${kieMainBranch}")

def folderPath="KIE/${kieMainBranch}"

// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

// create seed job script

def seedJob='''#!/bin/bash -e
cd job-dsls
./gradlew clean test'''

job("${folderPath}/a-seed-job-${kieMainBranch}") {

    description("this job creates all needed Jenkins jobs")

    label("kieci-02-docker")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    scm {
        git {
            remote {
                github("${organization}/kie-jenkins-scripts")
            }
            branch ("${kieMainBranch}")
            extensions {
                relativeTargetDirectory("scripts/kie-jenkins-scripts")
            }

        }
    }

    triggers {
        scm('H/15 * * * *')
    }

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
        preBuildCleanup()
    }

    steps {
        environmentVariables {
            envs(MAVEN_OPTS: "${mvnOpts}", MAVEN_HOME: "\$${mvnHome}", JAVA_HOME: "\$${javaHome}", MAVEN_REPO_LOCAL: "${m2Dir}", JENKINS_SETTINGS_XML_FILE: "\$SETTINGS_XML_FILE", PATH: "\$${mvnHome}/bin:\$PATH")
        }
        shell(seedJob)

        jobDsl {
            targets("job-dsls/jobs/**/*.groovy")
            useScriptText(false)
            sandbox(false)
            ignoreExisting(false)
            ignoreMissingFiles(false)
            failOnMissingPlugin(true)
            unstableOnDeprecation(true)
            removedJobAction('DELETE')
            removedViewAction('DELETE')
            //removedConfigFilesAction('IGNORE')
            lookupStrategy('SEED_JOB')
            additionalClasspath("job-dsls/src/main/groovy")
        }
    }
}