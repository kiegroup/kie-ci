/**
 * This job is a workaround for the nightly-meta-pipeline (wich was disabled) and will be removed once the UMB messages are sent again<br>
 * Instead UMB message a simple CRON job is applied.
 */

import org.kie.jenkins.jobdsl.Constants

def javadk=Constants.JDK_TOOL
def AGENT_LABEL="kie-rhel && kie-mem512m"

def NEXT_PRODUCT_VERSION=Constants.NEXT_PROD_VERSION
def NEXT_PRODUCT_BRANCH='7.67.x'
def NEXT_PRODUCT_CONFIG_BRANCH="rhba/${NEXT_PRODUCT_BRANCH}"
// Use again after nightly builds from main branch again
// def NEXT_RHBA_VERSION_PREFIX=Constants.RHBA_VERSION_PREFIX
def NEXT_RHBA_VERSION_PREFIX='7.67.1.redhat-'

def KOGITO_NEXT_PRODUCT_VERSION='1.13.0'
def KOGITO_NEXT_PRODUCT_BRANCH='1.13.x'
def KOGITO_NEXT_PRODUCT_CONFIG_BRANCH="kogito/${KOGITO_NEXT_PRODUCT_BRANCH}"

def OPTAPLANNER_NEXT_PRODUCT_VERSION='8.13.0'

def NEXT_BLUE_PRODUCT_VERSION=Constants.BAMOE_NEXT_PROD_VERSION
def NEXT_BLUE_PRODUCT_BRANCH='7.67.x-blue'
def NEXT_BLUE_PRODUCT_CONFIG_BRANCH="bamoe/7.67.x-blue"
def NEXT_BLUE_RHBA_VERSION_PREFIX='7.67.2.redhat-'

def KOGITO_BLUE_NEXT_PRODUCT_VERSION='1.13.2.blue'
def KOGITO_BLUE_NEXT_PRODUCT_BRANCH='1.13.x-blue'
def KOGITO_BLUE_NEXT_PRODUCT_CONFIG_BRANCH="kogito/1.13.x-blue"

def SERVERLESS_LOGIC_NEXT_PRODUCT_BRANCH='main-integration-quarkus-lts'
def SERVERLESS_LOGIC_NEXT_PRODUCT_CONFIG_BRANCH="master"

// def SERVERLESS_LOGIC_CURRENT_PRODUCT_VERSION='1.28.0'
// def SERVERLESS_LOGIC_KOGITO_CURRENT_PRODUCT_VERSION='1.35.0'
// def SERVERLESS_LOGIC_DROOLS_CURRENT_PRODUCT_VERSION='8.35.0'
// def SERVERLESS_LOGIC_CURRENT_PRODUCT_BRANCH='1.35.x'
// def SERVERLESS_LOGIC_CURRENT_PRODUCT_CONFIG_BRANCH="openshift-serverless-logic/1.35.x"

def RHBOP_NEXT_PRODUCT_BRANCH='main'
def RHBOP_NEXT_PRODUCT_CONFIG_BRANCH='master'

// def RHBOP_CURRENT_PRODUCT_VERSION='8.33.0'
// def RHBOP_CURRENT_PRODUCT_BRANCH='8.33.x'
// def RHBOP_CURRENT_PRODUCT_CONFIG_BRANCH='rhbop/8.33.x'
// def RHBOP_CURRENT_DROOLS_VERSION='8.29.0'

// Should be uncommented and used with kogitoWithSpecDroolsNightlyStage once Next is set for RHPAM 7.14.0 (or main)
// def DROOLS_NEXT_PRODUCT_VERSION='8.13.0'

// Should be uncommented and used with kogitoWithSpecDroolsNightlyStage once Current is set for RHPAM 7.14.0
// def DROOLS_CURRENT_PRODUCT_VERSION= OPTAPLANNER_CURRENT_PRODUCT_VERSION

// Drools Ansible Integration
def DAI_NEXT_PRODUCT_BRANCH='main'
def DAI_NEXT_PRODUCT_CONFIG_BRANCH='master'

def DAI_CURRENT_PRODUCT_VERSION='1.0.0'
def DAI_CURRENT_PRODUCT_BRANCH='1.0.x'
def DAI_CURRENT_PRODUCT_CONFIG_BRANCH='drools-ansible-integration/1.0.x'
def DAI_CURRENT_DROOLS_VERSION='8.36.0'

