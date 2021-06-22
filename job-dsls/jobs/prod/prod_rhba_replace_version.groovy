def prodVersion = '''
@Library('jenkins-pipeline-shared-libraries')_

pipeline {
    agent {
        label 'kie-rhel7 && !master'
    }
    tools {
        maven 'kie-maven-3.5.4'
        jdk 'kie-jdk1.8'
    }
    parameters {
        string(description: 'Initial branch, in which the new KIE version should be applied.', name: 'BASE_BRANCH', defaultValue: 'master')
        string(description: 'The community version', name: 'VERSION_ORG_KIE', defaultValue: '7.50.0-SNAPSHOT')
        string(description: 'The new product version', name: 'VERSION_RHBA', defaultValue: '7.11.0')
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
                    println "Setting 'version.org.kie' property to ${VERSION_ORG_KIE}."
                    def SETTINGS_XML_ID = "5d9884a1-178a-4d67-a3ac-9735d2df2cef"

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

                                    println "Executing maven set-property for ${project}"
                                    configFileProvider([configFile(fileId: SETTINGS_XML_ID, variable: 'MAVEN_SETTINGS_XML')]) {
                                        sh "mvn -s $MAVEN_SETTINGS_XML versions:set -DnewVersion=${VERSION_RHBA} -DgenerateBackupPoms=false"
                                        sh "mvn -s $MAVEN_SETTINGS_XML versions:set-property -Dproperty=version.org.kie -DnewVersion=${VERSION_ORG_KIE} -DgenerateBackupPoms=false"
                                    }

                                    def changes = sh(returnStdout: true, script: 'git status -s').split()
                                    println "changes ${changes}"
                                    if(changes.size() > 0) {
                                        println 'There are changes'
                                        sh 'git add pom.xml'
                                        sh 'git config user.email "kie-ci1@redhat.com"'
                                        sh 'git config user.name "kie-ci1"'
                                        sh "git commit -m 'version.org.kie updated to ${VERSION_ORG_KIE}'"
                                        sh 'git push'
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
String jobName = "${folderPath}/rhba-prod-replace-version.org.kie"

pipelineJob(jobName) {

    description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files.
                    |
                    |This job has to be run when community master was upgraded to the next SNAPSHOT version.
                    |
                    |""".stripMargin())

    parameters {
        stringParam ("VERSION_ORG_KIE","7.50.0-SNAPSHOT","The recent community (SNAPSHOT) version")
    }

    logRotator {
        numToKeep(10)
    }

    definition {
        cps {
            script("${prodVersion}")
            sandbox()
        }
    }

}
