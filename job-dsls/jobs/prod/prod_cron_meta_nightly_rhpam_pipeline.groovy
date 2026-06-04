/**
 * This job is a workaround for the nightly-meta-pipeline (wich was disabled) and will be removed once the UMB messages are sent again<br>
 * Instead UMB message a simple CRON job is applied.
 */

import org.kie.jenkins.jobdsl.Constants

def javadk=Constants.JDK_TOOL
def AGENT_LABEL="kie-rhel8 && kie-mem4g"

// is not used as we removed RHBA RHPAM builds from here
def NEXT_PRODUCT_VERSION=Constants.NEXT_PROD_VERSION
def NEXT_PRODUCT_BRANCH='7.67.x'
def NEXT_PRODUCT_CONFIG_BRANCH="rhba/${NEXT_PRODUCT_BRANCH}"
def NEXT_RHBA_VERSION_PREFIX='7.67.1.redhat-'
def OPTAPLANNER_NEXT_PRODUCT_VERSION='8.13.0'

// BAMOE nightly only on 7.67.x-blue branches
def NEXT_BLUE_PRODUCT_VERSION=Constants.BAMOE_NEXT_PROD_VERSION
def NEXT_BLUE_PRODUCT_BRANCH='7.67.x-blue'
def NEXT_BLUE_PRODUCT_CONFIG_BRANCH="bamoe/7.67.x-blue"
def NEXT_BLUE_RHBA_VERSION_PREFIX='7.67.2.redhat-'

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
        // BAMOE blue branches
        ${rhbaNightlyStage(NEXT_BLUE_PRODUCT_VERSION, NEXT_BLUE_PRODUCT_BRANCH, NEXT_BLUE_PRODUCT_CONFIG_BRANCH)}
    }
}
"""
// creates folder if is not existing
def folderPath="PROD"
folder(folderPath)

pipelineJob("${folderPath}/cron-meta-nightly-pipeline-rhpam") {

    description("This job is a workaround for the nightly-meta-pipeline (which was disabled) and will be removed once the UMB messages are sent again<br>\n" +
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
                    spec("H 15 * * 2")
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
