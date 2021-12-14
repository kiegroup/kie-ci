// pipeline DSL job to bump up the kie-prefix (KIE_PREFIX) in Constants of kie-jenkins-scripts

import org.kie.jenkins.jobdsl.Constants
def AGENT_LABEL="kie-rhel7 && kie-mem8g"
def MVN_TOOL = Constants.MAVEN_TOOL
def JDK_TOOL = Constants.JDK_TOOL
def BASE_BRANCH = ""
def KIE_PREFIX = ""
def ORGANIZATION=""
def COMMIT_MSG="upgraded kie-prefix to "

def updateKiePrefix='''
pipeline {
    agent {
        label "$AGENT_LABEL"
    }
    options{
        timestamps()
    }
    tools {
        maven "$MVN_TOOL"
        jdk "$JDK_TOOL"
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
        stage('clone kie-jenkins-scripts') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '$BASE_BRANCH']], browser: [$class: 'GithubWeb', repoUrl: 'git@github.com:$ORGANIZATION/kie-jenkins-scripts.git'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'kie-jenkins-scripts']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'kie-ci-user-key', url: 'git@github.com:$ORGANIZATION/kie-jenkins-scripts.git']]])
                dir("${WORKSPACE}" + '/kie-jenkins-scripts') {
                    sh 'pwd \\n' +
                    'git branch \\n' +
                    'git checkout -b $BASE_BRANCH \\n' +
                    'git remote -v'
                }
            }
        }
        stage ('create upstream for kie-jenkins-scripts'){
            steps {
                sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/kie-jenkins-scripts') {
                        sh 'git push --set-upstream origin $BASE_BRANCH'
                    }
                }
            }
        }
        stage('change version via sed'){
            steps{
                dir("${WORKSPACE}" + '/kie-jenkins-scripts') {
                    sh 'sed -i "s/KIE_PREFIX = .*/KIE_PREFIX = \\'${KIE_PREFIX}\\'/g" job-dsls/src/main/groovy/org/kie/jenkins/jobdsl/Constants.groovy'
            
                }    
            }
        }
        stage('view changes'){
            steps{
                sh 'cat kie-jenkins-scripts/job-dsls/src/main/groovy/org/kie/jenkins/jobdsl/Constants.groovy'
            }
        }
        stage ('add and commit version upgrades') {
            steps {
                dir("${WORKSPACE}" + '/kie-jenkins-scripts'){
                    sh 'git add .\'
                    sh 'git commit -m "${COMMIT_MSG} ${KIE_PREFIX}"'
                }
            }
        }        
        stage('push BASE_BRANCH to origin') {
            steps {
                sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/kie-jenkins-scripts') {
                        sh 'git push origin'
                    }
                }
            }
        }
    }
    post{
        always{
            cleanWs()
        }
    }    
}
'''

// creates folder if is not existing
folder("KIE")
folder("KIE/kie-tools")
folder("KIE/kie-tools/upgradeVersions")
def folderPath="KIE/kie-tools/upgradeVersions"

pipelineJob("${folderPath}/upgrade-kie-prefix") {

    description('Pipeline job for upgrading the kie-prefix in Constants of kie-jenkins-scripts')

    parameters {
        stringParam("BASE_BRANCH", "${BASE_BRANCH}", "Branch to clone and update")
        stringParam("KIE_PREFIX", "${KIE_PREFIX}", "the new KIE prefix. i.e. 7.65.0")
        stringParam("ORGANIZATION", "${ORGANIZATION}", "organization of github: mostly kiegroup")
        wHideParameterDefinition {
            name('AGENT_LABEL')
            defaultValue("${AGENT_LABEL}")
            description('name of machine where to run this job')
        }
        wHideParameterDefinition {
            name('MVN_TOOL')
            defaultValue("${MVN_TOOL}")
            description('version of maven')
        }
        wHideParameterDefinition {
            name('JDK_TOOL')
            defaultValue("${JDK_TOOL}")
            description('version of jdk')
        }
        wHideParameterDefinition {
            name('COMMIT_MSG')
            defaultValue("${COMMIT_MSG}")
            description('the commit message')
        }
    }

    logRotator {
        numToKeep(5)
    }

    definition {
        cps {
            script("${updateKiePrefix}")
            sandbox()
        }
    }
}