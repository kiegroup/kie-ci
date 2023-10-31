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
        stringParam('REPOSITORIES', '', 'List of repositories to be synced separated by comma, i.e. kogito-runtimes,kogito-apps')
        stringParam('SOURCE_ORGANIZATION', 'apache', 'The GitHub organization where the original branch is')
        stringParam('SOURCE_BRANCH', '', 'The source branch that is going to be synced, i.e. main')
        stringParam('TARGET_ORGANIZATION', 'kiegroup', 'The GitHub organization where the branch will be pushed')
        stringParam('TARGET_BRANCH', '\${SOURCE_BRANCH}', 'The name of the branch that will be generated on target repository, i.e. main-apache')
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
