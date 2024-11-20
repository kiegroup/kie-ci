/**
 * pipeline that sends a UMB message to trigger all daily builds of different branches
 */

import org.kie.jenkins.jobdsl.Constants

def javadk=Constants.JDK_TOOL
def AGENT_LABEL="kie-rhel7"

def metaJob="""
pipeline{
    agent {
        label "$AGENT_LABEL"
    } 
    tools {
        jdk "$javadk"        
    }   
    stages {
        stage('trigger daily build pipeline') {
            steps {
                parallel (
                    "main-jdk11-pipeline" : {
                        build job: "../main/daily-build-jdk11/jdk11-db-main", propagate: false
                    },  
                    "main-jdk8-pipeline" : {
                        build job: "../main/daily-build-jdk8/jdk8-db-main", propagate: false
                    },
                    "main-jdk11-prod-pipeline" : {
                        build job: '../main/daily-build-jdk11-prod/jdk11-prod-db-main', propagate: false
                    }, 
                    "7.67.x-jdk11-pipeline" : {
                        build job: "../7.67.x/daily-build-jdk11/jdk11-db-7.67.x", propagate: false
                    },  
                    "7.67.x-jdk8-pipeline" : {
                        build job: "../7.67.x/daily-build-jdk8/jdk8-db-7.67.x", propagate: false
                    }                    
                )      
            }    
        } 
    }
}                     
"""
// creates folder if is not existing
folder("KIE")
folder("KIE/tools-kie-ci")
def folderPath="KIE/tools-kie-ci"

pipelineJob("${folderPath}/kieAll_meta_pipeline") {
    
    description("This is job to orchestrate and run all daily build jobs")

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
        numToKeep(3)
    }

    properties {
        pipelineTriggers {
            triggers {
                cron{
                    spec("H 17 * * *")
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
