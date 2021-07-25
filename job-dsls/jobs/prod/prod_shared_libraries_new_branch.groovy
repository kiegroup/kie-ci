def newShared = '''
pipeline {
    agent {
        label 'kie-rhel7 && kie-mem4g'
    }
    tools {
        maven 'kie-maven-3.8.1'
        jdk 'kie-jdk1.8'
    }
    stages {
        stage('CleanWorkspace before') {
            steps {
                cleanWs()
            }
        }
        stage('Checkout droolsjbpm-build-bootstrap') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '$BASE_BRANCH']], browser: [$class: 'GithubWeb', repoUrl: 'git@github.com:kiegroup/jenkins-pipeline-shared-libraries.git'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'jenkins-pipeline-shared-libraries']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'kie-ci-user-key', url: 'git@github.com:kiegroup/jenkins-pipeline-shared-libraries.git']]])
                dir("${WORKSPACE}" + '/jenkins-pipeline-shared-libraries') {
                    sh 'pwd \\n' +
                       'git branch \\n' +
                       'git checkout -b $BRANCH_NAME \\n' +
                       'git branch'
                }
            }    
        }
        stage('Build new branch') {
            steps{
                configFileProvider([configFile(fileId: '3f317dd7-4d08-4ee4-b9bb-969c309e782c', targetLocation: 'jenkins-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                    dir("${WORKSPACE}" + '/jenkins-pipeline-shared-libraries') {
                        sh 'mvn clean install -s $SETTINGS_XML_FILE'
                    }
                }    
            }
        }
        stage ('Push new branch to origin') {
            steps{
                sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/jenkins-pipeline-shared-libraries') {
                        sh 'git push origin "${BRANCH_NAME}"'
                    }
                }
            }
        } 
        stage('CleanWorkspace after') {
            steps {
                cleanWs()
            }
        }    
    }    
}
'''

// create needed folder(s) for where the jobs are created
folder("PROD")
def folderPath = "PROD"

// job name
String jobName = "${folderPath}/jenkins-pipeline-shared-libraries-newBranch"

pipelineJob(jobName) {

    description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files.
                    |
                    |<h2>Job that creates a new branch for jenkins-pipeline-shared-libraries</h2><br>
                    |This job<br>
                    |- clones a branch from jenkins-pipeline-shared-libraries (parameter BASE_BRANCH)<br>
                    |- checks out a new branch (parameter BRANCH_NAME) based on the BASE_BRANCH<br>
                    |- Maven clean installs the new branch
                    |- if the build was successful and didn't break the new branch (parameter BRANCH_NAME) is pushed to origin<br>
                    |&nbsp;&nbsp;where origin = <i><b>git@github.com:kiegroup/jenkins-pipeline-shared-libraries.git
                    |
                    |""".stripMargin())

    parameters {
        stringParam ("BRANCH_NAME","7.48.x","branch name of the <b><i>new branch</i></b> to create and that will be pushed to blessed repository")
        stringParam ("BASE_BRANCH","main","branch that will be used as base to copy (checkout) a new branch. In most cases this will be the <b><i>main</b></i> branch")
    }

    logRotator {
        numToKeep(10)
    }

    definition {
        cps {
            script("${newShared}")
            sandbox()
        }
    }

}
