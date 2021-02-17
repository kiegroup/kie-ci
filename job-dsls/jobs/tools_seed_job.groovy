import org.kie.jenkins.jobdsl.Constants

// definition of parameters

def javaToolEnv="KIE_JDK1_8"
def kieMainBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def javadk=Constants.JDK_VERSION

// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

// create seed job script

def seedJob='''#!/bin/bash -e
cd job-dsls
./gradlew clean test'''

job("a-tools-seed-job") {

    description("this job creates all needed Jenkins jobs")

    label("kie-rhel7")

    logRotator {
        numToKeep(5)
    }

    jdk("${javadk}")

    scm {
        git {
            remote {
                github("${organization}/kie-jenkins-scripts")
            }
            branch ("${kieMainBranch}")
        }
    }

    /*triggers {
        scm('H/15 * * * *')
    }*/

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${javaToolEnv}")
        preBuildCleanup()
    }

    steps {
        shell(seedJob)

        jobDsl {
            targets("job-dsls/jobs/**/slave_image_build.groovy\n" +
                    "job-dsls/jobs/**/slave_image_build_PR_test.groovy\n" +
                    "job-dsls/jobs/**/tools_provisioning*.groovy\n" +
                    "job-dsls/jobs/**/tools_seed_job.groovy")
            useScriptText(false)
            sandbox(false)
            ignoreExisting(false)
            ignoreMissingFiles(false)
            failOnMissingPlugin(true)
            unstableOnDeprecation(true)
            removedJobAction('IGNORE')
            removedViewAction('DELETE')
            //removedConfigFilesAction('IGNORE')
            lookupStrategy('SEED_JOB')
            additionalClasspath("job-dsls/src/main/groovy")
        }
    }
}