def metaJob="""
pipeline{
    agent {
        label "$AGENT_LABEL"
    } 
    tools {
        jdk "$javadk"        
    }   
    // IMPORTANT: In case you trigger a new branch here, please create the same branch on build-configuration project
    
    stages {
        ${rhbaNightlyStage(NEXT_PRODUCT_VERSION, NEXT_PRODUCT_BRANCH, NEXT_PRODUCT_CONFIG_BRANCH)}
        ${kogitoNightlyStage(KOGITO_NEXT_PRODUCT_VERSION, KOGITO_NEXT_PRODUCT_BRANCH, OPTAPLANNER_NEXT_PRODUCT_VERSION, NEXT_PRODUCT_VERSION, NEXT_RHBA_VERSION_PREFIX, KOGITO_NEXT_PRODUCT_CONFIG_BRANCH)}
    
        // "current" prod nightlies removed, can be found in git history    

        // blue
        ${rhbaNightlyStage(NEXT_BLUE_PRODUCT_VERSION, NEXT_BLUE_PRODUCT_BRANCH, NEXT_BLUE_PRODUCT_CONFIG_BRANCH)}
        ${kogitoNightlyStage(KOGITO_BLUE_NEXT_PRODUCT_VERSION, KOGITO_BLUE_NEXT_PRODUCT_BRANCH, null, NEXT_BLUE_PRODUCT_VERSION, NEXT_BLUE_RHBA_VERSION_PREFIX, KOGITO_BLUE_NEXT_PRODUCT_CONFIG_BRANCH)}

        // Openshift Serverless Logic
        //{serverlessLogicNightlyStage(SERVERLESS_LOGIC_CURRENT_PRODUCT_BRANCH, SERVERLESS_LOGIC_CURRENT_PRODUCT_CONFIG_BRANCH, SERVERLESS_LOGIC_CURRENT_PRODUCT_VERSION, SERVERLESS_LOGIC_KOGITO_CURRENT_PRODUCT_VERSION, SERVERLESS_LOGIC_DROOLS_CURRENT_PRODUCT_VERSION)}
        ${serverlessLogicNightlyStage(SERVERLESS_LOGIC_NEXT_PRODUCT_BRANCH, SERVERLESS_LOGIC_NEXT_PRODUCT_CONFIG_BRANCH)}
    
        // RHBOP
        ${rhbopNightlyStage(RHBOP_NEXT_PRODUCT_BRANCH, RHBOP_NEXT_PRODUCT_CONFIG_BRANCH)}
        //{rhbopNightlyStage(RHBOP_CURRENT_PRODUCT_BRANCH, RHBOP_CURRENT_PRODUCT_CONFIG_BRANCH, RHBOP_CURRENT_PRODUCT_VERSION, RHBOP_CURRENT_DROOLS_VERSION)}

        // Drools Ansible Integration
        ${droolsAnsibleIntegrationNightlyStage(DAI_NEXT_PRODUCT_BRANCH, DAI_NEXT_PRODUCT_CONFIG_BRANCH)}
        ${droolsAnsibleIntegrationNightlyStage(DAI_CURRENT_PRODUCT_BRANCH, DAI_CURRENT_PRODUCT_CONFIG_BRANCH, DAI_CURRENT_PRODUCT_VERSION, DAI_CURRENT_DROOLS_VERSION, DAI_CURRENT_PRODUCT_BRANCH)}
    }
}
"""
// creates folder if is not existing
def folderPath="PROD"
folder(folderPath)

pipelineJob("${folderPath}/cron-meta-nightly-pipeline") {

    description("This job is a workaround for the nightly-meta-pipeline (wich was disabled) and will be removed once the UMB messages are sent again<br>\n" +
            "Instead UMB message a simple CRON job is applied.")

    parameters {
        wHideParameterDefinition {
            name('AGENT_LABEL')
            defaultValue("${AGENT_LABEL}")
            description('name of machine where to run this job')
        }
        wHideParameterDefinition {
            name('javadk')
            defaultValue("${javadk}")
            description('version of jdk')
        }
    }

    logRotator {
        numToKeep(5)
    }

    properties {
        pipelineTriggers {
            triggers {
                cron{
                    spec("H 15 * * *")
                }
            }
        }
    }

    definition {
        cps {
            script("${metaJob}")
            sandbox()
        }
    }
}

