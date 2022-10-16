/**
* Move Jiras with status "Resolved" to "Ready for QA" for a specific target release
*/
def scriptTemplate = this.getClass().getResource("job-scripts/prod_move_jiras_qa.jenkinsfile").text
def parsedScript = scriptTemplate.replaceAll(/<%=\s*(\w+)\s*%>/) { config[it[1]] ?: '' }

def folderPath = 'PROD'
folder(folderPath)

pipelineJob("${folderPath}/move-jiras-qa") {
    description('This job moves Jiras wih status "Resolved" to "Ready for QA" for a specific target release.')

    parameters {
        stringParam('PRODUCT', 'rhpam,rhdm', 'Comma separated list of product, i.e rhpam,rhdm')
        stringParam('VERSION', '7.13.2.CR1', 'The release candidate version, i.e. 7.13.2.CR1 or IBM BAMOE 8.0.2.CR1')
        stringParam('PRODUCT_VERSION', '7.13.2.GA', 'Product target version, i.e 7.13.2.GA or IBM BAMOE 8.0.2.GA')
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