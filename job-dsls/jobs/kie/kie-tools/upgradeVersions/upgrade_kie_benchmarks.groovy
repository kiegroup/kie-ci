// pipeline DSL job to bump up the branch (BASE_BRANCH) of kie-benchmarks to certain version (NEW_KIE_VERSION)

import org.kie.jenkins.jobdsl.Constants
def AGENT_LABEL="kie-rhel7 && kie-mem8g"
def MVN_TOOL = Constants.MAVEN_TOOL
def JDK_TOOL = Constants.JDK_TOOL
def BASE_BRANCH = ""
def CURRENT_KIE_VERSION = ""
def NEW_KIE_VERSION=""
def ORGANIZATION=""
def COMMIT_MSG="upgraded kie version to "


def updateKieBench='''
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
        stage('clone kie-benchmarks') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '$BASE_BRANCH']], browser: [$class: 'GithubWeb', repoUrl: 'git@github.com:$ORGANIZATION/kie-benchmarks.git'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'kie-benchmarks']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'kie-ci-user-key', url: 'git@github.com:$ORGANIZATION/kie-benchmarks.git']]])
                dir("${WORKSPACE}" + '/kie-benchmarks') {
                    sh 'pwd \\n' +
                    'git branch \\n' +
                    'git checkout -b $BASE_BRANCH \\n' +
                    'git remote -v'
                }
            }
        }
        stage ('create upstream for kie-benchmarks'){
            steps {
                sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/kie-benchmarks') {
                        sh 'git push --set-upstream origin $BASE_BRANCH'
                    }
                }
            }
        }
        stage('upgrade versions') {
            steps {
                dir("${WORKSPACE}" + '/kie-benchmarks'){
                    sh './update-version.sh $NEW_KIE_VERSION'
                }
            }
        }
        stage ('add and commit version upgrades') {
            steps {
                dir("${WORKSPACE}" + '/kie-benchmarks'){
                    echo "NEW_KIE_VERSION: ${NEW_KIE_VERSION}"
                    echo "COMMIT_MSG: ${COMMIT_MSG}"
                    sh 'git add .'
                    sh 'git commit -m "${COMMIT_MSG} ${NEW_KIE_VERSION}"'
                }
            }
        }
        stage('push BASE_BRANCH to origin') {
            steps {
                sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/kie-benchmarks') {
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

pipelineJob("${folderPath}/upgrade-kie-benchmarks") {

    description('Pipeline job for upgrading the version of kie-benchmarks on BASE_BRANCH')

    parameters {
        stringParam("BASE_BRANCH", "${BASE_BRANCH}", "Branch to clone and update")
        stringParam("CURRENT_KIE_VERSION", "${CURRENT_KIE_VERSION}", "the current version of KIE repositories on BASE_BRANCH")
        stringParam("NEW_KIE_VERSION", "${NEW_KIE_VERSION}", "KIE versions on BASE_BRANCH should be bumped up to this version")
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
            script("${updateKieBench}")
            sandbox()
        }
    }
}