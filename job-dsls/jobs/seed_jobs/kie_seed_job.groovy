import org.kie.jenkins.jobdsl.Constants

// definition of parameters

def javaToolEnv="KIE_JDK1_8"
def baseBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def javadk=Constants.JDK_TOOL

// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

// creation of folder where this seed job should run
folder("KIE")
folder("KIE/${baseBranch}")
def folderPath = "KIE/${baseBranch}"
job("${folderPath}/a-seed-job-${baseBranch}") {

    description("this job creates all needed Jenkins jobs for the ${baseBranch}-branch ")

    label("kie-rhel7 && kie-mem8g && !built-in")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    scm {
        git {
            remote {
                github("${organization}/kie-ci")
            }
            branch ("${baseBranch}")
        }
    }

    triggers {
        gitHubPushTrigger()
    }

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${javaToolEnv}")
        preBuildCleanup()
    }

    steps {
        jobDsl {
            targets("job-dsls/jobs/kie/branch/*.groovy\n" +
                    "job-dsls/jobs/seed_jobs/kie_seed_job.groovy")
            useScriptText(false)
            sandbox(false)
            ignoreExisting(false)
            ignoreMissingFiles(false)
            failOnMissingPlugin(true)
            unstableOnDeprecation(true)
            removedJobAction('DELETE')
            removedViewAction('IGNORE')
            removedConfigFilesAction('IGNORE')
            lookupStrategy('JENKINS_ROOT')
            additionalClasspath("job-dsls/src/main/groovy\n" +
                    "job-dsls/src/main/resources")
        }
    }
}