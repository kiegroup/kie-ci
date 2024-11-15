// pipeline DSL job to bump up the version of start.jbpm.org to the latest Final version (KIE_RELEASE_VERSION) after each release

def NEW_BRANCH = ''
def GH_ORG_UNIT = 'kiegroup'
def JENKINSFILE_REPO = 'kie-benchmarks'
def JENKINSFILE_PWD= 'kie-ci'
def JENKINSFILE_URL = "https://github.com/${GH_ORG_UNIT}/${JENKINSFILE_REPO}"
def JENKINSFILE_PATH = ".ci/jenkins/Jenkinsfile.branch"
def JENKINSFILE_BRANCH='main'


// creates folder if is not existing
folder("KIE")
folder("KIE/kie-tools")
folder("KIE/kie-tools/upgradeVersions")
def folderPath="KIE/kie-tools/upgradeVersions"

pipelineJob("${folderPath}/kie-benchmarks-create-new-branch") {

    description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |
                    |Pipeline job for creating a new branch in kie-benchmarks.
                    |""".stripMargin())

    parameters {
        stringParam("NEW_BRANCH", "${NEW_BRANCH}", "name of new branch to create in kie-benchmarks")
    }

    logRotator {
        numToKeep(5)
    }

    definition {
        cpsScm {
            scm {
                gitSCM {
                    userRemoteConfigs {
                        userRemoteConfig {
                            url(JENKINSFILE_URL)
                            credentialsId(JENKINSFILE_PWD)
                            name('')
                            refspec('')
                        }
                    }
                    branches {
                        branchSpec {
                            name("*/${JENKINSFILE_BRANCH}")
                        }
                    }
                    browser { }
                    doGenerateSubmoduleConfigurations(false)
                    gitTool('')
                }
            }
            scriptPath(JENKINSFILE_PATH)
        }
    }
}