String rhbaNightlyStage(String version, String branch, String configBranch) {
    return """
        stage('trigger RHBA nightly job ${branch}') {
            steps {
                build job: 'rhba.nightly/${branch}', propagate: false, wait: true, parameters: [
                        [\$class: 'StringParameterValue', name: 'KIE_GROUP_DEPLOYMENT_REPO_URL', value: "\${env.BXMS_QE_NEXUS}/service/local/repositories/scratch-release-rhba-${getNexusFromVersion(version)}/content-compressed"],
                        [\$class: 'StringParameterValue', name: 'UMB_VERSION', value: '${getUMBFromVersion(version)}'],
                        [\$class: 'StringParameterValue', name: 'PRODUCT_VERSION', value: "${version}"],
                        [\$class: 'StringParameterValue', name: 'CONFIG_BRANCH', value: "${configBranch}"],
                        [\$class: 'BooleanParameterValue', name: 'SKIP_TESTS', value: true]
                ]
            }
        }
    """
}

String kogitoNightlyStage(String kogitoVersion, String kogitoBranch, String optaplannerVersion, String rhbaVersion, String rhbaVersionPrefix, String configBranch) {
    return """
        stage('trigger KOGITO nightly job ${kogitoVersion}') {
            steps {
                build job: 'kogito.nightly/${kogitoBranch}', propagate: false, wait: true, parameters: [
                        [\$class: 'StringParameterValue', name: 'RHBA_MAVEN_REPO_URL', value: "\${env.BXMS_QE_NEXUS}/content/repositories/rhba-${getNexusFromVersion(rhbaVersion)}-nightly-with-upstream"],
                        [\$class: 'StringParameterValue', name: 'RHBA_VERSION_PREFIX', value: '${rhbaVersionPrefix}'],
                        [\$class: 'StringParameterValue', name: 'RHBA_RELEASE_VERSION', value: '${getNexusFromVersion(rhbaVersion)}'],
                        [\$class: 'StringParameterValue', name: 'KOGITO_DEPLOYMENT_REPO_URL', value: "\${env.BXMS_QE_NEXUS}/service/local/repositories/scratch-release-kogito-${getNexusFromVersion(kogitoVersion)}/content-compressed"],
                        [\$class: 'StringParameterValue', name: 'UMB_VERSION', value: '${getUMBFromVersion(kogitoVersion)}'],
                        [\$class: 'StringParameterValue', name: 'PRODUCT_VERSION', value: '${kogitoVersion}'],${optaplannerVersion ? """
                        [\$class: 'StringParameterValue', name: 'OPTAPLANNER_PRODUCT_VERSION', value: '${optaplannerVersion}'],""" : ''}
                        [\$class: 'StringParameterValue', name: 'CONFIG_BRANCH', value: '${configBranch}'],
                        [\$class: 'BooleanParameterValue', name: 'SKIP_TESTS', value: true]
                ]
            }
        }
    """
}

String kogitoWithSpecDroolsNightlyStage(String kogitoVersion, String kogitoBranch, String droolsVersion, String optaplannerVersion, String rhbaVersion, String rhbaVersionPrefix, String configBranch) {
    return """
        stage('trigger KOGITO nightly job ${kogitoVersion}') {
            steps {
                build job: 'kogito.nightly/${kogitoBranch}', propagate: false, wait: true, parameters: [
                        [\$class: 'StringParameterValue', name: 'RHBA_MAVEN_REPO_URL', value: 'https://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8443/nexus/content/repositories/rhba-${getNexusFromVersion(rhbaVersion)}-nightly-with-upstream'],
                        [\$class: 'StringParameterValue', name: 'RHBA_VERSION_PREFIX', value: '${rhbaVersionPrefix}'],
                        [\$class: 'StringParameterValue', name: 'RHBA_RELEASE_VERSION', value: '${getNexusFromVersion(rhbaVersion)}'],
                        [\$class: 'StringParameterValue', name: 'KOGITO_DEPLOYMENT_REPO_URL', value: 'https://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8443/nexus/service/local/repositories/scratch-release-kogito-${getNexusFromVersion(kogitoVersion)}/content-compressed'],
                        [\$class: 'StringParameterValue', name: 'UMB_VERSION', value: '${getUMBFromVersion(kogitoVersion)}'],
                        [\$class: 'StringParameterValue', name: 'PRODUCT_VERSION', value: '${kogitoVersion}'],
                        [\$class: 'StringParameterValue', name: 'DROOLS_PRODUCT_VERSION', value: '${droolsVersion}'],
                        [\$class: 'StringParameterValue', name: 'OPTAPLANNER_PRODUCT_VERSION', value: '${optaplannerVersion}'],
                        [\$class: 'StringParameterValue', name: 'CONFIG_BRANCH', value: '${configBranch}'],
                        [\$class: 'BooleanParameterValue', name: 'SKIP_TESTS', value: true]
                ]
            }
        }
    """
}

