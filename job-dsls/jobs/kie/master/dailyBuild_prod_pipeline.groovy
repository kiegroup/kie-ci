import org.kie.jenkins.jobdsl.Constants

def baseBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def kieVersion=Constants.KIE_PREFIX
def m2Dir = Constants.LOCAL_MVN_REP
def javadk=Constants.JDK_VERSION
def mvnVersion="kie-maven-" + Constants.MAVEN_VERSION
def AGENT_LABEL="kie-linux&&kie-rhel7&&kie-mem24g"

// creation of folder
folder("KIE")
folder ("KIE/${baseBranch}")
folder("KIE/${baseBranch}/daily-build-prod")

def folderPath="KIE/${baseBranch}/daily-build-prod"

def dailyProdBuild='''
pipeline {
    agent {
        label "$AGENT_LABEL"
    }
    options{
        timestamps()
    }    
    tools {
        maven "$mvnVersion"
        jdk "$javadk"
    }
    stages {
        stage('CleanWorkspace') {
            steps {
                cleanWs()
            }
        }
        stage('build sh script') {
            steps {
                script {
                    sh 'touch trace.sh'
                    sh 'chmod 755 trace.sh'
                    sh 'echo "wget --no-check-certificate ${BUILD_URL}consoleText" >> trace.sh'
                    sh 'echo "tail -n 1000 consoleText >> error.log" >> trace.sh'
                    sh 'echo "gzip error.log" >> trace.sh'
                    sh 'cat trace.sh'                
                }
            }
        }        
        stage('Calculate versions') {
            steps {
                script {
                    dataProd = new Date().format('yyyyMMdd')
                    kieVersion = "${kieVersion}.${dataProd}-prod"
                    dockerAbsPath = "KIE/${baseBranch}/Docker"

                    echo "dataProd: ${dataProd}"
                    echo "kieVersion: ${kieVersion}"
                    echo "baseBranch: ${baseBranch}"
                    echo "organization: ${organization}"
                }
            }
        }
        stage ('Checkout droolsjbpm-build-boostrap') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '$baseBranch']], browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/$organization/droolsjbpm-build-bootstrap'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'droolsjbpm-build-bootstrap']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'kie-ci-user-key', url: 'https://github.com/$organization/droolsjbpm-build-bootstrap.git']]])
                dir("${WORKSPACE}" + '/droolsjbpm-build-bootstrap') {
                    sh 'pwd \\n' +
                       'git branch \\n' +
                       'git checkout -b $baseBranch\'
                } 
            }
        }
        stage('Clone all other reps') {
            steps {        
                sshagent(['kie-ci-user-key']) { 
                    sh "./droolsjbpm-build-bootstrap/script/release/01_cloneBranches.sh $baseBranch"
                }
            }   
        }
        stage ('Remove M2') {
            steps {
                sh "./droolsjbpm-build-bootstrap/script/release/eraseM2.sh $m2Dir"
            }
        }         
        stage('Update versions') {
            steps {
                sh "echo 'kieVersion: $kieVersion'"
                sh "./droolsjbpm-build-bootstrap/script/release/03_upgradeVersions.sh $kieVersion"
            }
        }
        stage('Create clean up script') {
            steps {
                sh 'cat > "$WORKSPACE/clean-up.sh" << EOT \\n' +
                        'cd \\\\$1 \\n' +
                        '# Add test reports to the index to prevent their removal in the following step \\n' +
                        'git add --force **target/*-reports/TEST-*.xml \\n' +
                        'git clean -ffdx \\n' +
                        'EOT'
            }
        }
        stage('Clean install'){
            steps {
                configFileProvider([configFile(fileId: '771ff52a-a8b4-40e6-9b22-d54c7314aa1e', targetLocation: 'jenkins-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                    sh "./droolsjbpm-build-bootstrap/script/release/05d_dailyBuildProdInstall.sh $SETTINGS_XML_FILE"
                }
            }
        }                             
    }
    post {
        always {
            script {
                sh './trace.sh\'
            }
            junit '**/target/surefire-reports/**/*.xml\'
        }
        failure{
            emailext body: 'Build log: ${BUILD_URL}consoleText\\n' +
                           'Failed tests (${TEST_COUNTS,var="fail"}): ${BUILD_URL}testReport\\n' +
                           '(IMPORTANT: For visiting the links you need to have access to Red Hat VPN. In case you do not have access to RedHat VPN please download and decompress attached file.)',
                     subject: 'Build #${BUILD_NUMBER} of prod-daily-builds ${baseBranch} branch FAILED',
                     to: 'kie-jenkins-builds@redhat.com',
                     attachmentsPattern: 'error.log.gz\'
            cleanWs()                     
        }
        unstable{
            emailext body: 'Build log: ${BUILD_URL}consoleText\\n' +
                           'Failed tests (${TEST_COUNTS,var="fail"}): ${BUILD_URL}testReport\\n' +
                           '***********************************************************************************************************************************************************\\n' +
                           '${FAILED_TESTS}',
                     subject: 'Build #${BUILD_NUMBER} of prod-daily-builds ${baseBranch} branch was UNSTABLE',
                     to: 'kie-jenkins-builds@redhat.com'
            cleanWs()         
        }
        fixed {
            emailext body: '',
                 subject: 'Build #${BUILD_NUMBER} of prod-daily-builds ${baseBranch} branch is fixed and was SUCCESSFUL',
                 to: 'kie-jenkins-builds@redhat.com'
        }
        success {
            cleanWs()
        }                    
    }       
}
'''

pipelineJob("${folderPath}/daily-build-prod-pipeline-${baseBranch}") {

    description('this is a prod-pipeline job for the daily build of all reps')


    parameters{
        stringParam("kieVersion", "${kieVersion}", "Version of kie. This will be usually set automatically by the parent pipeline job. ")
        stringParam("baseBranch", "${baseBranch}", "kie branch. This will be usually set automatically by the parent pipeline job. ")
        stringParam("organization", "${organization}", "Name of organization. This will be usually set automatically by the parent pipeline job. ")
        wHideParameterDefinition {
            name('m2Dir')
            defaultValue("${m2Dir}")
            description('Path to .m2/repository')
        }
        wHideParameterDefinition {
            name('AGENT_LABEL')
            defaultValue("${AGENT_LABEL}")
            description('name of machine where to run this job')
        }
        wHideParameterDefinition {
            name('mvnVersion')
            defaultValue("${mvnVersion}")
            description('version of maven')
        }
        wHideParameterDefinition {
            name('javadk')
            defaultValue("${javadk}")
            description('version of jdk')
        }
    }

    logRotator {
        numToKeep(5)
    }

    definition {
        cps {
            script("${dailyProdBuild}")
            sandbox()
        }
    }
}


