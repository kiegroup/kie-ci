import org.kie.jenkins.jobdsl.Constants

// definition of parameters

def javaToolEnv="KIE_JDK1_8"
def baseBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def javadk=Constants.JDK_TOOL

// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

// creation of folder where this seed job should run
folder("KIE")
folder("KIE/kie-tools")
def folderPath="KIE/kie-tools"

job("${folderPath}/a-seed-job-kie-tools") {

    description("this job creates all needed Jenkins jobs in kie-tools folder ")

    label("kie-rhel8 && kie-mem8g && !built-in")

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

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${javaToolEnv}")
        preBuildCleanup()
    }

    triggers {
        gitHubPushTrigger()
    }

    steps {
        jobDsl {
            targets("job-dsls/jobs/kie/kie-tools/*.groovy\n" +
                    "job-dsls/jobs/kie/kie-tools/upgradeVersions/*.groovy\n" +
                    "job-dsls/jobs/seed_jobs/kie_tools_seed_job.groovy")
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