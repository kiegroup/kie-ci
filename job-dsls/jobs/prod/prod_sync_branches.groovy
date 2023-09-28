/**
* Sync branches of a repository from different GH organizations
*/
def scriptTemplate = this.getClass().getResource("job-scripts/prod_sync_branches.jenkinsfile").text
def parsedScript = scriptTemplate.replaceAll(/<%=\s*(\w+)\s*%>/) { config[it[1]] ?: '' }

def folderPath = "PROD"
folder(folderPath)


pipelineJob("${folderPath}/sync-branches") {
    description('This job sync branches of a repository from different GitHub organizations.')

    parameters {
        stringParam('REPOSITORIES', '', 'List of repositories separated by comma, i.e. kogito-runtimes,kogito-apps')
        stringParam('BRANCH', '', 'The branch that is going to be synced, i.e. 8.45.x')
        stringParam('SOURCE_ORGANIZATION', 'apache', 'The GitHub organization where the original branch is')
        stringParam('TARGET_ORGANIZATION', 'kiegroup', 'The GitHub organization where the branch will be pushed')
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
