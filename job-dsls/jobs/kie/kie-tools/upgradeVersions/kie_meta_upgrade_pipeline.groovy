/**
 * pipeline that starts all needed jobs for upgrading kiegruop repositories
 * community kiegroup/repositories
 * kie-benchmarks
 * kie-cloud-tests
 */

import org.kie.jenkins.jobdsl.Constants

def javadk=Constants.JDK_TOOL
def AGENT_LABEL="kie-linux && kie-rhel7"
def MVN_TOOL = Constants.MAVEN_TOOL
def JDK_TOOL = Constants.JDK_TOOL
def BASE_BRANCH = ""
def CURRENT_KIE_VERSION = ""
def NEW_KIE_VERSION=""
def KIE_PREFIX=""
def ORGANIZATION="kiegroup"

def metaJob='''
pipeline {
    agent {
        label "$AGENT_LABEL"
    }
    options{
        timestamps()
    }
    tools {
        maven "$MVN_TOOL"
        jdk "$JDK_TOOL"
    }
    stages {
        stage('CleanWorkspace') {
            steps {
                cleanWs()
            }
        } 
        stage('build upgrade jobs') {
            steps {
                parallel (
                    "kieAll" : {
                        build job: "upgrade-kiegroup-all", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'BASE_BRANCH', value: BASE_BRANCH], [$class: 'StringParameterValue', name: 'CURRENT_KIE_VERSION', value: CURRENT_KIE_VERSION], [$class: 'StringParameterValue', name: 'NEW_KIE_VERSION', value: NEW_KIE_VERSION], [$class: 'StringParameterValue', name: 'ORGANIZATION', value: ORGANIZATION]]
                    },  
                    "kie-benchmarks" : {
                        build job: "upgrade-kie-benchmarks", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'BASE_BRANCH', value: BASE_BRANCH], [$class: 'StringParameterValue', name: 'CURRENT_KIE_VERSION', value: CURRENT_KIE_VERSION], [$class: 'StringParameterValue', name: 'NEW_KIE_VERSION', value: NEW_KIE_VERSION], [$class: 'StringParameterValue', name: 'ORGANIZATION', value: ORGANIZATION]]
                    }, 
                    "kie-cloud-tests" : {
                        build job: 'upgrade-kie-cloud-tests', propagate: false, parameters: [[$class: 'StringParameterValue', name: 'BASE_BRANCH', value: BASE_BRANCH], [$class: 'StringParameterValue', name: 'CURRENT_KIE_VERSION', value: CURRENT_KIE_VERSION], [$class: 'StringParameterValue', name: 'NEW_KIE_VERSION', value: NEW_KIE_VERSION], [$class: 'StringParameterValue', name: 'ORGANIZATION', value: ORGANIZATION]]
                    },
                    "kie-prefix" : {
                        build job: 'upgrade-kie-prefix', propagate: false, parameters: [[$class: 'StringParameterValue', name: 'BASE_BRANCH', value: BASE_BRANCH], [$class: 'StringParameterValue', name: 'KIE_PREFIX', value: KIE_PREFIX], [$class: 'StringParameterValue', name: 'ORGANIZATION', value: ORGANIZATION]]                    
                    }                   
                )      
            }    
        } 
    }
    post{
        always{
            cleanWs()
        }
    }
}                     
'''
// creates folder if is not existing
folder("KIE")
folder("KIE/kie-tools")
folder("KIE/kie-tools/upgradeVersions")
def folderPath="KIE/kie-tools/upgradeVersions"

pipelineJob("${folderPath}/kie-meta-upgrade-pipeline") {

    description("This pipeline trigger jobs to upgrade kiegroup versions to a new version")

    parameters {
        stringParam("BASE_BRANCH", "${BASE_BRANCH}", "Branch to clone and update")
        stringParam("CURRENT_KIE_VERSION", "${CURRENT_KIE_VERSION}", "the current version of KIE repositories on BASE_BRANCH")
        stringParam("NEW_KIE_VERSION", "${NEW_KIE_VERSION}", "KIE versions on BASE_BRANCH should be bumped up to this version")
        stringParam("ORGANIZATION", "${ORGANIZATION}", "organization of github: mostly kiegroup")
        stringParam("KIE_PREFIX","${KIE_PREFIX}","prefix for kie versions. i.e. 7.65.0")
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
            name('JDK_TOOL')
            defaultValue("${JDK_TOOL}")
            description('version of jdk')
        }
    }

    logRotator {
        numToKeep(3)
    }

    definition {
        cps {
            script("${metaJob}")
            sandbox()
        }
    }
}
