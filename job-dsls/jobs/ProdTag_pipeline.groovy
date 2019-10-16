import org.kie.jenkins.jobdsl.Constants

def kieVersion=Constants.KIE_PREFIX
def baseBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def TPB="target product build"
def tagName="sync-xxx-date"
def m2Dir="\$HOME/.m2/repository"
def reportBranch=Constants.REPORT_BRANCH
def MAVEN_OPTS="-Xms1g -Xmx3g"
def cutOffDate = new Date().format('yyyy-MM-dd')
def commitMsg_1="Remove npm and yarn files "
def commitMsg_2="Upgraded version to "

// creation of folder
folder("ProdTag")

def folderPath="ProdTag"

def prodTag='''
pipeline {
    agent {
        label 'kie-releases'
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
        stage('Checkout droolsjbpm-build-bootstrap') {
        steps {
                checkout([$class: 'GitSCM', branches: [[name: '$baseBranch']], browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/$organization/droolsjbpm-build-bootstrap'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'droolsjbpm-build-bootstrap']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/$organization/droolsjbpm-build-bootstrap.git']]])
                dir("${WORKSPACE}" + '/droolsjbpm-build-bootstrap') {
                    sh 'pwd \\n' +
                       'git branch \\n' +
                       'git checkout -b $baseBranch'
                } 
            }
        }
        stage('Clone all other reps') {
            steps {
                sh "sh droolsjbpm-build-bootstrap/script/release/01_cloneBranches.sh $baseBranch"
            }
        }
        stage ('Remove npm and yarn files'){
            steps {
                sh "sh droolsjbpm-build-bootstrap/script/release/kie-deleteNpmLockFiles.sh"
            }
        }
        stage ('Add and commit removed npm/yarn files') {
            steps {
                echo 'commitMsg: $commitMsg_1'
                sh 'sh droolsjbpm-build-bootstrap/script/release/addAndCommit.sh "$commitMsg_1" '
            }
        }     
        stage('Update versions') {
            steps {
                echo 'kieVersion: $kieVersion'
                sh 'sh droolsjbpm-build-bootstrap/script/release/03_upgradeVersions.sh $kieVersion'
            }
        }
        stage ('Add and commit version upgrades') {
            steps {
                echo 'kieVersion: $kieVersion'
                echo 'commitMsg: $commitMsg_2'                
                sh 'sh droolsjbpm-build-bootstrap/script/release/addAndCommit.sh "$commitMsg_2" $kieVersion'
            }
        }
        stage('Build & deploy repositories locally'){
            steps {
                configFileProvider([configFile(fileId: '771ff52a-a8b4-40e6-9b22-d54c7314aa1e', targetLocation: 'jenkins-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                    sh "sh droolsjbpm-build-bootstrap/script/release/05b_prodDeployLocally.sh $SETTINGS_XML_FILE"
                }
            }
        }
        stage('Create log file for reports') {
            steps {
                    sh 'pwd \\n' +
                       './droolsjbpm-build-bootstrap/script/git-all.sh log -11 --pretty=oneline >> log.txt'
            }
        }        
        stage('Publish JUnit test results reports') {
            steps {
              junit '**/target/*-reports/TEST-*.xml'    
            }
        }         
        stage ('Send email with BUILD result') {
            steps {
                emailext body: 'Build of prod tag #${BUILD_NUMBER} of ${baseBranch} branch for ${TPB} was:  ' + "${currentBuild.currentResult}" +  '\\n' +
                    ' \\n' +
                    'Please look here: ${BUILD_URL} \\n' +
                    ' \\n' +
                    '${BUILD_LOG, maxLines=750}', subject: 'Build of prod tag for ${TPB} ', to: 'mbiarnes@redhat.com'
            }    
        }
        stage('Approval (Point of NO return)') {
            steps {
                input message: 'Was the build good enough to create a tag', ok: 'Continue with creating and pushing tag'
            }
        }   
        stage ('Add remote pointing to Gerrit') {
            steps {
               sh "sh droolsjbpm-build-bootstrap/script/release/07_addRemoteToGerrit.sh"
            }
        }  
        stage ('Create & push tags to Gerrit') {
            steps {
                echo 'Name of the tag: $tagName\'
                sh 'git config --global push.default simple'
                sshagent(['code.engineering.redhat.com']) {
                    sh './droolsjbpm-build-bootstrap/script/release/08b_prodPushTags.sh $tagName'
                } 
            }
        }
        stage('Clone, create and push kie-release-reports') {
            steps {
                sshagent(['kie-ci-user-key']) {
                    sh 'git clone git@github.com:jboss-integration/kie-release-reports.git -b $reportBranch'
                    dir('kie-release-reports') {
                        sh 'git branch \\n' +
                           'cp ../log.txt script/ \\n' +
                           'sh script/infoRelease.sh' + ' ' +  "${baseBranch}" + ' ' + "${tagName}" + ' ' + "${TPB}" + ' ' + "${cutOffDate}" + '\\n' +
                           'git status \\n' +
                           'git add . \\n' +
                           'git commit -m "added report for "' + "${TPB}" + '\\n' +
                           'git remote -v \\n' +
                           'git push origin $reportBranch'
                    }    
                }
            }
        }                  
        stage ('Send mail') {
            steps {
                emailext body: 'The tags ' + "${tagName}" + ' are available on Gerrit (https://code.engineering.redhat.com/gerrit/) \\n' +
                'For more information please look at handover report: https://github.com/jboss-integration/kie-release-reports/blob/' + "${reportBranch}" + '/reports/tags/' + "${tagName}" + '/' + "${tagName}" + '.txt \\n' +
                '\\n' +
                'Component versions: \\n' +
                'kieVersion:' + "${kieVersion}" + '\\n' +
                '\\n' +
                'Test Results: https://rhba-jenkins.rhev-ci-vms.eng.rdu2.redhat.com/job/KIE/job/' + "${baseBranch}" + '/job/release/job/buildAndDeployLocally-kieReleases-' + "${baseBranch}" + '/' + "${BUILD_NUMBER}" + '/testReport/',
                subject: '${TPB} tag available ', to: 'mbiarnes@redhat.com'
            }    
        }                                                     
    }
}
'''


