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
        stringParam('KN_WORKFLOW_BINARY_BASE_URL', '\${ETERA_SERVER_URL}/kn-workflow-plugin/1/1.34/1.34.0-1/signed/', 'The base URL for the Kn Workflow Plugin binary')
        stringParam('DATA_INDEX_EPHEMERAL_BREW', '', 'The openshift-serverless-1-logic-data-index-ephemeral-rhel8-container Brew build ID. This parameter is optional and in case it is not defined, the latest built image is used.')
        stringParam('DATA_INDEX_POSTGRESQL_BREW', '', 'The openshift-serverless-1-logic-data-index-postgresql-rhel8-container Brew build ID. This parameter is optional and in case it is not defined, the latest built image is used.')
        stringParam('JOBS_SERVICE_EPHEMERAL_BREW', '', 'The openshift-serverless-1-logic-jobs-service-ephemeral-rhel8-container Brew build ID. This parameter is optional and in case it is not defined, the latest built image is used.')
        stringParam('JOBS_SERVICE_POSTGRESQL_BREW', '', 'The openshift-serverless-1-logic-jobs-service-postgresql-rhel8-container Brew build ID. This parameter is optional and in case it is not defined, the latest built image is used.')
        stringParam('SWF_BUILDER_BREW', '', 'The openshift-serverless-1-logic-swf-builder-rhel8-container Brew build ID. This parameter is optional and in case it is not defined, the latest built image is used.')
        stringParam('SWF_DEVMODE_BREW', '', 'The openshift-serverless-1-logic-swf-devmode-rhel8-container Brew build ID. This parameter is optional and in case it is not defined, the latest built image is used.')
        stringParam('MANAGEMENT_CONSOLE_BREW', '', 'The openshift-serverless-1-logic-management-console-rhel8-container Brew build ID. This parameter is optional and in case it is not defined, the latest built image is used.')
        stringParam('DB_MIGRATOR_TOOL_BREW', '', 'The openshift-serverless-1-logic-db-migrator-tool-rhel8-container Brew build ID. This parameter is optional and in case it is not defined, the latest built image is used.')
        stringParam('OPERATOR_BREW', '', 'The openshift-serverless-1-logic-rhel8-operator-container Brew build ID. This parameter is optional and in case it is not defined, the latest built image is used.')
        stringParam('OPERATOR_BUNDLE_BREW', '', 'The openshift-serverless-1-logic-rhel8-operator-bundle-container Brew build ID. This parameter is optional and in case it is not defined, the latest built image is used.')
        stringParam('KN_WORKFLOW_CLI_ARTIFACTS_BREW', '', 'The openshift-serverless-1-logic-kn-workflow-cli-artifacts-rhel8-container Brew build ID. This parameter is optional and in case it is not defined, the latest built image is used.')
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