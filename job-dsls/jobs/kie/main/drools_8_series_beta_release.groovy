/**
 * Job that runs the community release pipeline, but only over the reps until & incl. droolsjbpm-integration
 */

import org.kie.jenkins.jobdsl.Constants

def kieVersion=Constants.DROOLS_8_SERIES + ".Beta"
def baseBranch=Constants.BRANCH
def releaseBranch="r" + Constants.DROOLS_8_SERIES + ".Beta"
def organization=Constants.GITHUB_ORG_UNIT
def m2Dir = Constants.LOCAL_MVN_REP
def commitMsg="Upgraded version to "
def javadk=Constants.JDK_VERSION
def mvnVersion="kie-maven-" + Constants.MAVEN_VERSION
def AGENT_LABEL="kie-rhel7-pipeline&&kie-mem24g&&!kie-releases"

// Creation of folders where jobs are stored
folder("KIE")
folder("KIE/${baseBranch}")
folder("KIE/${baseBranch}/drools-8-series-beta-release")
def folderPath = ("KIE/${baseBranch}/drools-8-series-beta-release")


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
        stage('Checkout drools') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '$baseBranch']], browser: [$class: 'GithubWeb', repoUrl: 'git@github.com:$organization/drools.git'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'drools']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'kie-ci-user-key', url: 'git@github.com:$organization/drools.git']]])
                dir("${WORKSPACE}" + '/drools') {
                    sh  'pwd \\n' +
                        'git branch \\n' +
                        'git checkout -b $baseBranch'
                }
            }    
        }
        // checks if release branch already exists
        stage ('Check if branch exists') {
            steps{
                sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/drools') {
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
        // if release branch doesn't exist it will be created
        stage('Create drools release branch') {
            when{
                expression { branchExists == '0'}
            }
            steps {
                sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/drools') {
                        sh 'git checkout -b $releaseBranch $baseBranch'
                    }    
                }
            }
        }
        // part of the Maven rep will be erased
        stage ('Remove M2') {
            steps {
                sh "rm -rf ${m2Dir}/drools"
            }
        }                
        // poms will be upgraded to new version ($kieVersion)
        stage('Update versions') {
            when{
                expression { branchExists == '0'}
            }
            steps {
                sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/drools') {
                        configFileProvider([configFile(fileId: '771ff52a-a8b4-40e6-9b22-d54c7314aa1e', targetLocation: 'jenkins-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                            echo 'kieVersion: ' + "{$kieVersion}"
                            // update parent pom
                            sh 'mvn -B -N -e -s $SETTINGS_XML_FILE versions:update-parent -Dfull -DparentVersion="[$kieVersion]" -DallowSnapshots=true -DgenerateBackupPoms=false'
                            // update child poms
                            sh 'mvn -B -N -e -s $SETTINGS_XML_FILE versions:update-child-modules -Dfull -DallowSnapshots=true -DgenerateBackupPoms=false'
                        }
                    }        
                }    
            }
        }
        stage ('Add and commit version upgrades') {
            when{
                expression { branchExists == '0'}
            }
            steps {
                sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/drools') {
                        echo 'kieVersion: ' + "{$kieVersion}"
                        echo 'commitMsg: ' + "{$commitMsg}"
                        sh 'git add .'
                        sh 'git commit -m "${commitMsg} ${kieVersion}"'
                    }    
                }
            }
        }
        //release branches are pushed to github
        stage('Push release branches') {
            when{
                expression { branchExists == '0'}
            }
            steps {
                sshagent(['kie-ci-user-key']) {            
                    dir("${WORKSPACE}" + '/drools') {
                        sh 'git push origin $releaseBranch'
                    }
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
                    dir("${WORKSPACE}" + '/drools') {
                        sh 'git fetch origin'
                        sh 'git checkout ' + "$releaseBranch"
                    }
                }    
            }
        }
        // mvn clean deploy of drools repository to a locally directory that will be uploaded later on - this saves time
        stage('Build & deploy drools repository locally'){
            steps {
                sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/drools') {            
                        configFileProvider([configFile(fileId: '771ff52a-a8b4-40e6-9b22-d54c7314aa1e', targetLocation: 'jenkins-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                            sh 'deployDir=$WORKSPACE/drools/drools-deploy-dir'
                            sh 'mvn-all.sh -B -e -U clean deploy -s $SETTINGS_XML_FILE -Dkie.maven.settings.custom=$SETTINGS_XML_FILE -Dfull -Drelease -Pwildfly -DaltDeploymentRepository=local::default::file://$deployDir -Dmaven.test.failure.ignore=true -Dgwt.memory.settings="-Xmx10g" -Prun-code-coverage -Dcontainer.profile=wildfly -Dcontainer=wildfly -Dintegration-tests=true -Dmaven.wagon.httpconnectionManager.ttlSeconds=25 -Dmaven.wagon.http.retryHandler.count=3'
                        }
                    }
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
               sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/drools/drools-deploy-dir') { 
                        configFileProvider([configFile(fileId: '3f317dd7-4d08-4ee4-b9bb-969c309e782c', targetLocation: 'uploadNexus-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                            script {
                                stagingRep=15c58a1abc895b
                                deployDir=$WORKSPACE/drools/drools-deploy-dir
                                // upload the content to remote staging repo on Nexus
                                mvn -B -e -s ${SETTINGS_XML_FILE} -Dkie.maven.settings.custom=${SETTINGS_XML_FILE} org.sonatype.plugins:nexus-staging-maven-plugin:1.6.5:deploy-staged-repository -DnexusUrl=https://repository.jboss.org/nexus -DserverId=jboss-releases-repository -DrepositoryDirectory=$deployDir -DstagingProfileId=$stagingRep -DstagingDescription="kie-$kieVersion" -DkeepStagingRepositoryOnCloseRuleFailure=true -DkeepStagingRepositoryOnFailure=true -DstagingProgressTimeoutMinutes=120'
                            }
                        }    
                    }        
                }    
            }
        }
        // the tag of the drools release will be created and pushed to github
        stage('Push community tag') {
            steps {
               sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/drools') {
                        script {
                            sh 'git tag -a $kieVersion" -m "tagged ${kieVersion}"' 
                            sh 'git push -n origin $kieVersion'
                        }    
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


pipelineJob("${folderPath}/drools-8-series-pipeline-${baseBranch}") {

    description('This job is a pipeline for a drools 8 series release<br>The reps in repository-list will be<br>droolsjbpm-build-bootstrap<br>droolsjbpm-knowledge<br>drools')

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

