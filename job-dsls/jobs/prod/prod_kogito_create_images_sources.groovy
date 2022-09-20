/**
* Create the OpenShift images sources
*/
def scriptTemplate = this.getClass().getResource("job-scripts/prod_kogito_create_images_sources.jenkinsfile").text
def parsedScript = scriptTemplate.replaceAll(/<%=\s*(\w+)\s*%>/) { config[it[1]] ?: '' }

def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/kogito-create-images-sources") {
    description('This job creates the Openshift sources for Kogito.')

    parameters {
        stringParam('VERSION', '', ' The milestone version, i.e. 1.13.1')
        stringParam('BUILDS', '', 'List of Brew builds separated by comma')
        stringParam('OVERRIDING_FILES', 'rhpam-kogito-runtime-jvm-rhel8-overrides.yaml,rhpam-kogito-builder-rhel8-overrides.yaml', 'Comma separated list of the overriding files that will be fetched from the images repositories')
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