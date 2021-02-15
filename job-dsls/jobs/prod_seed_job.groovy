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

job("prod-seed-job") {

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
            targets("job-dsls/jobs/**/prod_replace_shared_libraries.groovy\n" +
                    "job-dsls/jobs/**/prod_rhba_prod_branch.groovy\n" +
                    "job-dsls/jobs/**/prod_rhba_replace_version.groovy\n" +
                    "job-dsls/jobs/**/prod_shared_libraries_new_branch.groovy\n" +
                    "job-dsls/jobs/**/prod_offline_repo_builder.groovy\n" +
                    "job-dsls/jobs/**/prod_rhba_properties_generator.groovy\n" +
                    "job-dsls/jobs/**/prod_rhba_staging.groovy\n" +
                    "job-dsls/jobs/**/prod_kogito_properties_generator.groovy\n" +
                    "job-dsls/jobs/**/prod_seed_job.groovy")
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
