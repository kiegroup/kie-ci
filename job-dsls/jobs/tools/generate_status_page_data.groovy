/**
* Create the OpenShift images templates and sources
*/
def scriptTemplate = this.getClass().getResource("job-scripts/generate_status_page_data.jenkinsfile").text
def parsedScript = scriptTemplate.replaceAll(/<%=\s*(\w+)\s*%>/) { config[it[1]] ?: '' }

def folderPath = "Tools"
folder(folderPath)

pipelineJob("${folderPath}/rhba.generate-status-page-data") {
    description('This job generates data for https://kiegroup.github.io/droolsjbpm-build-bootstrap')

    logRotator {
        numToKeep(20)
    }

    properties {
        pipelineTriggers {
            triggers {
                cron{
                    spec("H */6 * * *")
                }
            }
        }
    }

    definition {
        cps {
            script(parsedScript)
            sandbox()
        }
    }

}
