import org.kie.jenkins.jobdsl.Constants

def javadk=Constants.JDK_VERSION
def mvnVersion="kie-maven-3.5.2"
def javaToolEnv="KIE_JDK1_8"
def mvnToolEnv="KIE_MAVEN_3_5_2"
def mvnHome="${mvnToolEnv}_HOME"
def kogitoVersion=Constants.KOGITO_PREFIX
def baseBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def m2Dir = Constants.LOCAL_MVN_REP

// creation of folder
folder ("kogito")
folder("kogito/daily-build")

def folderPath="kogito/daily-build"

def kogitoDailyBuild='''
pipeline {
    agent {
        label 'kie-linux&&kie-rhel7&&kie-mem24g'
    }
    tools {
        maven 'kie-maven-3.5.2'
        jdk 'kie-jdk1.8'
    }
    stages {
        stage('CleanWorkspace') {
            steps {
                cleanWs()
            }
        }
        stage('Calculate versions') {
            steps {
                script {
                    data = new Date().format('yyyyMMdd-hhMMss')
                    kogitoVersion = "${kogitoVersion}.${data}"

                    echo "data: ${data}"
                    echo "kogitoVersion: ${kogitoVersion}"
                    echo "baseBranch: ${baseBranch}"
                    echo "organization: ${organization}"
                }
            }
        }
        stage ('Clone KOGITO repositories') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '$baseBranch']], browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/$organization/kogito-bom'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'kogito-bom']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/$organization/kogito-bom.git']]])
                checkout([$class: 'GitSCM', branches: [[name: '$baseBranch']], browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/$organization/kogito-runtimes'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'kogito-runtimes']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/$organization/kogito-runtimes.git']]])
                checkout([$class: 'GitSCM', branches: [[name: '$baseBranch']], browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/$organization/kogito-cloud'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'kogito-cloud']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/$organization/kogito-cloud.git']]])
                checkout([$class: 'GitSCM', branches: [[name: '$baseBranch']], browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/$organization/kogito-examples'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'kogito-examples']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/$organization/kogito-examples.git']]])                
            }
        }
        stage ('Remove M2') {
            steps {
                sh 'if [ -d $m2Dir ]; then \\n' +
                        'rm -rf $m2Dir/org/kogito \\n' +
                    'fi'
            }
        }         
        stage('Update versions') {
            steps {
                sh "echo 'kogitoVersion: $kogitoVersion'"
                sh "sh kogito-bom/scripts/release/02-update-version-all.sh $kogitoVersion"
            }
        }
        stage('Install repositories'){
            steps {
                configFileProvider([configFile(fileId: '771ff52a-a8b4-40e6-9b22-d54c7314aa1e', targetLocation: 'jenkins-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                    sh "cd kogito-bom && mvn -B -e -U clean install -Dfull -s $SETTINGS_XML_FILE -Dmaven.test.redirectTestOutputToFile=true -Dmaven.test.failure.ignore=true"
                    sh "cd kogito-runtimes && mvn -B -e -U clean install -Dfull -s $SETTINGS_XML_FILE -Dmaven.test.redirectTestOutputToFile=true -Dmaven.test.failure.ignore=true"
                    sh "cd kogito-cloud && mvn -B -e -U clean install -Dfull -s $SETTINGS_XML_FILE -Dmaven.test.redirectTestOutputToFile=true -Dmaven.test.failure.ignore=true"
                    sh "cd kogito-examples && mvn -B -e -U clean install -Dfull -s $SETTINGS_XML_FILE -Dmaven.test.redirectTestOutputToFile=true -Dmaven.test.failure.ignore=true"
                }
            }
        }
        stage('Publish JUnit test results reports') {
            steps {
              junit '**/target/*-reports/TEST-*.xml'    
            }
        }                                               
    }
    post {
        failure{        
            emailext body: '${baseBranch}:kogito-daily-build #${BUILD_NUMBER} was: ' + "${currentBuild.currentResult}" +  '\\n' +
                    'Please look here: ${BUILD_URL} \\n' +
                    ' \\n' +                    
                    '${BUILD_LOG, maxLines=750}', subject: '${baseBranch}:kogito-daily-build #${BUILD_NUMBER}: ' + "${currentBuild.currentResult}", to: 'kie-jenkins-builds@redhat.com'
        }
        unstable{
            emailext body: '${baseBranch}:kogito-daily-build #${BUILD_NUMBER} was: ' + "${currentBuild.currentResult}" +  '\\n' +
                    'Please look here: ${BUILD_URL} \\n' +
                    ' \\n' +
                    'Failed tests: ${BUILD_URL}/testReport \\n' +
                    ' \\n' +
                    '${BUILD_LOG, maxLines=750}', subject: '${baseBranch}:kogito-daily-build #${BUILD_NUMBER}: ' + "${currentBuild.currentResult}", to: 'kie-jenkins-builds@redhat.com'   
        }
        success{
            emailext body: '${baseBranch}:kogito-daily-build #${BUILD_NUMBER} was:' + "${currentBuild.currentResult}" +  '\\n' +
                    'Please look here: ${BUILD_URL}', subject: '${baseBranch}:kogito-daily-build #${BUILD_NUMBER}: ' + "${currentBuild.currentResult}", to: 'kie-jenkins-builds@redhat.com'             
        }            
    }    
}
'''

pipelineJob("${folderPath}/kogito-daily-build-pipeline-${baseBranch}") {

    description('this is a pipeline job for the daily build of all reps')


    parameters{
        stringParam("kogitoVersion", "${kogitoVersion}", "Version of kie. This will be usually set automatically by the parent pipeline job. ")
        stringParam("baseBranch", "${baseBranch}", "kie branch. This will be usually set automatically by the parent pipeline job. ")
        stringParam("organization", "${organization}", "Name of organization. This will be usually set automatically by the parent pipeline job. ")
        wHideParameterDefinition {
            name('m2Dir')
            defaultValue("${m2Dir}")
            description('Path to .m2/repository')
        }
    }

    logRotator {
        numToKeep(10)
        daysToKeep(10)
    }

    definition {
        cps {
            script("${kogitoDailyBuild}")
            sandbox()
        }
    }
}