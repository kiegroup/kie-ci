// pipeline DSL job to bump up the version of start.jbpm.org to the latest Final version (KIE_RELEASE_VERSION) after each release

def CURRENT_KIE_VERSION = ''
def NEW_KIE_VERSION= ''
def PROD_VERSION=''
def GH_ORG_UNIT = 'kiegroup'
def JENKINSFILE_REPO = 'start.jbpm.org'
def JENKINSFILE_PWD= 'kie-ci'
def JENKINSFILE_URL = "https://github.com/${GH_ORG_UNIT}/${JENKINSFILE_REPO}"
def JENKINSFILE_BRANCH='main'


// creates folder if is not existing
folder("KIE")
folder("KIE/kie-tools")
folder("KIE/kie-tools/upgradeVersions")
def folderPath="KIE/kie-tools/upgradeVersions"

pipelineJob("${folderPath}/upgrade-start.jbpm.org") {

    description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |
                    |Pipeline job for bumping up and add kie-versions or adding redhat product versions to start.jbpm.org.
                    |""".stripMargin())

    parameters {
        stringParam("CURRENT_KIE_VERSION", "${CURRENT_KIE_VERSION}", "the current kie-version in start.jbpm.org")
        stringParam("NEW_KIE_VERSION", "${NEW_KIE_VERSION}", "Final kie-version of latest community release")
        choiceParam("IS_PROD",["NO", "YES"],"Please select yest if you want to update start.bpm.org by a new prod version")
        stringParam("PROD_VERSION","${PROD_VERSION}","Edit the version for product (i.e. Enterprise 7.11.1: 7.52.0.Final-redhat-00008). Makes only sense if the previous parameter is YES)")
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
                            url("${JENKINSFILE_URL}")
                            credentialsId("${JENKINSFILE_PWD}")
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
            scriptPath(".ci/jenkins/Jenkinsfile.update")
        }
    }
}