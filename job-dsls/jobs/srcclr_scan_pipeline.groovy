import org.kie.jenkins.jobdsl.Constants

def prefix = Constants.GITHUB_REPO_PREFIX

def pipelineScript =
        '''
@Library('jenkins-pipeline-shared-libraries')_

def repoList = []

def RECURSE = Boolean.valueOf("${RECURSIVE}") ? "--recursive" : ""
def DEBUG = Boolean.valueOf("${DEBUGGING}") ? "-d" : ""
def TRACE = Boolean.valueOf("${TRACING}") ? "--trace" : ""
def MVNPARAMETER = "${MVNPARAMS}" !="" ? "--maven-param=${MVNPARAMS}":""
def SCMVERSIONPARAM = "${SCAN_TYPE}" == "scm" ? " --ref=${SCMVERSION}":""

node('kie-rhel7') {
    
    stage('Read repo file') {
      git url: "${env.DROOLSJBPM_BUILD_BOOTSTRAP_REPO}", branch: "${env.DROOLSJBPM_BUILD_BOOTSTRAP_VERSION}"
      def repoListFile = readFile "./${env.REPO_LIST_FILE_PATH}"
      repoList = repoListFile.readLines()
    }
}

def branches = [:]

for (repo in repoList) {
    def branchName = "Source Clear ${repo}"
    def repoName = "${repo}"
    branches[branchName] = {
        try {
            node('kie-rhel7') {
                withCredentials([string(credentialsId: 'SRCCLR_API_TOKEN', variable: 'SRCCLR_API_TOKEN')]) {
                    stage(branchName) {
                        git url: "${env.SRCCLR_INVOKER_REPO_URL}", branch: 'master'
                        def url = "${URL_PREFIX}${repoName}"
                        maven.runMavenWithSettings(settingsXmlId, "-Pjenkins test -Dmaven.buildNumber.skip=true -DargLine='' -Dsourceclear=\"\${DEBUG} \${TRACE} --processor=\${PROCESSOR_TYPE} --product-version=\${VERSION} --package=\${PACKAGE} --product=\"\${NAME}\" --threshold=\${THRESHOLD} \${SCAN_TYPE} --url=\${URL} \${MVNPARAMETER} \${SCMVERSIONPARAM} \${RECURSE}\"", new Properties())
                    }
                }
            }
        } catch (e) {
            println 'ERROR'
            throw e
        } finally {
            step([$class: 'JUnitResultArchiver', testResults: '**/target/*-reports/*.xml', healthScaleFactor: 1.0, allowEmptyResults: true])
            publishHTML (target: [
                    allowMissing: true,
                    alwaysLinkToLastBuild: false,
                    keepAll: true,
                    reportDir: 'coverage',
                    reportFiles: 'index.html',
                    reportName: "Junit Report"
            ]) 
        }
    }
}

parallel branches

        '''

pipelineJob("Upstream Source Clear Scanning") {

    description("This is a pipeline, which runs source clear scanning jobs")

    parameters {
        choiceParam('SCAN_TYPE', ['scm', 'binary'])
        stringParam('SRCCLR_INVOKER_REPO_URL','')
        stringParam('URL','')
        stringParam('VERSION', '')
        stringParam('PACKAGE','')
        stringParam('NAME', '')
        stringParam('MVNPARAMS', '')
        choiceParam('PROCESSOR_TYPE', ['cve', 'cvss'])
        booleanParam('RECURSIVE', false)
        booleanParam('DEBUGGING', false)
        booleanParam('TRACING', false)
        stringParam('SCMVERSION', '')
        stringParam('THRESHOLD', '1','Threshold from 1 to 10 for cvss processor')
    }

    definition {
        cps {
            script("${pipelineScript}")
            sandbox()
        }
    }

}