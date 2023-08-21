/**
* Create the OpenShift images sources
*/
def scriptTemplate = this.getClass().getResource("job-scripts/prod_bamoe_kogito_create_images_sources.jenkinsfile").text
def parsedScript = scriptTemplate.replaceAll(/<%=\s*(\w+)\s*%>/) { config[it[1]] ?: '' }

def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/bamoe-kogito-create-images-sources") {
    description('This job creates the Openshift sources for BAMOE Kogito.')

    parameters {
        stringParam('VERSION', '', 'The milestone version, i.e. 1.13.5')
        stringParam("PRODUCT_MILESTONE", "CR1")
        stringParam('BAMOE_VERSION', '', 'The BAMOE version related to the BAMOE Kogito release, i.e. 8.0.3')
        stringParam('BRANCH', '1.13.x-blue', 'The OpenShift images branch used to build the images')
        stringParam('BUILDS', '', 'List of Brew builds IDs separated by comma. Required images are: Kogito Runtime JVM, Kogito Runtime Native and Kogito Builder')
        stringParam('OVERRIDING_FILES', 'bamoe-kogito-runtime-jvm-rhel8-overrides.yaml,bamoe-kogito-builder-rhel8-overrides.yaml', 'Comma separated list of the overriding files that will be fetched from the images repositories')
        stringParam('GITHUB_REFERENCE', '', 'Override the GitHub reference for all cloned repositories')
        booleanParam('UPLOAD_ARTIFACTS', true, 'If the generated artifacts should be uploaded to rcm-host')
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