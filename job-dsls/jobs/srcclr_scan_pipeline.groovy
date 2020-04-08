import org.kie.jenkins.jobdsl.Constants

def prefix = Constants.GITHUB_REPO_PREFIX
def repoListFileId = Constants.REPO_LIST_FILE_ID

def pipelineScript =
        '''
@Library('jenkins-pipeline-shared-libraries')_

def repoList = []
node('kie-rhel7') {
    stage('Read repo file') {
        configFileProvider([configFile(fileId: "${repoListFileId}", variable:'repoListFile')]) {
            def repoFile = readFile "$repoListFile"
            repoList = repoFile.readLines()
        }
    }
}

def branches = [:]

for (repo in repoList) {
    def branchName = 'Source Clear ' + "${repo}"
    println 'Repo before node ' + "${repo}"
    def repoName = "${repo}"
    branches[branchName] = {
        node('kie-rhel7') {
            stage(branchName) {
                def url = "${prefix}" + "${repoName}"
                def jobName = 'srcclr-scan-' + "${repoName}"
                build job: "${jobName}", propagate: false, parameters: [
                            [$class: 'StringParameterValue', name: 'SCAN_TYPE', value: 'scm'],
                            [$class: 'StringParameterValue', name: 'URL', value: "${url}"],
                            [$class: 'StringParameterValue', name: 'VERSION', value: "${kieVersion}"],
                            [$class: 'StringParameterValue', name: 'NAME', value: "${repoName}"],
                            [$class: 'StringParameterValue', name: 'PROCESSOR_TYPE', value: 'cve'],
                            [$class: 'StringParameterValue', name: 'THRESHOLD', value: '1']
                ]

            }
        }
    }
}

parallel branches

        '''

pipelineJob("parallel source clear scanning") {

    description("This is a pipeline, which runs source clear scanning jobs")

    parameters {
        stringParam('kieVersion')
    }

    definition {
        cps {
            script("${pipelineScript}")
            sandbox()
        }
    }

}