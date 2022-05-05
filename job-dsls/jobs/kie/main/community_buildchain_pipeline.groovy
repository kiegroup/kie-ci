import org.kie.jenkins.jobdsl.Constants
import org.kie.jenkins.jobdsl.templates.additionalTests.*

def KIE_VERSION=Constants.KIE_PREFIX + ".Final"
def BASE_BRANCH=Constants.BRANCH
def RELEASE_BRANCH="r" + Constants.KIE_PREFIX + ".Final"
def M2DIR = Constants.LOCAL_MVN_REP
def MAVEN_OPTS="-Xms1g -Xmx3g"
def COMMIT_MSG="Upgraded_version_to_"
def JAVADK=Constants.JDK_TOOL
def MVN_TOOL=Constants.MAVEN_TOOL
// number of build that has stored the binaries (*tar.gz) that are wanted to upload
def BUILD_NR=1
def TOOLS_VER="7.46.0.Final"
def AGENT_LABEL='kie-releases'
// directory where the zip with all binaries is stored
def ZIP_DIR="community-deploy-dir"
// download URL of jboss-eap for the additional tests
def EAP7_DOWNLOAD_URL=Constants.EAP7_DOWNLOAD_URL
def GH_ORG_UNIT = Constants.GITHUB_ORG_UNIT
def JENKINSFILE_REPO = 'droolsjbpm-build-bootstrap'
def JENKINSFILE_PWD= 'kie-ci'
def JENKINSFILE_URL = "https://github.com/${GH_ORG_UNIT}/${JENKINSFILE_REPO}"
def JENKINSFILE_PATH = '.ci/jenkins/Jenkinsfile.release'
def NEXUS_URL = "https://repository.jboss.org/nexus/content/groups/kie-group"
def SETTINGS_XML = '3f317dd7-4d08-4ee4-b9bb-969c309e782c'


// creation of folder
folder("KIE")
folder("KIE/${BASE_BRANCH}")
folder("KIE/${BASE_BRANCH}/release")

def FOLDER_PATH="KIE/${BASE_BRANCH}/release"

pipelineJob("${FOLDER_PATH}/kie-release") {

    description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |
                    |Community release pipeline
                    |""".stripMargin())

    parameters {
        stringParam("KIE_VERSION", "${KIE_VERSION}", "Version of KIE for the current release")
        stringParam("BASE_BRANCH", "${BASE_BRANCH}", "Branch that the release is based on. Mostly: main")
        stringParam("RELEASE_BRANCH", "${RELEASE_BRANCH}", "Please edit name of the releaseBranch - i.e. r7.51.0.Final ")
        stringParam("GH_ORG_UNIT", "${GH_ORG_UNIT}", "Please edit the name of organization ")
        stringParam("TOOLS_VER", "${TOOLS_VER}", "Please edit the latest stable version of droolsjbpm-tools<br>Important: needed for the jbpm-installer creation. Latest stable version is 7.46.0.Final.")
        choiceParam("RUN_BUILD",["YES", "NO"],"Please select if<br>you want to do a new build = YES<br>a new build is not required and artifacts are already uploaded to Nexus = NO ")
        wHideParameterDefinition {
            name('MAVEN_OPTS')
            defaultValue("${MAVEN_OPTS}")
            description('Please edit the Maven options')
        }
        wHideParameterDefinition {
            name('COMMIT_MSG')
            defaultValue("${COMMIT_MSG}\${KIE_VERSION}")
            description('Please edit the commitMsg')
        }
        wHideParameterDefinition {
            name('M2DIR')
            defaultValue("${M2DIR}")
            description('Path to .m2/repository')
        }
        wHideParameterDefinition {
            name('BUILD_NR')
            defaultValue("${BUILD_NR}")
            description('')
        }
        wHideParameterDefinition {
            name('AGENT_LABEL')
            defaultValue("${AGENT_LABEL}")
            description('name of machine where to run this job')
        }
        wHideParameterDefinition {
            name('MVN_TOOL')
            defaultValue("${MVN_TOOL}")
            description('version of maven')
        }
        wHideParameterDefinition {
            name('JAVADK')
            defaultValue("${JAVADK}")
            description('version of jdk')
        }
        wHideParameterDefinition {
            name('ZIP_DIR')
            defaultValue("${ZIP_DIR}")
            description('Where is the zipped file to upload?')
        }
        wHideParameterDefinition {
            name('NEXUS_URL')
            defaultValue("${NEXUS_URL}")
            description('URL of Nexus server')
        }
        wHideParameterDefinition {
            name('SETTINGS_XML')
            defaultValue("${SETTINGS_XML}")
            description('Right settings.xml for releases')
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
// Creates jbpmTestCoverageMatrix job
def jobDefinition1 = matrixJob("${FOLDER_PATH}/jbpmTestCoverageMatrix")
JbpmTestCoverageMatrix.addDeployConfiguration(jobDefinition1,
        kieVersion = KIE_VERSION,
        jdkVersion = JAVADK,
        mvnTool =  MVN_TOOL,
        nexusUrl = NEXUS_URL,
        settingsXml = SETTINGS_XML)

// Creates kieWbTestMatrix job
def jobDefinition3 = matrixJob("${FOLDER_PATH}/kieWbTestMatrix")
KieWbTestMatrix.addDeployConfiguration(jobDefinition3,
        kieVersion = KIE_VERSION,
        jdkVersion = JAVADK,
        mvnTool =  MVN_TOOL,
        nexusUrl = NEXUS_URL,
        settingsXml = SETTINGS_XML)

// Creates kieWbTestServer job
def jobDefinition4 = matrixJob("${FOLDER_PATH}/kieServerMatrix")
KieServerMatrix.addDeployConfiguration(jobDefinition4,
        kieVersion = KIE_VERSION,
        jdkVersion = JAVADK,
        mvnTool =  MVN_TOOL,
        downloadUrl = EAP7_DOWNLOAD_URL,
        nexusUrl = NEXUS_URL,
        settingsXml = SETTINGS_XML)