pipelineJob("${folderPath}/prodTag-pipeline-${baseBranch}") {

    description('this is a pipeline job for a tag for productization of all reps')


    parameters{
        stringParam("kieVersion", "${kieVersion}", "Please edit the version of kie i.e 7.26.1.2019.09.26.prod ")
        stringParam("baseBranch", "${baseBranch}", "Please edit the name of the kie branch ")
        stringParam("organization", "${organization}", "Please edit the name of organization.")
        stringParam("tagName", "${tagName}", "Please edit name of the prod tag - i.e. sync-XXX-yyyy.dd.mm ")
        stringParam("TPB", "${TPB}","Please edit the name of the target product build i.e. RHPAM 7.5.0.CR1")
        wHideParameterDefinition {
            name('reportBranch')
            defaultValue("${reportBranch}")
            description('Please edit branch for release reports on jboss-integration')
        }
        wHideParameterDefinition {
            name('MAVEN_OPTS')
            defaultValue("${MAVEN_OPTS}")
            description('Please edit the Maven options')
        }
        wHideParameterDefinition {
            name('cutOffDate')
            defaultValue("${cutOffDate}")
            description('Please edit the cutOffDate')
        }
        wHideParameterDefinition {
            name('commitMsg_1')
            defaultValue("${commitMsg_1}")
            description('Please edit the commitMsg')
        }
        wHideParameterDefinition {
            name('commitMsg_2')
            defaultValue("${commitMsg_2}")
            description('Please edit the commitMsg')
        }
    }

    logRotator {
        numToKeep(10)
        daysToKeep(10)
    }

    definition {
        cps {
            script("${prodTag}")
            sandbox()
        }
    }

    publishers {
        buildDescription ("KIE version ([^\\s]*)")
        mailer('mbiarnes@redhat.com', false, false)
    }

}
