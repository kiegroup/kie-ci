/**
 * This job is a workaround for the nightly-meta-pipeline (wich was disabled) and will be removed once the UMB messages are sent again<br>
 * Instead UMB message a simple CRON job is applied.
 */

import org.kie.jenkins.jobdsl.Constants

def javadk=Constants.JDK_TOOL
def AGENT_LABEL="kie-rhel8 && kie-mem4g"

def NEXT_PRODUCT_VERSION=Constants.NEXT_PROD_VERSION

def SERVERLESS_LOGIC_NEXT_PRODUCT_VERSION='999.0.0'
def SERVERLESS_LOGIC_KOGITO_NEXT_PRODUCT_VERSION=SERVERLESS_LOGIC_NEXT_PRODUCT_VERSION
def SERVERLESS_LOGIC_DROOLS_NEXT_PRODUCT_VERSION=SERVERLESS_LOGIC_NEXT_PRODUCT_VERSION
def SERVERLESS_LOGIC_NEXT_PRODUCT_BRANCH='main'
def SERVERLESS_LOGIC_NEXT_PRODUCT_CONFIG_BRANCH="master"

def SERVERLESS_LOGIC_CURRENT_PRODUCT_VERSION='1.36.0'
def SERVERLESS_LOGIC_KOGITO_CURRENT_PRODUCT_VERSION='9.103.0'
def SERVERLESS_LOGIC_DROOLS_CURRENT_PRODUCT_VERSION='9.103.0'
def SERVERLESS_LOGIC_CURRENT_PRODUCT_BRANCH='9.103.x-prod'
def SERVERLESS_LOGIC_CURRENT_PRODUCT_CONFIG_BRANCH="openshift-serverless-logic/9.103.x"

// Should be uncommented and used with kogitoWithSpecDroolsNightlyStage once Next is set for RHPAM 7.14.0 (or main)
// def DROOLS_NEXT_PRODUCT_VERSION='8.13.0'

// Should be uncommented and used with kogitoWithSpecDroolsNightlyStage once Current is set for RHPAM 7.14.0
// def DROOLS_CURRENT_PRODUCT_VERSION= OPTAPLANNER_CURRENT_PRODUCT_VERSION

// Drools Ansible Integration
def DAI_NEXT_PRODUCT_VERSION='' // took from project pom
def DAI_NEXT_DROOLS_VERSION='999.0.0'
def DAI_NEXT_PRODUCT_BRANCH='main'
def DAI_NEXT_PRODUCT_CONFIG_BRANCH='master'

def DAI_CURRENT_PRODUCT_BRANCH='1.0.x'
def DAI_CURRENT_PRODUCT_CONFIG_BRANCH='drools-ansible-integration/1.0.x'

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
        // Openshift Serverless Logic
        ${serverlessLogicNightlyStage(SERVERLESS_LOGIC_CURRENT_PRODUCT_BRANCH, SERVERLESS_LOGIC_CURRENT_PRODUCT_CONFIG_BRANCH, SERVERLESS_LOGIC_CURRENT_PRODUCT_VERSION, SERVERLESS_LOGIC_KOGITO_CURRENT_PRODUCT_VERSION, SERVERLESS_LOGIC_DROOLS_CURRENT_PRODUCT_VERSION)}
        ${serverlessLogicNightlyStage(SERVERLESS_LOGIC_NEXT_PRODUCT_BRANCH, SERVERLESS_LOGIC_NEXT_PRODUCT_CONFIG_BRANCH, SERVERLESS_LOGIC_NEXT_PRODUCT_VERSION, SERVERLESS_LOGIC_KOGITO_NEXT_PRODUCT_VERSION, SERVERLESS_LOGIC_DROOLS_NEXT_PRODUCT_VERSION)}
    
        // Drools Ansible Integration
        ${droolsAnsibleIntegrationNightlyStage(DAI_NEXT_PRODUCT_BRANCH, DAI_NEXT_PRODUCT_CONFIG_BRANCH, DAI_NEXT_PRODUCT_BRANCH, DAI_NEXT_PRODUCT_VERSION, DAI_NEXT_DROOLS_VERSION)}
        ${droolsAnsibleIntegrationNightlyStage(DAI_CURRENT_PRODUCT_BRANCH, DAI_CURRENT_PRODUCT_CONFIG_BRANCH, DAI_CURRENT_PRODUCT_BRANCH)}
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

String droolsAnsibleIntegrationNightlyStage(String branch, String configBranch, String definitionFileBranch = 'main', String version = '', String droolsVersion = '') {
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
    return [Constants.MAIN_BRANCH_PROD_VERSION, Constants.KOGITO_MAIN_BRANCH_PROD_VERSION].contains(version)
}
