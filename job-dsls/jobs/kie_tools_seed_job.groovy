import org.kie.jenkins.jobdsl.Constants

// definition of parameters

def javaToolEnv="KIE_JDK1_8"
def kieMainBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def javadk=Constants.JDK_VERSION
def labelName="kie-rhel7"

// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

// create seed job script

def seedJob='''#!/bin/bash -e
cd job-dsls
./gradlew clean test'''

job("a-kie-tools-seed-job") {

    description("this job creates all needed Jenkins jobs for kie-tools")

    label(labelName)

    logRotator {
        numToKeep(5)
    }

    jdk(javadk)

    scm {
        git {
            remote {
                github("${organization}/kie-jenkins-scripts")
            }
            branch ("${kieMainBranch}")
        }
    }

    triggers {
        scm('H/15 * * * *')
    }

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${javaToolEnv}")
        preBuildCleanup()
    }

    steps {
        shell(seedJob)

        jobDsl {
            targets("job-dsls/jobs/**/send_UMB_trigger_after_version_upgrade.groovy \n" +
                    "job-dsls/jobs/**/kieAll_meta_pipeline.groovy \n" +
                    "job-dsls/jobs/**/deploy_development_version.groovy \n" +
                    "job-dsls/jobs/**/kie_docker_ui_webapp.groovy \n" +
                    "job-dsls/jobs/**/kie_tools_seed_job.groovy")
            useScriptText(false)
            sandbox(false)
            ignoreExisting(false)
            ignoreMissingFiles(false)
            failOnMissingPlugin(true)
            unstableOnDeprecation(true)
            removedJobAction('IGNORE')
            removedConfigFilesAction('IGNORE')
            lookupStrategy('SEED_JOB')
            additionalClasspath("job-dsls/src/main/groovy")
        }
    }
}