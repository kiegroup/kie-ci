/**
* Print content required by RHSA Errata
*/
def scriptTemplate = this.getClass().getResource("job-scripts/prod_rhsa_errata_content.jenkinsfile").text
def parsedScript = scriptTemplate.replaceAll(/<%=\s*(\w+)\s*%>/) { config[it[1]] ?: '' }

def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/rhsa-errata-content") {
    description('This job generates the content needed when creating the RHSA Errata.')

    parameters {
        stringParam('PRODUCT_NAME', 'RHPAM', 'Product name')
        stringParam('PRODUCT_VERSION', '7.12.0.GA', 'Product target version')
        stringParam('COMPONENT_NAME', '', 'Filter product Jiras result by a component')
        stringParam('ADDITIONAL_JIRAS', '', 'Comma separated list of extra Jiras not part of current product name/version')
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