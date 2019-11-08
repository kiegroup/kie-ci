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

job("a-seed-job-${kieMainBranch}") {

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
            targets("job-dsls/jobs/**/DailyBuild_pipeline.groovy " +
                    "job-dsls/jobs/**/DailyBuild_prod_pipeline.groovy " +
                    "job-dsls/jobs/**/ProdTag_pipeline.groovy " +
                    "job-dsls/jobs/**/compile_downstream_build.groovy " +
                    "job-dsls/jobs/**/deploy_jobs.groovy " +
                    "job-dsls/jobs/**/downstream_pr_jobs.groovy " +
                    "job-dsls/jobs/**/kie_build_helper_jenkins_plugin_pr_job.groovy " +
                    "job-dsls/jobs/**/kie_docs_pr.groovy " +
                    "job-dsls/jobs/**/kie_jenkinsScripts_PR.groovy " +
                    "job-dsls/jobs/**/pr_jobs.groovy " +
                    "job-dsls/jobs/**/seed_job.groovy " +
                    "job-dsls/jobs/**/springboot_pr_job.groovy " +
                    "job-dsls/jobs/**/turtleTests.groovy")

            useScriptText(false)
            sandbox(false)
            ignoreExisting(false)
            ignoreMissingFiles(false)
            failOnMissingPlugin(true)
            unstableOnDeprecation(true)
            removedJobAction('DISABLE')
            removedViewAction('IGNORE')
            //removedConfigFilesAction('IGNORE')
            lookupStrategy('SEED_JOB')
            additionalClasspath("job-dsls/src/main/groovy")
        }
    }
}