String serverlessLogicNightlyStage(String branch, String configBranch, String productVersion = '', String kogitoVersion = '', String droolsVersion = '') {
    // when kogitoVersion or droolsVersion are empty, the Jenkins job will get them from the current branch pom (mostly main)
    return """
        stage('trigger Serverless Logic nightly job ${branch}') {
            steps {
                build job: 'kogito.nightly/${branch}', propagate: false, wait: true, parameters: [
                        [\$class: 'StringParameterValue', name: 'UMB_VERSION', value: '${getUMBFromVersion(productVersion)}'],
                        [\$class: 'StringParameterValue', name: 'PRODUCT_VERSION', value: '${kogitoVersion}'],
                        [\$class: 'StringParameterValue', name: 'DROOLS_PRODUCT_VERSION', value: '${droolsVersion}'],
                        [\$class: 'StringParameterValue', name: 'CONFIG_BRANCH', value: '${configBranch}'],
                        [\$class: 'BooleanParameterValue', name: 'SKIP_TESTS', value: true]
                ]
            }
        }
    """
}

String rhbopNightlyStage(String branch, String configBranch, String version = '', String droolsVersion = '') {
    // when version or droolsVersion are empty, the Jenkins job will get them from the main branch pom
    return """
        stage('trigger RHBOP nightly job ${branch}') {
            steps {
                build job: 'rhbop.nightly/${branch}', propagate: false, wait: true, parameters: [
                        [\$class: 'StringParameterValue', name: 'KIE_GROUP_DEPLOYMENT_REPO_URL', value: 'https://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8443/nexus/service/local/repositories/scratch-release-rhbop-${getNexusFromVersion(version)}/content-compressed'],
                        [\$class: 'StringParameterValue', name: 'UMB_VERSION', value: '${getUMBFromVersion(version)}'],
                        [\$class: 'StringParameterValue', name: 'PRODUCT_VERSION', value: "${version}"],
                        [\$class: 'StringParameterValue', name: 'DROOLS_PRODUCT_VERSION', value: '${droolsVersion}'],
                        [\$class: 'StringParameterValue', name: 'CONFIG_BRANCH', value: "${configBranch}"],
                ]
            }
        }
    """
}

String droolsAnsibleIntegrationNightlyStage(String branch, String configBranch, String version = '', String droolsVersion = '', String definitionFileBranch = 'main') {
    // when version or droolsVersion are empty, the Jenkins job will get them from the main branch pom
    return """
        stage('trigger Drools Ansible Integration nightly job ${branch}') {
            steps {
                build job: 'drools-ansible-integration.nightly/${branch}', propagate: false, wait: true, parameters: [
                        [\$class: 'StringParameterValue', name: 'NEXUS_DEPLOYMENT_REPO_URL', value: 'https://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8443/nexus/service/local/repositories/scratch-release-drools-ansible-integration-${getNexusFromVersion(version)}/content-compressed'],
                        [\$class: 'StringParameterValue', name: 'PRODUCT_VERSION', value: "${version}"],
                        [\$class: 'StringParameterValue', name: 'DROOLS_PRODUCT_VERSION', value: '${droolsVersion}'],
                        [\$class: 'StringParameterValue', name: 'CONFIG_BRANCH', value: "${configBranch}"],
                        [\$class: 'StringParameterValue', name: 'DEFINITION_FILE_OWNER', value: 'kiegroup'],
                        [\$class: 'StringParameterValue', name: 'DEFINITION_FILE_BRANCH', value: "${definitionFileBranch}"],
                ]
            }
        }
    """
}

String getUMBFromVersion(def version) {
    // if empty return main branch too
    if (isMainBranchVersion(version) || version == '') {
        return Constants.MAIN_BRANCH
    }
    def matcher = version =~ /(\d*)\.(\d*)\.?/
    return "${matcher[0][1]}${matcher[0][2]}${getBlueSuffix(version, '')}"
}

String getNexusFromVersion(def version) {
    // if empty return main branch too
    if (isMainBranchVersion(version) || version == '') {
        return Constants.MAIN_BRANCH
    }
    def matcher = version =~ /(\d*)\.(\d*)\.?/
    return "${matcher[0][1]}.${matcher[0][2]}${getBlueSuffix(version, '-')}"
}

String getBlueSuffix(String version, String separator) {
    return version.endsWith('blue') ? separator + 'blue' : ''
}

boolean isMainBranchVersion(String version) {
    return [Constants.MAIN_BRANCH_PROD_VERSION, Constants.KOGITO_MAIN_BRANCH_PROD_VERSION, Constants.RHBOP_MAIN_BRANCH_PROD_VERSION].contains(version)
}
