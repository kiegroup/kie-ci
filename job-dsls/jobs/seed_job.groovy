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
            targets("job-dsls/jobs/**/DailyBuild_pipeline.groovy" +
                    "DailyBuild_prod_pipeline.groovy" +
                    "ProdTag_pipeline.groovy" +
                    "compile_downstream_build.groovy" +
                    "deploy_jobs.groovy" +
                    "downstream_pr_jobs.groovy" +
                    "kie_build_helper_jenkins_plugin_pr_job.groovy" +
                    "kie_docs_pr.groovy" +
                    "kie_jenkinsScripts_PR.groovy" +
                    "pr_jobs.groovy" +
                    "seed_job.groovy" +
                    "springboot_pr_job.groovy" +
                    "turtleTests.groovy")

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