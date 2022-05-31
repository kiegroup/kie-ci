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
        stringParam('VERSION', '7.13.0.CR1', 'The release candidate version, i.e. 7.13.0.CR1')
        stringParam('PRODUCT_VERSION', '7.13.0.GA', 'Product target version, i.e 7.13.0.GA')
        stringParam('CUTOFF_DATE', '2022-05-30', 'The cutoff date. Jiras resolved after this date will be ignored, i.e. 2022-05-30')
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