/**
* Move Jiras that were fixed on a specific target release to QA
*/
def scriptTemplate = this.getClass().getResource("job-scripts/prod_move_jiras_qa.jenkinsfile").text
def parsedScript = scriptTemplate.replaceAll(/<%=\s*(\w+)\s*%>/) { config[it[1]] ?: '' }

def folderPath = 'PROD'
folder(folderPath)

pipelineJob("${folderPath}/move-jiras-qa") {
    description('This job moves Jiras that were fixed on a specific target release to QA.')

    parameters {
        stringParam('PRODUCT', 'rhpam,rhdm', 'Comma separated list of product, i.e rhpam,rhdm / rhbop')
        stringParam('VERSION', '7.13.2.CR1', 'The release candidate version, i.e. 7.13.2.CR1 / IBM BAMOE 8.0.2.CR1 / 8.29.0.CR1')
        stringParam('PRODUCT_VERSION', '7.13.2.GA', 'Product target version, i.e 7.13.2.GA / IBM BAMOE 8.0.2.GA / 8.29.0.GA')
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