/**
 * job that publishes automatically the ${repo}-website
 */

// creation of folder
folder("webs")
def folderPath="webs"

def final DEFAULTS = [repository : "drools"]

def final REPO_CONFIGS = [
        "drools"    : [],
        "jbpm"      : [repository : "jbpm"],
        "optaplanner" : [repository : "optaplanner"]
]

for (reps in REPO_CONFIGS) {
    Closure<Object> get = { String key -> reps.value[key] ?: DEFAULTS[key] }

    String repo = reps.key

    def awp = """pipeline {
        agent {
            label 'kie-rhel7 && kie-mem4g'
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
            stage ('checkout website') {
                steps {
                    checkout([\$class: 'GitSCM', branches: [[name: 'master']], browser: [\$class: 'GithubWeb', repoUrl: 'https://github.com/kiegroup/${repo}-website'], doGenerateSubmoduleConfigurations: false, extensions: [[\$class: 'RelativeTargetDirectory', relativeTargetDir: '${repo}-website']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/kiegroup/${repo}-website']]])
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
            always {
                cleanWs()             
            }
        } 
    }
"""


    pipelineJob("${folderPath}/${repo}-automatic-web-publishing") {

        description("this is a pipeline job for publishing automatically ${repo}-website")

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