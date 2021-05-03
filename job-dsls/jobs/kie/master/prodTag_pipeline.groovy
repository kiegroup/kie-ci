import org.kie.jenkins.jobdsl.Constants

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

def kieVersion=Constants.KIE_PREFIX
def baseBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def m2Dir = Constants.LOCAL_MVN_REP
def TPB=""
def tagName="sync-xxx-date"
def reportBranch=Constants.REPORT_BRANCH
def MAVEN_OPTS="-Xms1g -Xmx3g"
def cutOffDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
def commitMsg="Upgraded version to "
def javadk=Constants.JDK_VERSION
def mvnVersion="kie-maven-" + Constants.MAVEN_VERSION
def AGENT_LABEL="kie-rhel7 && kie-mem24g"


// Creation of folders where jobs are stored
folder("KIE")
folder("KIE/${baseBranch}")
folder("KIE/${baseBranch}/prod-tag")
def folderPath = ("KIE/${baseBranch}/prod-tag")

def productTag='''
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
        stage('Check TPB'){
            steps{
                script {
                    if (params.TPB == '') {
                        echo "Target Product Build Parameter was not set"
                        currentBuild.result = 'ABORTED'
                        error('Please set TPB')
                    }
                }                
            }
        }        
        stage('Checkout droolsjbpm-build-bootstrap') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '$baseBranch']], browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/$organization/droolsjbpm-build-bootstrap'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'droolsjbpm-build-bootstrap']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'kie-ci-user-key', url: 'https://github.com/$organization/droolsjbpm-build-bootstrap.git']]])
                dir("${WORKSPACE}" + '/droolsjbpm-build-bootstrap') {
                    sh 'pwd \\n' +
                       'git branch \\n' +
                       'git checkout -b $baseBranch'
                } 
            }
        }
        stage ('Clone others'){
            steps {
                sshagent(['kie-ci-user-key']) {
                    sh './droolsjbpm-build-bootstrap/script/release/01_cloneBranches.sh $baseBranch\'
                }    
            }
        } 
        stage ('Global git conf') {
            steps{
                sh 'git config --global user.name "kie-ci"'
                sh 'git config --global user.email "kieciuser@gmail.com"'                
            }
        }
        stage ('Remove M2') {
            steps {
                sh "./droolsjbpm-build-bootstrap/script/release/eraseM2.sh $m2Dir"
            }
        }                   
        stage('Update versions') {
            steps {
                echo 'kieVersion: ' + "$kieVersion"
                sh './droolsjbpm-build-bootstrap/script/release/03_upgradeVersions.sh $kieVersion'
            }
        }
        stage ('Add and commit version upgrades') {
            steps {
                echo 'kieVersion: ' + "$kieVersion"
                echo 'commitMsg: ' + "$commitMsg"                
                sh './droolsjbpm-build-bootstrap/script/release/addAndCommit.sh "$commitMsg" $kieVersion'
            }
        }
        stage('Clean install repositories '){
            steps {
                configFileProvider([configFile(fileId: '771ff52a-a8b4-40e6-9b22-d54c7314aa1e', targetLocation: 'jenkins-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                    sh "./droolsjbpm-build-bootstrap/script/release/05b_prodInstall.sh $SETTINGS_XML_FILE"
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
                emailext body: 'Build of prod tag ${tagName} of ${baseBranch} branch for ${TPB} was:  ' + "${currentBuild.currentResult}" +  '\\n' +
                    ' \\n' +
                    'Please look here: ${BUILD_URL} \\n' +
                    ' \\n' +
                    ' \\n' +
                    'Failed tests: ${BUILD_URL}testReport \\n' +
                    ' \\n' +                    
                    '${BUILD_LOG, maxLines=750}', subject: 'prod-tag-${baseBranch} for ${TPB}', to: 'bxms-prod@redhat.com'
            }    
        }
        stage('Approval (Point of NO return)') {
            steps {
                input message: 'Was the build good enough to create a tag', ok: 'Continue with creating and pushing tag'
            }
        }   
        stage ('Add remote pointing to Gerrit') {
            steps {
               sh "./droolsjbpm-build-bootstrap/script/release/07_addRemoteToGerrit.sh"
            }
        }  
        stage ('Create & push tags to Gerrit') {
            steps {
                echo 'Name of the tag: ' + "$tagName"
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
                           './script/infoRelease.sh' + ' ' +  "${baseBranch}" + ' ' + "${tagName}" + ' ' + "${TPB}" + ' ' + "${cutOffDate}" + '\\n' +
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
                'Test Results: ${BUILD_URL}testReport',
                subject: 'prod-tag-${baseBranch} for ${TPB}', to: 'bsig@redhat.com, bxms-prod@redhat.com, bpms-pm-list@redhat.com'
            }    
        }                                                     
    }
    post {
        failure{        
            emailext body: 'Build of prod tag #${BUILD_NUMBER} of ${baseBranch} branch for ${TPB} failed \\n' +
                    'Please look here: ${BUILD_URL} \\n' +
                    ' \\n' +                    
                    '${BUILD_LOG, maxLines=750}', subject: 'prod-tag-${baseBranch} for ${TPB}: ' + "${currentBuild.currentResult}", to: 'bxms-prod@redhat.com'
        }
    }       
}
'''


pipelineJob("${folderPath}/prod-tag-pipeline-${baseBranch}") {

    description('this is a pipeline job for a tag for productization of all reps')


    parameters{
        stringParam("kieVersion", "${kieVersion}", "Please edit the version of kie i.e 7.26.1.20190926-prod ")
        stringParam("baseBranch", "${baseBranch}", "Please edit the name of the kie branch ")
        stringParam("organization", "${organization}", "Please edit the name of organization.")
        stringParam("tagName", "${tagName}", "Please edit name of the prod tag - i.e. sync-XXX-yyyy.dd.mm ")
        stringParam("cutOffDate","$cutOffDate","Please edit the cut off date here in format yyyy-MM-dd")
        stringParam("TPB", "${TPB}","Please edit the name of the target product build i.e. RHPAM_7.5.0.CR1")
        stringParam("reportBranch","${reportBranch}","Please edit branch for release reports on jboss-integration")
        wHideParameterDefinition {
            name('MAVEN_OPTS')
            defaultValue("${MAVEN_OPTS}")
            description('Please edit the Maven options')
        }
        wHideParameterDefinition {
            name('commitMsg')
            defaultValue("${commitMsg}")
            description('Upgraded version to ')
        }
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
        numToKeep(3)
    }

    definition {
        cps {
            script("${productTag}")
            sandbox()
        }
    }

}
