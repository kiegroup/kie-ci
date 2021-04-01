import org.kie.jenkins.jobdsl.Constants

// definition of parameters

def javaToolEnv="KIE_JDK1_8"
def baseBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def javadk=Constants.JDK_VERSION

// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

// creation of folder where this seed job should run
folder("OSBS")
def folderPath = "OSBS"
job("${folderPath}/a-seed-job-OSBS") {

    description("this job creates all needed Jenkins jobs in OSBS folder ")

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
            targets("job-dsls/jobs/osbs/*.groovy")
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