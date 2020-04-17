def pipelineScript =
        '''
@Library('jenkins-pipeline-shared-libraries')_

def repoList = []
node('kie-rhel7') {
    stage('Read repo file') {
      git url: "${env.DROOLSJBPM_BUILD_BOOTSTRAP_REPO}", branch: 'master'
      def repoListFile = readFile "./script/repository-list.txt}"
      repoList = repoListFile.readLines()
    }
}

def branches = [:]

for (repo in repoList) {
    def branchName = "Source Clear ${repo}"
    def repoName = "${repo}"
    branches[branchName] = {
        node('kie-rhel7') {
            stage(branchName) {
                def url = "https://github.com/kiegroup/${repoName}"
                def jobName = "srcclr-scan-${repoName}"
                build job: "${jobName}", propagate: false, parameters: [
                            [$class: 'StringParameterValue', name: 'SCAN_TYPE', value: 'scm'],
                            [$class: 'StringParameterValue', name: 'SRCCLR_INVOKER_REPO_URL, value: 'https://github.com/project-ncl/sourceclear-invoker'],
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