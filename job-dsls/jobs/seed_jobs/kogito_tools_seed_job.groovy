import org.kie.jenkins.jobdsl.Constants

// definition of parameters

def javaToolEnv="KIE_JDK11"
def baseBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def javadk=Constants.JDK_TOOL

// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

// creation of folder where this seed job should run
folder("KIE")
folder("KIE/kogito")
folder("KIE/kogito/kie-tools")
def folderPath="KIE/kogito/kie-tools"

job("${folderPath}/a-seed-job-kie-tools") {

    description("this job creates needed Jenkins job for kie-tools in kogito folder")

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
            branch("${baseBranch}")
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
            targets("job-dsls/jobs/kie/kogito/kie_tools_prerelease_branch_UMB_trigger.groovy \n" +
                    "job-dsls/jobs/seed_jobs/kogito_tools_seed_job.groovy")
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
