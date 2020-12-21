/**
 * job that publishes automatically the ${repo}-website
 */

import org.kie.jenkins.jobdsl.Constants

// creation of folder
folder("webs")
def folderPath="webs"

def javadk=Constants.JDK_VERSION
def mvnVersion="kie-maven-" + Constants.MAVEN_VERSION
def AGENT_LABEL="kie-rhel7 && kie-mem4g"

def final DEFAULTS = [
        repository : "drools",
        mailRecip : "mbiarnes@redhat.com"
]

def final REPO_CONFIGS = [
        "drools"    : [],
        "jbpm"      : [repository : "jbpm"],
        "optaplanner" : [
                repository : "optaplanner",
                mailRecip  : DEFAULTS["mailRecip"] + ",gdsmet@redhat.com"
        ]
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
            maven "$mvnVersion"
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
                    checkout([\$class: 'GitSCM', branches: [[name: 'master']], browser: [\$class: 'GithubWeb', repoUrl: 'https://github.com/kiegroup/${repo}-website'], doGenerateSubmoduleConfigurations: false, extensions: [[\$class: 'RelativeTargetDirectory', relativeTargetDir: '${repo}-website']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'kie-ci-user-key', url: 'https://github.com/kiegroup/${repo}-website']]])
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
                    dir("\${WORKSPACE}" + '/${repo}-website') {
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
            stage('create directory that stores key files'){
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
            stage('build docker container') {
                steps {
                    withCredentials([sshUserPrivateKey(credentialsId: '${repo}-filemgmt', keyFileVariable: 'FILEMGMT_KEY')]) {
                        dir("\${WORKSPACE}" + '/${repo}-website') {
                            sh '''cp \$FILEMGMT_KEY \$WORKSPACE/keys/id_rsa
                               chmod 600 \$WORKSPACE/keys/id_rsa
                               ls -l \$WORKSPACE/keys/id_rsa
                               docker images
                               docker build -t kiegroup/${repo}-website:latest _dockerPublisher
                               docker run --cap-add net_raw --cap-add net_admin -i --rm --volume "\${WORKSPACE}"/keys/:/home/jenkins/.ssh/:Z --name ${repo}-container kiegroup/${repo}-website:latest bash -l -c 'echo "INSIDE THE CONTAINER" && echo "WHOAMI" && whoami && echo "PWD" && pwd && ls -al && echo "inside .ssh" && cd /home/jenkins/.ssh && ls -al && cd /home/jenkins/${repo}-website-master && sudo rake setup && rake clean build && rake publish'
                               '''
                        }
                    }
                }
            }
        }
        post {
            failure{
                emailext body: 'Build log: \${BUILD_URL}consoleText\\n' +
                '(IMPORTANT: For visiting the links you need to have access to Red Hat VPN. In case you do not have access to RedHat VPN please download and decompress attached file.)',
                subject: 'Build #\${BUILD_NUMBER} of ${repo}-web FAILED',
                to: '${mailRecip}'
                cleanWs()          
            }
            fixed {
                emailext body: '',
                subject: 'Build #\${BUILD_NUMBER} of ${repo}-web is fixed and was SUCCESSFUL',
                to: '${mailRecip}'
                cleanWs()     
            }
            success {
            cleanWs()
            }                    
        }
    }
"""
    pipelineJob("${folderPath}/${repo}-automatic-web-publishing") {

        description("this is a pipeline job for publishing automatically ${repo}-website")

        parameters {
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
                script("${awp}")
                sandbox()
            }
        }

        properties {
            githubProjectUrl("https://github.com/kiegroup/${repo}-website")
        }

        triggers {
            scm("H/10 * * * *") {
                ignorePostCommitHooks(false)
            }
        }


    }

}