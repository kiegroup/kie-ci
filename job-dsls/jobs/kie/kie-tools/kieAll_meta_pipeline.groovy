/**
 * pipeline that sends a UMB message to trigger all daily builds of different branches
 */

import org.kie.jenkins.jobdsl.Constants

def javadk=Constants.JDK_VERSION
def AGENT_LABEL="kie-linux && kie-rhel7 && kie-mem8g"

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
                    "main-pipeline" : {
                        build job: "../main/daily-build/daily-build-pipeline-main", propagate: false
                    },  
                    "main-jdk11-pipeline" : {
                        build job: "../main/daily-build-jdk11/daily-build-jdk11-pipeline-main", propagate: false
                    }, 
                    "prod-main-pipeline" : {
                        build job: '../main/daily-build-prod/daily-build-prod-pipeline-main', propagate: false
                    }.
                    "7.59.x-pipeline" : {
                        build job: "../7.59.x/daily-build/daily-build-pipeline-7.59.x", propagate: false
                    },  
                    "7.59.x-jdk11-pipeline" : {
                        build job: "../7.59.x/daily-build-jdk11/daily-build-jdk11-pipeline-7.59.x", propagate: false
                    }, 
                    "7.59.x-main-pipeline" : {
                        build job: '../7.59.x/daily-build-prod/daily-build-prod-pipeline-7.59.x', propagate: false
                    }                    
                )      
            }    
        } 
    }
}                     
"""
// creates folder if is not existing
folder("KIE")
folder("KIE/kie-tools")
def folderPath="KIE/kie-tools"

pipelineJob("${folderPath}/kieAll_meta_pipeline") {
    
    description("This is a pipeline job for sending an UMB trigger to run all daily build jobs")

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
