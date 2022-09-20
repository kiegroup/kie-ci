/**
* Create the OpenShift images templates and sources
*/
def scriptTemplate = this.getClass().getResource("job-scripts/prod_create_images_templates_and_sources.jenkinsfile").text
def parsedScript = scriptTemplate.replaceAll(/<%=\s*(\w+)\s*%>/) { config[it[1]] ?: '' }

def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/create-images-templates-and-sources") {
    description('This job creates the Openshift images templates and sources for RHPAM.')

    parameters {
        stringParam('VERSION', '', ' The milestone version, i.e. 7.12.1')
        stringParam('BUILDS', '', 'List of Brew builds separated by comma')
        stringParam('OVERRIDING_FILES', 'branch-overrides.yaml', 'Comma separated list of the overriding files that will be fetched from the images repositories')
        stringParam('GITHUB_REFERENCE', '', 'Override the GitHub reference for all cloned repositories')
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