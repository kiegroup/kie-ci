// pipeline DSL job to bump up a branch ($baseBranch) to certain version ($nextDevVersion)

import org.kie.jenkins.jobdsl.Constants

def nextDevVer="x.x.0-SNAPSHOT"
def baseBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT



// creation of folder
folder("KIE")

def folderPath="KIE"


def bumpUp='''
pipeline {
    agent {
        label 'kie-rhel7-priority'      
    }
    tools {
        maven 'kie-maven-3.6.3'
        jdk 'kie-jdk1.8'
    }
    stages {
        stage('CleanWorkspace') {
            steps {
                cleanWs()
            }
        }
        stage ('Checkout droolsjbpm-build-boostrap') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '$baseBranch']], browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/$organization/droolsjbpm-build-bootstrap'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'droolsjbpm-build-bootstrap']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'kie-ci-user-key', url: 'https://github.com/$organization/droolsjbpm-build-bootstrap.git']]])
                dir("${WORKSPACE}" + '/droolsjbpm-build-bootstrap') {
                    sh 'pwd \\n' +
                       'git branch \\n' +
                       'git checkout -b $baseBranch'
                } 
            }
        }
        
        stage('Clone all other reps') {
            sshagent(['kie-ci-user-key']) {        
                steps {
                    sh "sh droolsjbpm-build-bootstrap/script/release/01_cloneBranches.sh $baseBranch"
                }
            }   
        }
            
        stage('Update versions') {
            steps {
                sh "echo 'next development version: $nextDevVer'"
                sh "sh droolsjbpm-build-bootstrap/script/release/03_upgradeVersions.sh $nextDevVer"
            }
        }
                      
        stage ('deploy to next development version') {
            steps {
                configFileProvider([configFile(fileId: '7774c60d-cab3-425a-9c3b-26653e5feba1', targetLocation: 'uploadNexus-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                    sh "./droolsjbpm-build-bootstrap/script/mvn-all.sh -B -e clean deploy -Dfull -Drelease -s $SETTINGS_XML_FILE -Dkie.maven.settings.custom=$SETTINGS_XML_FILE -DskipTests -Dgwt.compiler.skip=true -Dgwt.skipCompilation=true"
                }
            }    
        }             
    }  
}
'''

pipelineJob("${folderPath}/deploy-development-version") {

    description('''
    Pipeline job for upgrading all kiegroup reps to the next SNAPSHOT version<br>
    and deploy it to SNAPSHOT Nexus.<br>
    It is needed that the new SNAPSHOTS exist before bumping-up and pushing to any branch, so developer haven't to wait a full day for the new SNAPSHOTS.<br>
    They will be overwritten whith any deploy job.<br>
    <b>Run this job please just before a you have to bump up the versions of any branch.</b>
    ''')

    parameters {
        stringParam("nextDevVer", "${nextDevVer}", "Next development version (xxx-SNAPSHOT) kie will be upgraded to.")
        stringParam("baseBranch", "${baseBranch}", "kie branch. This will be usually set automatically by the parent pipeline job.")
        stringParam("organization", "${organization}", "Name of organization. This will be usually set automatically by the parent pipeline job.")
    }

    logRotator {
        numToKeep(5)
    }

    definition {
        cps {
            script("${bumpUp}")
            sandbox()
        }
    }
}