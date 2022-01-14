/**
 * job that publishes automatically the ${repo}-website
 */

import org.kie.jenkins.jobdsl.Constants

def baseBranch=Constants.BRANCH

// creation of folder
folder("KIE")
folder("KIE/${baseBranch}")
folder("KIE/${baseBranch}/webs")
def folderPath="KIE/${baseBranch}/webs"

def javadk=Constants.JDK_TOOL
def mvnToolEnv=Constants.MAVEN_TOOL
def AGENT_LABEL="kie-rhel7 && kie-mem4g"

def final DEFAULTS = [
        repository : "jbpm",
        mailRecip : "mbiarnes@redhat.com"
]

def final REPO_CONFIGS = [
        "jbpm"      : []
]

for (reps in REPO_CONFIGS) {
    Closure<Object> get = { String key -> reps.value[key] ?: DEFAULTS[key] }

    String repo = reps.key
    String mailRecip = get("mailRecip")

    def awp = """pipeline {
        agent {
            label "$AGENT_LABEL"
        }
        tools {
            maven "$mvnToolEnv"
            jdk "$javadk"
        }
        stages {
            stage('CleanWorkspace') {
                steps {
                    cleanWs()
                }
            }
            stage ('checkout website') {
                steps {
                    checkout([\$class: 'GitSCM', branches: [[name: 'main']], browser: [\$class: 'GithubWeb', \n
                    repoUrl: 'https://github.com/kiegroup/${repo}-website'], \n
                    doGenerateSubmoduleConfigurations: false, \n
                    extensions: [[\$class: 'RelativeTargetDirectory', relativeTargetDir: '${repo}-website']], \n
                    submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'kie-ci-user-key', url: 'https://github.com/kiegroup/${repo}-website']]])
                    dir("\${WORKSPACE}" + '/${repo}-website') {
                        sh '''pwd  
                           ls -al
                           git branch
                           '''
                           
                    }
                }
            }
            stage('remove docker image kiegroup/${repo}-website if it exists') {
                steps {
                    dir("\${WORKSPACE}") {
                        sh '''#!/bin/bash -e 
                           isImage=\$(docker images -q kiegroup/${repo}-website)
                           echo "Image: \$isImage"
                           if [ "\$isImage" != "" ]; then
                               docker rmi kiegroup/${repo}-website
                           fi
                           '''
                    }
                }
            }
            stage('create directory that stores secrets'){
                steps{
                    withCredentials([sshUserPrivateKey(credentialsId: 'filemgmt-host', keyFileVariable: 'KNOWN_HOSTS')]) {
                        dir("\${WORKSPACE}") {
                            sh '''# create dir where key files are stored 
                               mkdir keys
                               cp \$KNOWN_HOSTS \$WORKSPACE/keys/known_hosts
                               chmod 644 \$WORKSPACE/keys/known_hosts
                               '''
                        }
                    }
                }
            }
            stage('copy secrets to keys') {
                steps {
                    withCredentials([sshUserPrivateKey(credentialsId: 'jbpm-filemgmt', keyFileVariable: 'FILEMGMT_KEY')]) {
                        dir("\${WORKSPACE}") {
                            sh '''
                               cp \$FILEMGMT_KEY \$WORKSPACE/keys/id_rsa
                               chmod 600 \$WORKSPACE/keys/id_rsa
                               ls -l \$WORKSPACE/keys
                               '''
                        }
                    }
                }
            }
            stage('build docker container') {
                steps {
                    withCredentials([sshUserPrivateKey(credentialsId: 'jbpm-filemgmt', keyFileVariable: 'FILEMGMT_KEY')]) {
                        dir("\${WORKSPACE}" + '/jbpm-website') {
                            sh '''
                               docker images
                               docker build -t kiegroup/jbpm-website:latest _dockerPublisher
                               docker run --cap-add net_raw --cap-add net_admin -i --rm --volume "\${WORKSPACE}"/keys/:/home/jenkins/.ssh/:Z --name jbpm-container kiegroup/jbpm-website:latest bash -l -c 'echo "INSIDE THE CONTAINER" && \\n
                               echo "WHOAMI" && whoami && echo "PWD" && pwd && ls -al && echo "inside .ssh" && \\n
                               sudo chown -R jenkins:jenkins /home/jenkins/.ssh && \\n 
                               cd /home/jenkins/.ssh && ls -al && cd /home/jenkins/jbpm-website-main && \\n 
                               sudo rake setup && rake clean build && \\n
                               rsync -Pavqr -e "ssh -i /home/jenkins/.ssh/id_rsa -o KexAlgorithms=diffie-hellman-group1-sha1 -o PubkeyAcceptedKeyTypes=ssh-rsa" --protocol=28 --delete-after _site/* jbpm@filemgmt.jboss.org:/www_htdocs/jbpm/'
                               '''
                        }
                    }
                }
            }
        }
        post {
            failure{
                emailext body: 'Build log: \${BUILD_URL}consoleText \\n' +
                '(IMPORTANT: For visiting the links you need to have access to Red Hat VPN. In case you do not have access to RedHat VPN please download \\n' +
                'and decompress attached file.)',
                subject: 'Build #\${BUILD_NUMBER} of jbpm-web FAILED',
                to: 'mbiarnes@redhat.com'
                cleanWs()          
            }
            fixed {
                emailext body: '',
                subject: 'Build #\${BUILD_NUMBER} of jbpm-web is fixed and was SUCCESSFUL',
                to: 'mbiarnes@redhat.com'
                cleanWs()     
            }
            success {
            cleanWs()
            }                    
        }
    }
"""
    pipelineJob("${folderPath}/${repo}-website-automatic-publishing") {

        description("this is a pipeline job for publishing automatically ${repo}-website")

        parameters {
            wHideParameterDefinition {
                name('AGENT_LABEL')
                defaultValue("${AGENT_LABEL}")
                description('name of machine where to run this job')
            }
            wHideParameterDefinition {
                name('mvnToolEnv')
                defaultValue("${mvnToolEnv}")
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

        properties {
            githubProjectUrl("https://github.com/kiegroup/${repo}-website")
            pipelineTriggers {
                triggers {
                    githubPush()
                }
            }
        }

        definition {
            cps {
                script("${awp}")
                sandbox()
            }
        }

    }

}
