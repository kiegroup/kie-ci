def pipelineScript =
        '''
@Library('jenkins-pipeline-shared-libraries')_

def repoList = []
node('kie-rhel7') {
    stage('Read repo file') {
      git url: "${DROOLSJBPM_BUILD_BOOTSTRAP_URL}", branch: "${DROOLSJBPM_BUILD_BOOTSTRAP_BRANCH}"
      def repoListFile = readFile "./script/repository-list.txt"
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
                            [$class: 'StringParameterValue', name: 'SRCCLR_INVOKER_REPO_URL, value: "${SRCCLR_INVOKER_REPO_URL}"],
                            [$class: 'StringParameterValue', name: 'URL', value: "${url}"],
                            [$class: 'StringParameterValue', name: 'VERSION', value: "${KIE_VERSION}"],
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

pipelineJob("${JOB_PATH}/srcclrpipeline") {

    description("This is a pipeline, which runs source clear scanning jobs")

    parameters {
        stringParam('KIE_VERSION')
        stringParam('SRCCLR_INVOKER_REPO_URL','https://github.com/project-ncl/sourceclear-invoker','URL of the JUnit tests, which invoke srcclr scanning.')
        stringParam('DROOLSJBPM_BUILD_BOOTSTRAP_URL','https://github.com/kiegroup/droolsjbpm-build-bootstrap.git','')
        stringParam('DROOLSJBPM_BUILD_BOOTSTRAP_BRANCH','master','')
    }

    definition {
        cps {
            script("${pipelineScript}")
            sandbox()
        }
    }

}