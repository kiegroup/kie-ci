import org.kie.jenkins.jobdsl.Constants

def javadk=Constants.JDK_VERSION
def mvnVersion="kie-maven-" + Constants.MAVEN_VERSION
def baseBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def AGENT_LABEL="kie-releases"

// creation of folder
folder("KIE")
folder("KIE/${baseBranch}")
folder("KIE/${baseBranch}/i18n_Translations")

def folderPath="KIE/${baseBranch}/i18n_Translations"

def i18n_trans= '''
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
        stage('Checkout droolsjbpm-build-bootstrap') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '$baseBranch']], browser: [$class: 'GithubWeb', repoUrl: 'git@github.com:$organization/droolsjbpm-build-bootstrap.git'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'droolsjbpm-build-bootstrap']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'kie-ci-user-key', url: 'git@github.com:$organization/droolsjbpm-build-bootstrap.git']]])
                dir("${WORKSPACE}" + '/droolsjbpm-build-bootstrap') {
                    sh  'pwd \\n' +
                        'git branch \\n' +
                        'git checkout -b $baseBranch \\n' +
                        'git branch'
                }
            }    
        }
        stage('update repository-list.txt'){
            steps {
                dir("${WORKSPACE}") {
                    /* replaces repository list by the repositories containing i18n directories*/
                    configFileProvider([configFile(fileId: 'e70f7a3d-2d99-4f4c-821e-4085287cacfe', targetLocation: 'repository-list.txt', variable: 'NEW_REP_LIST')]) {
                        sh 'cp $NEW_REP_LIST droolsjbpm-build-bootstrap/script \\n' +
                           'cat droolsjbpm-build-bootstrap/script/repository-list.txt'
                    }                     
                }    
            }
        }
        stage ('Clone others'){
            steps {
                sshagent(['kie-ci-user-key']) {
                    sh './droolsjbpm-build-bootstrap/script/git-clone-others.sh'
                }    
            }
        }
        stage ('Find all properties'){
            steps {
                dir("${WORKSPACE}"){
                    sh \'\'\'
                        date=$(date '+%Y-%m-%d')
                        for REPOSITORY in $(cat repository-list.txt) ; do
                            echo "************************"      
                            echo "Repository: " $REPOSITORY
                            echo "************************"
                            cd $REPOSITORY
                            pwd
                            find . -path "*/i18n/*" -type f -name "*.properties" -not -name "*_*.properties" -exec zip -u ${REPOSITORY}_properties.zip {} \\\\;
                            mv ${REPOSITORY}_properties.zip $WORKSPACE
                            cd ..
                        done
                        zip -m ${date}-kieTranslations.zip *_properties.zip\'\'\'
                }
            }
        }    
        stage('Archiving artifacts') {
            steps {
                dir("${WORKSPACE}") {
                    archiveArtifacts '*.zip'
                }
            }
        }        
    }
    post {
        success {
            emailext body: 'Here are the most recent tarbnslation file (*Constants.properties) from the kiegroup repositories',
            subject: 'kiegroup i18n translations',
            to: 'mbiarnes@redhat.com',
            attachmentsPattern: '*.zip'
            cleanWs()    
        }
    }
}
'''

pipelineJob("${folderPath}/translations") {

    description('this is a pipeline job for the daily build of all reps')


    parameters{
        stringParam("baseBranch", "${baseBranch}", "kie branch. This will be usually set automatically by the parent pipeline job. ")
        stringParam("organization", "${organization}", "Name of organization. This will be usually set automatically by the parent pipeline job. ")
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
        numToKeep(10)
    }

    definition {
        cps {
            script("${i18n_trans}")
            sandbox()
        }
    }
}