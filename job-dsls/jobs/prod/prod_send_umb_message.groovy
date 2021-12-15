/**
 * Sends umb message to a specific provider
 *
 */

def metaJob='''
pipeline {
    agent {
        node {
            label "kie-rhel||rhos-01-kie-rhel&&!master"
        }
    }
    options {
        timeout(time: "${TIMEOUT}", unit: 'MINUTES')
        ansiColor('xterm')
        timestamps()
    }
    stages {
        stage ("Clean") {
            steps {
                echo "[INFO] Cleaning the workspace."
                cleanWs()
                echo "[SUCCESS] Workspace was successfully cleaned up."
            }
        }
        
        stage ("Send message") {
            steps {
                echo "[INFO] Sending message to '${PROVIDER_NAME}' provider with body: ${MESSAGE_BODY} "
                script {
                    def sendResult = sendCIMessage providerName: ${PROVIDER_NAME}, \
                        messageContent: MESSAGE_BODY, \
                        messageType: 'Custom', \
                        messageProperties: "EVENT_TYPE=${EVENT_TYPE} \n label=${EVENT_LABEL}", \
                        overrides: [topic: TOPIC], \
                        failOnError: true
                    echo "[INFO] Sent message ID: ${sendResult.getMessageId()}"
                    echo "[INFO] Sent message contents: ${sendResult.getMessageContent()}"
                }
            }
        }
    }
}
'''

// creates folder if is not existing
def folderPath='PROD'
folder(folderPath)

pipelineJob("${folderPath}/send-umb-message") {

    description('Send a UMB message')

    parameters {
        stringParam('PROVIDER_NAME', 'Red Hat UMB', 'The UMB provider name')
        stringParam('MESSAGE_BODY', '', 'the message content')
        stringParam('TOPIC', 'VirtualTopic.qe.ci.ba.rhdm.77.nightly.trigger', 'The UMB topic to be consumed')
        stringParam('EVENT_TYPE', 'rhdm-77-nightly-qe-trigger', 'The UMB event to be consumed')
        stringParam('EVENT_LABEL', 'rhba-ci', 'The UMB label')
        stringParam('TIMEOUT', '1', 'The job timeout in minutes')
    }

    logRotator {
        numToKeep(32)
    }

    definition {
        cps {
            script("${metaJob}")
            sandbox()
        }
    }
}
