def replaceShared = '''
@Library('jenkins-pipeline-shared-libraries')_

pipeline {
    agent {
        label 'kie-rhel8 && !built-in'
    }
    options {
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10')
        timeout(time: 20, unit: 'MINUTES')
    }
    stages {
        stage('Initialize') {
            steps {
                sh 'printenv'

            }
        }
        stage('Replace projects') {
            steps {
                script {
                    def sharedLibrariesProjects = ['kiegroup/droolsjbpm-build-bootstrap': ['.ci/jenkins/Jenkinsfile', '.ci/jenkins/Jenkinsfile.nightly'], 'jboss-integration/rhba' : ['.ci/jenkins/Jenkinsfile']]
                    sharedLibrariesProjects.each { key, val ->
                        println "[INFO] Treating project ${key}"
                        def projectGroupName = util.getProjectGroupName(key) 
                        dir("${projectGroupName[0]}_${projectGroupName[1]}") {
                            withCredentials([usernamePassword(credentialsId: 'kie-ci', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                                def encodedPass = URLEncoder.encode(PASS, 'UTF-8')
                                sh "git clone -b ${BRANCH_NAME} https://${USER}:${encodedPass}@github.com/${key} ."
                                
                                val.each { file -> 
                                    println "[INFO] Treating file ${file}"
                                    sh """
                                    sed -i "/^@Library('jenkins-pipeline-shared-libraries')_/c\\\\@Library('jenkins-pipeline-shared-libraries@${BRANCH_NAME}')_" ${file}
                                    """
                                    sh "git add ${file}"
                                }
                                
                                def changes = sh(returnStdout: true, script: 'git status -s').split()
                                println "changes ${changes}"
                                if(changes.size() > 0) {
                                    println 'There are changes'
                                    sh 'git config user.email "kie-ci1@redhat.com"'
                                    sh 'git config user.name "kie-ci1"'
                                    sh "git commit -m 'pipelines shared libraries import pointing to updated to ${BRANCH_NAME}'"
                                    sh 'git push origin ${BRANCH_NAME}'
                                } else {
                                    println 'No changes, so, nothing to push'
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}
'''
// create needed folder(s) for where the jobs are created
folder("PROD")
def folderPath = "PROD"

// job name
String jobName = "${folderPath}/replace-jenkins-pipeline-shared-libraries"

pipelineJob(jobName) {

    description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files.
                    |
                    |Will update Jenkinsfile pointing to the new shared-libraries version
                    |
                    |""".stripMargin())

    parameters {
        stringParam ("BRANCH_NAME","7.48.x","The community projects branch")
    }

    logRotator {
        numToKeep(10)
    }

    definition {
        cps {
            script("${replaceShared}")
            sandbox()
        }
    }

}
