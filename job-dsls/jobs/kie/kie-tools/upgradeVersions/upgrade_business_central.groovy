import org.kie.jenkins.jobdsl.Constants

def CURRENT_KIE_FINAL_VERSION= ""
def NEW_KIE_FINAL_VERSION= ""
def MVN_TOOL = Constants.MAVEN_TOOL
def JAVA_TOOL = Constants.JDK_TOOL
def KIECREDS ="kie_to_quay"
def BASE_BRANCH = Constants.MAIN_BRANCH
def GH_ORG_UNIT = "jboss-dockerfiles"
def JENKINSFILE_REPO = 'business-central'
def JENKINSFILE_PWD= 'kie-ci'
def JENKINSFILE_URL = "https://github.com/${GH_ORG_UNIT}/${JENKINSFILE_REPO}"
def JENKINSFILE_PATH = '.ci/jenkins/Jenkinsfile.update'


// creation of folder
folder("KIE")
folder("KIE/kie-tools")
folder("KIE/kie-tools/upgradeVersions")

def FOLDER_PATH="KIE/kie-tools/upgradeVersions"

pipelineJob("${FOLDER_PATH}/upgrade-community-docker-images") {

    description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |
                    |Community release pipeline
                    |""".stripMargin())

    parameters {
        stringParam("CURRENT_KIE_FINAL_VERSION", "${CURRENT_KIE_FINAL_VERSION}", "the current final kie-version in jboss-dockerfiles/business-central")
        stringParam("NEW_KIE_FINAL_VERSION", "${NEW_KIE_FINAL_VERSION}", "the new final kie-version in jboss-dockerfiles/business-central")
        wHideParameterDefinition {
            name('MVN_TOOL')
            defaultValue("${MVN_TOOL}")
            description('version of Maven')
        }
        wHideParameterDefinition {
            name('JAVA_TOOL')
            defaultValue("${JAVA_TOOL}")
            description('Please edit the commitMsg')
        }
        wHideParameterDefinition {
            name('KIECREDS')
            defaultValue("${KIECREDS}")
            description('credential for token to push to quay.io')
        }
    }

    logRotator {
        numToKeep(8)
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
                            name("*/${BASE_BRANCH}")
                        }
                    }
                    browser { }
                    doGenerateSubmoduleConfigurations(false)
                    gitTool('')
                }
            }
            scriptPath("${JENKINSFILE_PATH}")
        }
    }
}