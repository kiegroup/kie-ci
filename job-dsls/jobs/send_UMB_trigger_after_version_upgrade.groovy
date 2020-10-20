/**
 * pipeline that sends a UMB message to trigger a job after a version update of kiegroup
 */

def VERSION_ORG_KIE = "7.46.0-SNAPSHOT"

def sendUMB="""
pipeline {
    agent {
        label 'kie-rhel7 && kie-mem4g'
    }
    tools {
        maven 'kie-maven-3.6.3'
        jdk 'kie-jdk1.8'
    }
    stages {
        stage('CleanWorkspace') {
            steps {
                cleanWs()
            }
        }
        stage ('UMB message'){
            steps {
                sendCIMessage failOnError: false, messageContent: "\${VERSION_ORG_KIE}", messageProperties: '''CI_TYPE=custom
label=rhba-ci''', messageType: 'Custom', overrides: [topic: 'VirtualTopic.qe.ci.ba.rhba.master.update-versions'], providerName: 'Red Hat UMB'  
            }
        }
    }
    post {
        always {
            cleanWs()             
        }
    }         
}                     
"""
pipelineJob("send_UMB_trigger_after_kiegroup_version_update") {

    description("This is a pipeline job for sending an UMB trigger. <br>\
                This job should run after kiegroup repositories (master branches) were bumped up no the next development (-SNAPSHOT) version.")

    parameters {
        stringParam("VERSION_ORG_KIE", "${VERSION_ORG_KIE}", "Please edit the current SNAPSHOT version of kiegroup reps. i.e. 7.46.0-SNAPSHOT")
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
}