import org.kie.jenkins.jobdsl.Constants

// definition of parameters

def javaToolEnv="KIE_JDK1_8"
def baseBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def javadk=Constants.JDK_VERSION

// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

job("a-seed-job-migration") {

    disabled()

    description("this job creates all needed seed jobs in the new Jenkins after migration ")

    label("kie-rhel7")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    scm {
        git {
            remote {
                github("${organization}/kie-jenkins-scripts")
            }
            branch ("${baseBranch}")
        }
    }

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${javaToolEnv}")
        preBuildCleanup()
    }

    steps {
        jobDsl {
            targets("job-dsls/jobs/kie/seed_jobs/kie_master_branch_seed_job.groovy\n" +
                    "job-dsls/jobs/kie/seed_jobs/kie_tools_seed_job.groovy\n" +
                    "job-dsls/jobs/kie/seed_jobs/kogito_docs_seed_job.groovy\n" +
                    "job-dsls/jobs/kie/seed_jobs/osbs_seed_job.groovy\n" +
                    "job-dsls/jobs/kie/seed_jobs/prod_seed_job.groovy\n" +
                    "job-dsls/jobs/kie/seed_jobs/prod_seed_job.groovy")
            useScriptText(false)
            sandbox(false)
            ignoreExisting(false)
            ignoreMissingFiles(false)
            failOnMissingPlugin(true)
            unstableOnDeprecation(true)
            removedJobAction('IGNORE')
            removedViewAction('IGNORE')
            removedConfigFilesAction('IGNORE')
            lookupStrategy('JENKINS_ROOT')
            additionalClasspath("job-dsls/src/main/groovy\n" +
                    "job-dsls/src/main/resources")
        }
    }
}