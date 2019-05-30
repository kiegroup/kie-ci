import org.kie.jenkins.jobdsl.Constants

// definition of parameters

def javaToolEnv="KIE_JDK1_8"
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
            targets("job-dsls/jobs/**/bxms_patch_tools_pr.groovy\n" +
                    "job-dsls/jobs/**/kie_docs_pr.groovy\n" +
                    "job-dsls/jobs/**/kie_build_helper_jenkins_plugin_pr_job.groovy\n" +
                    "job-dsls/jobs/**/pr_jobs.groovy\n" +
                    "job-dsls/jobs/**/downstream_pr_jobs.groovy\n" +
                    "job-dsls/jobs/**/deploy_jobs.groovy\n" +
                    "job-dsls/jobs/**/compile_downstream_build.groovy\n" +
                    "job-dsls/jobs/**/sonarcloud_daily.groovy\n" +
                    "job-dsls/jobs/**/springboot_pr_job.groovy\n" +
                    "job-dsls/jobs/**/kogito.groovy\n" +
                    "job-dsls/jobs/**/kie_dailyBuild_pipeline.groovy\n" +
                    "job-dsls/jobs/**/kie_release_jobs.groovy\n" +
                    "job-dsls/jobs/**/zanata*.groovy")
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