/**
 * pipeline that starts all needed jobs for upgrading kiegroup repositories
 * community kiegroup/repositories
 * kie-benchmarks
 * kie-cloud-tests
 * KIE_PREFIX change in kie-jenkins-scripts Constants
 */

import org.kie.jenkins.jobdsl.Constants

def javadk=Constants.JDK_TOOL
def AGENT_LABEL="rhos-d && kie-rhel7 && kie-mem8g"
def MVN_TOOL = Constants.MAVEN_TOOL
def JDK_TOOL = Constants.JDK_TOOL
def BASE_BRANCH = ""
def CURRENT_KIE_VERSION = ""
def NEW_KIE_VERSION=""
def KIE_PREFIX=""
def NEW_BRANCH=""
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
                        build job: "upgrade-kiegroup-all", propagate: false, parameters: \n
                        [[$class: 'StringParameterValue', name: 'BASE_BRANCH', value: BASE_BRANCH], \n
                        [$class: 'StringParameterValue', name: 'CURRENT_KIE_VERSION', value: CURRENT_KIE_VERSION], \n
                        [$class: 'StringParameterValue', name: 'NEW_KIE_VERSION', value: NEW_KIE_VERSION], \n
                        [$class: 'StringParameterValue', name: 'ORGANIZATION', value: ORGANIZATION], \n
                        [$class: 'StringParameterValue', name: 'NEW_BRANCH', value: NEW_BRANCH], \n
                        [$class: 'StringParameterValue',name: 'createNewBranch', value: createNewBranch]]
                    },  
                    "kie-benchmarks" : {
                        build job: "upgrade-kie-benchmarks", propagate: false, parameters: \n 
                        [[$class: 'StringParameterValue', name: 'BASE_BRANCH', value: BASE_BRANCH],  \n
                        [$class: 'StringParameterValue', name: 'CURRENT_KIE_VERSION', value: CURRENT_KIE_VERSION], \n 
                        [$class: 'StringParameterValue', name: 'NEW_KIE_VERSION', value: NEW_KIE_VERSION], \n 
                        [$class: 'StringParameterValue', name: 'ORGANIZATION', value: ORGANIZATION], \n 
                        [$class: 'StringParameterValue', name: 'NEW_BRANCH', value: NEW_BRANCH], \n 
                        [$class: 'StringParameterValue',name: 'createNewBranch', value: createNewBranch]]
                    }, 
                    "kie-cloud-tests" : {
                        build job: 'upgrade-kie-cloud-tests', propagate: false, parameters: \n 
                        [[$class: 'StringParameterValue', name: 'BASE_BRANCH', value: BASE_BRANCH], \n 
                        [$class: 'StringParameterValue', name: 'CURRENT_KIE_VERSION', value: CURRENT_KIE_VERSION], \n 
                        [$class: 'StringParameterValue', name: 'NEW_KIE_VERSION', value: NEW_KIE_VERSION], \n 
                        [$class: 'StringParameterValue', name: 'ORGANIZATION', value: ORGANIZATION], \n 
                        [$class: 'StringParameterValue', name: 'NEW_BRANCH', value: NEW_BRANCH], \n 
                        [$class: 'StringParameterValue',name: 'createNewBranch', value: createNewBranch]]
                    }
                )
            }    
        }    
        stage('run kie prefix job'){
            when{
                expression {createNewBranch == 'NO'}
            }
            steps{
                build job: 'upgrade-kie-prefix', propagate: false, parameters: \n 
                [[$class: 'StringParameterValue', name: 'BASE_BRANCH', value: BASE_BRANCH], \n 
                [$class: 'StringParameterValue', name: 'KIE_PREFIX', value: KIE_PREFIX], \n 
                [$class: 'StringParameterValue', name: 'ORGANIZATION', value: ORGANIZATION], \n 
                [$class: 'StringParameterValue',name: 'createNewBranch', value: createNewBranch]]
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
        stringParam("KIE_PREFIX","${KIE_PREFIX}","prefix for kie versions. i.e. 7.65.0. This parameter only has to be edited when \n" +
                "a new branch won't be created. i.e. createNewBranch = NO ")
        choiceParam("createNewBranch",["NO", "YES"],"should a new branch be created?")
        stringParam("NEW_BRANCH","${NEW_BRANCH}","makes only sense if you want to create a new branch based on BASE_BRANCH and \n" +
                " upgrade it's version. This parameter can be empty if the previous choice parameter is NO")
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
