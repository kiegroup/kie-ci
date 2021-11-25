/**
 * Job that runs the community release pipeline, but only over the reps until & incl. droolsjbpm-integration
 */

import org.kie.jenkins.jobdsl.Constants

def kieVersion=Constants.KIE_PREFIX
def baseBranch=Constants.BRANCH
def releaseBranch="r7.45.0.t20201015"
def organization=Constants.GITHUB_ORG_UNIT
def m2Dir = Constants.LOCAL_MVN_REP
def commitMsg="Upgraded version to "
def javadk=Constants.JDK_TOOL
def mvnVersion="kie-maven-" + Constants.MAVEN_VERSION
def AGENT_LABEL="kie-rhel7 && kie-mem24g"

// Creation of folders where jobs are stored
folder("KIE")
folder("KIE/${baseBranch}")
folder("KIE/${baseBranch}/reduced-drools-release")
def folderPath = ("KIE/${baseBranch}/reduced-drools-release")


def redRelease='''
pipeline {
    agent {
        label "$AGENT_LABEL"
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
        stage('User metadata'){
            steps {
                sh "git config --global user.email kieciuser@gmail.com"
                sh "git config --global user.name kie-ci"
            }
        }        
        stage('Checkout droolsjbpm-build-bootstrap') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '$baseBranch']], browser: [$class: 'GithubWeb', repoUrl: 'git@github.com:$organization/droolsjbpm-build-bootstrap.git'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'droolsjbpm-build-bootstrap']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'kie-ci-user-key', url: 'git@github.com:$organization/droolsjbpm-build-bootstrap.git']]])
                dir("${WORKSPACE}" + '/droolsjbpm-build-bootstrap') {
                    sh  'pwd \\n' +
                        'git branch \\n' +
                        'git checkout -b $baseBranch'
                }
            }    
        }
        stage ('Replace repository-list.txt') {
            steps {
                configFileProvider([configFile(fileId: '1a43573a-318c-426a-bb2b-c9df7fe97a02', targetLocation: 'repository-list.txt', variable: 'REP_LIST')]) {
                    dir("${WORKSPACE}") {
                        sh 'cp repository-list.txt droolsjbpm-build-bootstrap/script/repository-list.txt \\n' +
                           'cat droolsjbpm-build-bootstrap/script/repository-list.txt \\n' +
                           'rm droolsjbpm-build-bootstrap/script/branched-7-repository-list.txt'
                    }       
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
        // checks if release branch already exists
        stage ('Check if branch exists') {
            steps{
                sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/droolsjbpm-build-bootstrap') {
                        script {
                            branchExists = sh(script: 'git ls-remote --heads origin ${releaseBranch} | wc -l', returnStdout: true).trim()
                        }
                    }
                }
            }
        }
        stage ('Log results') {
            steps {
                echo 'branchExists: ' + "$branchExists"
                script {
                    if ( "$branchExists" == "1") {
                        echo "branch exists"
                    } else {
                        echo "branch does not exist"
                    }
                }
            }
        }
        // if release branches doesn't exist they will be created
        stage('Create release branches') {
            when{
                expression { branchExists == '0'}
            }
            steps {
                sshagent(['kie-ci-user-key']) {
                    sh './droolsjbpm-build-bootstrap/script/release/02_createReleaseBranches.sh $releaseBranch'
                }
            }
        }
        // part of the Maven rep will be erased
        stage ('Remove M2') {
            steps {
                sh "./droolsjbpm-build-bootstrap/script/release/eraseM2.sh $m2Dir"
            }
        }                
        // poms will be upgraded to new version ($kieVersion)
        stage('Update versions') {
            when{
                expression { branchExists == '0'}
            }
            steps {
                configFileProvider([configFile(fileId: '771ff52a-a8b4-40e6-9b22-d54c7314aa1e', targetLocation: 'jenkins-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                    echo 'kieVersion: ' + "{$kieVersion}"
                    sh './droolsjbpm-build-bootstrap/script/release/03_upgradeVersions.sh $kieVersion'
                }    
            }
        }
        stage ('Add and commit version upgrades') {
            when{
                expression { branchExists == '0'}
            }
            steps {
                echo 'kieVersion: ' + "{$kieVersion}"
                echo 'commitMsg: ' + "{$commitMsg}"
                sh './droolsjbpm-build-bootstrap/script/release/addAndCommit.sh "$commitMsg" $kieVersion'
            }
        }
        //release branches are pushed to github
        stage('Push release branches') {
            when{
                expression { branchExists == '0'}
            }
            steps {
                sshagent(['kie-ci-user-key']) {
                    sh './droolsjbpm-build-bootstrap/script/release/04_pushReleaseBranches.sh $releaseBranch'
                }
            }
        }
        // if the release branches already exist they will be fetched from github
        stage('Pull from existing release Branches') {
            when{
                expression { branchExists == '1'}
            }
            steps {
                sshagent(['kie-ci-user-key']) {
                    sh './droolsjbpm-build-bootstrap/script/git-all.sh fetch origin\'
                    sh './droolsjbpm-build-bootstrap/script/git-all.sh checkout ' + "$releaseBranch"
                }
            }
        }
        // mvn clean deploy of each repository to a locally directory that will be uploaded later on - this saves time
        stage('Build & deploy repositories locally'){
            steps {
                configFileProvider([configFile(fileId: '771ff52a-a8b4-40e6-9b22-d54c7314aa1e', targetLocation: 'jenkins-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                    sh './droolsjbpm-build-bootstrap/script/release/05a_communityDeployLocally.sh $SETTINGS_XML_FILE'
                }
            }
        }
        stage ('Send mail only if build fails') {
            when{
                expression { currentBuild.currentResult == 'FAILURE'}
            }        
            steps {
                emailext body: 'Status of drools build for ${kieVersion} was: ' + "${currentBuild.currentResult}" +  '\\n' +
                    'Please look here: ${BUILD_URL} \\n' +
                    ' \\n' +                    
                    '${BUILD_LOG, maxLines=750}', subject: 'drools-release for ${kieVersion} failed', to: 'mbiarnes@redhat.com' 
            }
        }                               
        stage('Publish JUnit test results reports') {      
            steps {
              junit '**/target/*-reports/TEST-*.xml'    
            }
        } 
        // binaries created in previous step will be uploaded to Nexus
        stage('Upload binaries to staging repository to Nexus') {        
            steps {
                configFileProvider([configFile(fileId: '3f317dd7-4d08-4ee4-b9bb-969c309e782c', targetLocation: 'uploadNexus-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                    sh './droolsjbpm-build-bootstrap/script/release/06_uploadBinariesToNexus.sh $SETTINGS_XML_FILE\'
                }    
            }
        }
        // the tags of the release will be created and pushed to github
        stage('Push community tag') {
            steps {
                sshagent(['kie-ci-user-key']) {
                    script {
                        sh './droolsjbpm-build-bootstrap/script/release/08a_communityPushTags.sh'
                    }
                }
            }        
        }                                      
        stage ('Email send with BUILD result') {       
            steps {
                emailext body: 'Build of drools ${kieVersion} was:  ' + "${currentBuild.currentResult}" +  '\\n' +
                    ' \\n' +
                    'Failed tests: $BUILD_URL/testReport \\n' +
                    ' \\n' +
                    ' \\n' +
                    ' \\n' +
                    'KIE version: ' + "${kieVersion}" + '\\n' +
                    ' \\n' +
                    ' \\n' +
                    'You have to go to Nexus and still release the closed staging-release!',
                     subject: 'reduced-release-${baseBranch} ${kieVersion} status and artefacts', to: 'mbiarnes@redhat.com'
            }    
        }                                                                           
    }
}
'''


pipelineJob("${folderPath}/drools-pipeline-${baseBranch}") {

    description('This job is a pipeline for a drools release (reduced repositories)<br>The reps in repository-list will be from lienzo-core until droolsjbpm-integration')

    parameters{
        stringParam("kieVersion", "${kieVersion}", "please edit the version for the current release, format should be: i.e. 7.45.0.t20201014 ")
        stringParam("baseBranch", "${baseBranch}", "please edit the name of the branch ")
        stringParam("releaseBranch", "${releaseBranch}", "please edit name of the current release branch, format should be: i.e. r7.45.0.t20201014 - branch should start with <b>r</b>")
        stringParam("organization", "${organization}", "please edit the name of organization ")
        wHideParameterDefinition {
            name('commitMsg')
            defaultValue("${commitMsg}")
            description('Please edit the commitMsg')
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
            script("${redRelease}")
            sandbox()
        }
    }

}

