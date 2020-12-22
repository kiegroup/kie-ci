/**
 * pipeline that sends a UMB message to trigger all daily builds of different branches
 */

import org.kie.jenkins.jobdsl.Constants

def javadk=Constants.JDK_VERSION
def AGENT_LABEL="kie-linux && kie-rhel7&&kie-mem8g"

def sendUMB="""pipeline{
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
                    "master-pipeline" : {
                        build job: "../master/daily-build/daily-build-pipeline-master", propagate: false
                    },  
                    "master-jdk11-pipeline" : {
                        build job: "../master/daily-build-jdk11/daily-build-jdk11-pipeline-master", propagate: false
                    }, 
                    "prod-master-pipeline" : {
                        build job: '../master/daily-build-prod/daily-build-prod-pipeline-master', propagate: false
                    },
                    "7.44.x-pipeline" : {
                        build job: "../7.44.x/daily-build/daily-build-pipeline-7.44.x", propagate: false
                    },  
                    "prod-7.44.x-pipeline" : {
                        build job: '../7.44.x/daily-build-prod/daily-build-prod-pipeline-7.44.x', propagate: false
                    },
                    "7.48.x-pipeline" : {
                        build job: "../7.48.x/daily-build/daily-build-pipeline-7.48.x", propagate: false
                    },
                    "7.48.x-jdk11-pipeline" : {
                        build job: "../7.48.x/daily-build-jdk11/daily-build-jdk11-pipeline-7.48.x", propagate: false
                    },                     
                    "prod-7.48.x-pipeline" : {
                        build job: '../7.48.x/daily-build-prod/daily-build-prod-pipeline-7.48.x', propagate: false
                    }                                        
                )      
            }    
        } 
    }
}                     
"""

pipelineJob("kieAll_meta_pipeline") {

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

    definition {
        cps {
            script("${sendUMB}")
            sandbox()
        }
    }

    triggers {
        ciBuildTrigger {
            providers {
                providerDataEnvelope {
                    providerData {
                        activeMQSubscriberProviderData {
                            // UMB provider name - for development purposes this should be changed to 'Red Hat UMB Stage'.
                            // The "Red Hat UMB" provider is configured out-of-the-box by the redhat-ci-plugin
                            name("Red Hat UMB")
                            overrides {
                                // The topic name needs to be unique for every job listening to the UMB (note the UUID in the middle).
                                // When reusing (copy-pasting) this configuration, make sure to change the UUID to a different one, or use
                                // different unique string
                                topic("Consumer.rh-jenkins-ci-plugin.\${JENKINS_UMB_ID}-prod-daily-all-trigger.VirtualTopic.qe.ci.ba.daily-all.trigger")
                            }
                            selector('label = \'rhba-ci\'')
                            timeout(60)
                            variable('CI_MESSAGE')

                        }
                    }
                }
            }
            noSquash(true)
        }
    }
}