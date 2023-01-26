/**
* Create the UMB message body for OpenShift Serverless Logic and invoke prod/send_umb_message job to send the message.
*/
def scriptTemplate = this.getClass().getResource("job-scripts/prod_rhbop_trigger_umb_message.jenkinsfile").text
def parsedScript = scriptTemplate.replaceAll(/<%=\s*(\w+)\s*%>/) { config[it[1]] ?: '' }

def folderPath = 'PROD'
folder(folderPath)

pipelineJob("${folderPath}/rhbop-trigger-umb-message") {
    description('This job creates the RHBOP UMB message body and invoke prod/send_umb_message job to send the message.')

    parameters {
        stringParam('PRODUCT_ID', '', 'The product ID. This parameter is optional and in case it is not defined, the default OSL product ID is used.')
        stringParam('MILESTONE', '', 'The release milestone, i.e. 8.29.0.CR1. This parameter is optional and in case it is not defined, the latest available milestone is used.')
        stringParam('QUARKUS_VERSION', '2.13.5.Final-redhat-00003', 'The productized version of Quarkus core used for building Red Hat build of OptaPlanner currect milestone.')
        stringParam('QUARKUS_PLATFORM_VERSION', '2.13.5.SP1-redhat-00002', 'The productized version of Quarkus platform used for building Red Hat build of OptaPlanner currect milestone.')
        stringParam("PNC_API_URL", "\${ORCH_PSI_URL}/pnc-rest/v2", "PNC Rest API endpoint. See: \${DOCS_ENGINEERING_URL}/display/JP/User%27s+guide")
        stringParam("INDY_URL", "\${INDY_PNC_URL}", "Indy PNC builds repository")
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