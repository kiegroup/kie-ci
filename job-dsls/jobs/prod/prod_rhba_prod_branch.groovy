def newBranch = '''
@Library('jenkins-pipeline-shared-libraries')_

pipeline {
    agent {
        label 'kie-rhel7 && !built-in'
    }
    options {
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10')
        timeout(time: 10, unit: 'MINUTES')
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
                    println "Creating new branch ${BRANCH_NAME}."

                    configFileProvider([configFile(fileId: "49737697-ebd6-4396-9c22-11f7714808eb", variable: 'PRODUCTION_PROJECT_LIST')]) {
                        println "Reading file ${PRODUCTION_PROJECT_LIST} jenkins file"
                        def productionProjectListFile = readFile "${env.PRODUCTION_PROJECT_LIST}"
                        def projectCollection = productionProjectListFile.readLines()
                        println "Project Collection ${projectCollection}."
                        
                        projectCollection.each { project ->
                            def projectGroupName = util.getProjectGroupName(project)
                            def group = projectGroupName[0]
                            def name = projectGroupName[1]
                            dir("${env.WORKSPACE}/${group}_${name}") {
                                withCredentials([usernamePassword(credentialsId: 'kie-ci', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                                    def encodedPass = URLEncoder.encode(PASS, 'UTF-8')
                                    sh "git clone https://${USER}:${encodedPass}@github.com/${group}/${name} ."
                                    sh "git checkout -b ${BRANCH_NAME}"
                                    sh 'git config user.email "kie-ci1@redhat.com"'
                                    sh 'git config user.name "kie-ci1"'
                                    sh "git push origin ${BRANCH_NAME}"
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
String jobName = "${folderPath}/rhba-prod-branch"

pipelineJob(jobName) {

    description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files.
                    |
                    |This job has to be run when a new branch for prod is required.
                    |
                    |""".stripMargin())

    parameters {
        stringParam ("BRANCH_NAME","7.48.x","The new branch to create")
    }

    logRotator {
        numToKeep(10)
    }

    definition {
        cps {
            script("${newBranch}")
            sandbox()
        }
    }

}
