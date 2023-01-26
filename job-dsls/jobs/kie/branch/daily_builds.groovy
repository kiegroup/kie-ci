import org.kie.jenkins.jobdsl.Constants
import org.kie.jenkins.jobdsl.templates.additionalTests.*

def MVN_TOOL=Constants.MAVEN_TOOL
def KIE_VERSION=Constants.KIE_PREFIX
def BASE_BRANCH=Constants.BRANCH
def GH_ORG_UNIT=Constants.GITHUB_ORG_UNIT
def AGENT_LABEL="rhos-01 && kie-rhel7-pipeline && kie-mem24g && !built-in"
def JENKINSFILE_REPO = 'droolsjbpm-build-bootstrap'
def JENKINSFILE_PWD= 'kie-ci'
def JENKINSFILE_PATH = '.ci/jenkins/Jenkinsfile.daily'
def JENKINSFILE_URL = "https://github.com/${GH_ORG_UNIT}/${JENKINSFILE_REPO}"
def M2DIR = Constants.LOCAL_MVN_REP
def URL_EXTENDED_PATH = "/content/repositories/kieAllBuild-${BASE_BRANCH}"
def SETTINGS_XML='771ff52a-a8b4-40e6-9b22-d54c7314aa1e'


def final DEFAULTS = [
        JDK_VERSION : 'kie-jdk11.0.15',
        ADDITIONAL_MAVEN_FLAG : ''
]

def final DAILY_CONFIGS = [
        'jdk11'   : [],
        'jdk8'   : [ JDK_VERSION  : 'kie-jdk1.8' ]
]

for (dailyConfig in DAILY_CONFIGS) {
    Closure<Object> get = { String key -> dailyConfig.value[key] ?: DEFAULTS[key] }

    String BUILD_NAME = dailyConfig.key
    String JDK_VERSION = get('JDK_VERSION')
    String ADDITIONAL_MAVEN_FLAG = get('ADDITIONAL_MAVEN_FLAG')

    // creation of folder
    folder("KIE")
    folder("KIE/${BASE_BRANCH}")
    folder("KIE/${BASE_BRANCH}/daily-build-${BUILD_NAME}")

    def FOLDER_PATH= "KIE/${BASE_BRANCH}/daily-build-${BUILD_NAME}"

    pipelineJob("${FOLDER_PATH}/${BUILD_NAME}-db-${BASE_BRANCH}") {

        description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                        |
                        |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                        |
                        |Pipeline job for running "${BUILD_NAME}" build.
                        |""".stripMargin())

        parameters {
            stringParam("KIE_VERSION", "${KIE_VERSION}", "Version of KIE. This will be usually set automatically by the parent pipeline job. ")
            stringParam("BASE_BRANCH", "${BASE_BRANCH}", "KIE branch. This will be usually set automatically by the parent pipeline job. ")
            stringParam("GH_ORG_UNIT", "${GH_ORG_UNIT}", "Name of organization. This will be usually set automatically by the parent pipeline job. ")
            stringParam("ADDITIONAL_MAVEN_FLAG", "${ADDITIONAL_MAVEN_FLAG}", "if there are any additional flags to the mvn command, please edit them here")
            wHideParameterDefinition {
                name('AGENT_LABEL')
                defaultValue(AGENT_LABEL)
                description('name of machine where to run this job')
            }
            wHideParameterDefinition {
                name('MVN_TOOL')
                defaultValue(MVN_TOOL)
                description('version of maven')
            }
            wHideParameterDefinition {
                name('JDK_VERSION')
                defaultValue(JDK_VERSION)
                description('version of jdk')
            }
            wHideParameterDefinition {
                name('BUILD_NAME')
                defaultValue(BUILD_NAME)
                description('name of daily build')
            }
            wHideParameterDefinition {
                name('M2DIR')
                defaultValue(M2DIR)
                description('where maven artifacts are stored')
            }
            wHideParameterDefinition {
                name('NEXUS_URL')
                defaultValue("\${BXMS_QE_NEXUS}${URL_EXTENDED_PATH}")
                description('URL of Nexus server')
            }
            wHideParameterDefinition {
                name('SETTINGS_XML')
                defaultValue(SETTINGS_XML)
                description('Right settings.xml for daily builds')
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
                                url(JENKINSFILE_URL)
                                credentialsId(JENKINSFILE_PWD)
                                name('')
                                refspec('')
                            }
                        }
                        branches {
                            branchSpec {
                                name("*/${BASE_BRANCH}")
                            }
                        }
                        browser {}
                        doGenerateSubmoduleConfigurations(false)
                        gitTool('')
                    }
                }
                scriptPath(JENKINSFILE_PATH)
            }
        }
    }
    if ( "${BUILD_NAME}" == "jdk11" ) {
        JDK_VERSION = "kie-jdk11.0.15"
    } else {
        JDK_VERSION = "kie-jdk1.8"
    }

    // Creates jbpmTestCoverageMatrix job
    def jobDefinition1 = matrixJob("${FOLDER_PATH}/jbpmTestCoverageMatrix")
    JbpmTestCoverageMatrix.addDeployConfiguration(jobDefinition1,
            kieVersion = KIE_VERSION,
            jdkVersion = JDK_VERSION,
            mvnTool =  MVN_TOOL,
            nexusUrl = "\${BXMS_QE_NEXUS}${URL_EXTENDED_PATH}",
            settingsXml = SETTINGS_XML)

    // Creates jbpmContainerTestMatrix job
    def jobDefinition2 = matrixJob("${FOLDER_PATH}/jbpmContainerTestMatrix")
    JbpmContainerTestMatrix.addDeployConfiguration(jobDefinition2,
            kieVersion = KIE_VERSION,
            jdkVersion = JDK_VERSION,
            mvnTool =  MVN_TOOL,
            nexusUrl = "\${BXMS_QE_NEXUS}${URL_EXTENDED_PATH}",
            settingsXml = SETTINGS_XML)

    // Creates kieWbTestMatrix job
    def jobDefinition3 = matrixJob("${FOLDER_PATH}/kieWbTestMatrix")
    KieWbTestMatrix.addDeployConfiguration(jobDefinition3,
            kieVersion = KIE_VERSION,
            jdkVersion = JDK_VERSION,
            mvnTool =  MVN_TOOL,
            nexusUrl = "\${BXMS_QE_NEXUS}${URL_EXTENDED_PATH}",
            settingsXml = SETTINGS_XML)

    // Creates kieServerMatrix job
    def jobDefinition4 = matrixJob("${FOLDER_PATH}/kieServerMatrix")
    KieServerMatrix.addDeployConfiguration(jobDefinition4,
            kieVersion = KIE_VERSION,
            jdkVersion = JDK_VERSION,
            mvnTool =  MVN_TOOL,
            downloadUrl = "\${EAP_DOWNLOAD_URL}7/7.4.8/jboss-eap-7.4.8.zip",
            nexusUrl = "\${BXMS_QE_NEXUS}${URL_EXTENDED_PATH}",
            settingsXml = SETTINGS_XML)
}