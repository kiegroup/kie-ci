/**
* Create the release tracker tickets
*/
def scriptTemplate = this.getClass().getResource("job-scripts/prod_create_release_trackers.jenkinsfile").text
def parsedScript = scriptTemplate.replaceAll(/<%=\s*(\w+)\s*%>/) { config[it[1]] ?: '' }

def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/create-release-trackers") {
    description('This job creates the release trackers tickets required by each release.')

    parameters {
        stringParam('VERSION', '', ' The release candidate version, i.e. 7.12.1.CR1')
        choiceParam('TEMPLATE', ['rhba', 'rhbop'], 'The template file that describes the tickets that will be created')
    }

    logRotator {
        numToKeep(20)
    }

    definition {
        cps {
            script(parsedScript)
            sandbox()
        }
    }

}