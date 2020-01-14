import org.kie.jenkins.jobdsl.Constants

def baseBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def kieVersion=Constants.KIE_PREFIX
def m2Dir="\$HOME/.m2/repository"


// creation of folder
folder("daily-build-prod")

def folderPath="daily-build-prod"

def dailyProdBuild='''
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
                checkout([$class: 'GitSCM', branches: [[name: '$baseBranch']], browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/$organization/droolsjbpm-build-bootstrap'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'droolsjbpm-build-bootstrap']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/$organization/droolsjbpm-build-bootstrap.git']]])
                dir("${WORKSPACE}" + '/droolsjbpm-build-bootstrap') {
                    sh 'pwd \\n' +
                       'git branch \\n' +
                       'git checkout -b $baseBranch\'
                } 
            }
        }
        stage('Clone all other reps') {
            steps {
                sh "sh droolsjbpm-build-bootstrap/script/release/01_cloneBranches.sh $baseBranch"
            }
        }
        stage('Update versions') {
            steps {
                sh "echo 'kieVersion: $kieVersion'"
                sh "sh droolsjbpm-build-bootstrap/script/release/03_upgradeVersions.sh $kieVersion"
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
                    sh "sh droolsjbpm-build-bootstrap/script/release/05d_dailyBuildProdInstall.sh $SETTINGS_XML_FILE"
                }
            }
        }
        stage('Publish JUnit test results reports') {
            steps {
              junit '**/target/*-reports/TEST-*.xml'    
            }
        }                
        stage('Delete workspace when build is done') {
            steps {
                cleanWs()
            }
        }                
    }
    post {
        failure{           
            emailext body: 'prod daily build #${BUILD_NUMBER} (${baseBranch} branch) was: ' + "${currentBuild.currentResult}" +  '\\n' +
                'Please look here: ${BUILD_URL} \\n' +
                ' \\n' +                 
                '${BUILD_LOG, maxLines=750}', subject: 'prod-daily-build-${baseBranch} #${BUILD_NUMBER}: ' + "${currentBuild.currentResult}", to: 'kie-jenkins-builds@redhat.com'
        }
        unstable{
            emailext body: 'prod daily build #${BUILD_NUMBER} of ${baseBranch} was:' + "${currentBuild.currentResult}" +  '\\n' +
                'Please look here: ${BUILD_URL} \\n' +
                ' \\n' +                
                'Failed tests: ${BUILD_URL}/testReport \\n' +
                ' \\n' +                 
                '${BUILD_LOG, maxLines=750}', subject: 'prod-daily-build-${baseBranch} #${BUILD_NUMBER}: ' + "${currentBuild.currentResult}", to: 'kie-jenkins-builds@redhat.com'    
        }
        success{
            emailext body: 'prod daily build #${BUILD_NUMBER} of ${baseBranch} was:' + "${currentBuild.currentResult}" +  '\\n' +
                'Please look here: ${BUILD_URL}', subject: 'prod-daily-build-${baseBranch} #${BUILD_NUMBER}: ' + "${currentBuild.currentResult}", to: 'mbiarnes@redhat.com, mnovotny@redhat.com'            
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
    }

    logRotator {
        numToKeep(10)
        daysToKeep(10)
    }

    definition {
        cps {
            script("${dailyProdBuild}")
            sandbox()
        }
    }
}


