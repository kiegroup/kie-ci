/**
* Create the UMB message body for OpenShift Serverless Logic and invoke prod/send_umb_message job to send the message.
*/
def scriptTemplate = this.getClass().getResource("job-scripts/prod_osl_trigger_umb_message.jenkinsfile").text
def parsedScript = scriptTemplate.replaceAll(/<%=\s*(\w+)\s*%>/) { config[it[1]] ?: '' }

def folderPath = 'PROD'
folder(folderPath)

pipelineJob("${folderPath}/osl-trigger-umb-message") {
    description('This job creates the OSL UMB message body and invoke prod/send_umb_message job to send the message.')

    parameters {
        stringParam('PRODUCT_ID', '', 'The product ID. This parameter is optional and in case it is not defined, the default OSL product ID is used.')
        stringParam('MILESTONE', '', 'The release milestone, i.e. 1.29.0.CR1. This parameter is optional and in case it is not defined, the latest available milestone is used.')
        stringParam('DROOLS_VERSION', '8.38.0.Final-redhat-00001', 'The productized version of Drools used for building Openshift Serverless Logic currect milestone.')
        stringParam('QUARKUS_PLATFORM_VERSION', '2.13.7.Final-redhat-00003', 'The productized version of Quarkus Platform used for building Openshift Serverless Logic currect milestone.')
        stringParam('QUARKUS_VERSION', '2.13.7.Final-redhat-00003', 'The productized version of Quarkus used for building Openshift Serverless Logic currect milestone.')
        stringParam('PNC_API_URL', "\${ORCH_PSI_URL}/pnc-rest/v2", "PNC Rest API endpoint. See: \${DOCS_ENGINEERING_URL}/display/JP/User%27s+guide")
        stringParam('DATA_INDEX_EPHEMERAL_BREW', '', 'The openshift-serverless-1-logic-data-index-ephemeral-rhel8-container brew build id, i.e. 2195547. This parameter is optional and in case it is not defined, the latest built image is used.')
    }

    logRotator {
        numToKeep(32)
    }

    definition {
        cps {
            script(parsedScript)
            sandbox()
        }
    }
}