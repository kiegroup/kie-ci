// pipeline DSL job for uploading kogito-docs from main-kogito branch

import org.kie.jenkins.jobdsl.Constants

def currentKieSnapshot = "7.45.0-SNAPSHOT"
def nextKieSnapshot = "7.46.0-SNAPSHOT"
def currentKogitoDocsSnaphot = "0.17.0-SNAPSHOT"
def currentKogitoDocsVersion = "0.17.0"
def currentKogitoDocsTagName = "0.17.0-kogito"
def nextKogitoDocsSnapshot = "0.18.0-SNAPSHOT"
def sshKogitoDocsPath = "kogito@filemgmt.jboss.org:/docs_htdocs/kogito/release"
def javadk=Constants.JDK_VERSION
def mvnVersion="kie-maven-" + Constants.MAVEN_VERSION
def AGENT_LABEL="kie-rhel7 && kie-mem4g"

// creation of folder
folder("KIE")
folder ("KIE/kogito")
folder ("KIE/kogito/kogito-docs")

def folderPath="KIE/kogito/kogito-docs"


def uploadDocs='''
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
        stage ('checkout kie-docs') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: 'main-kogito']], browser: [$class: 'GithubWeb', repoUrl: 'git@github.com:kiegroup/kie-docs.git'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'kie-docs']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'kie-ci-user-key', url: 'git@github.com:kiegroup/kie-docs.git']]])
                dir("${WORKSPACE}" + '/kie-docs') {
                    sh 'pwd \\n' +
                       'ls -al \\n' +
                       'git checkout -b main-kogito \\n' +
                       'git branch \\n' +
                       'git config --global user.email "kieciuser@gmail.com" \\n' +
                       'git config --global user.name "kieciuser"\'                       
                } 
            }
        }
        stage('replace versions of org.kie.kogito:kogito-docs') {
            steps {
                dir("${WORKSPACE}" + '/kie-docs/doc-content/kogito-docs') {
                    sh 'sed -i "s/<version>${currentKogitoDocsSnaphot}<\\\\/version>/<version>${currentKogitoDocsVersion}<\\\\/version>/" pom.xml \\n' +
                       'git add . \\n' +
                       'git commit -m "upgraded to ${currentKogitoDocsVersion} for tagging" \\n' +
                       'git log -1 --pretty=oneline'                    
                }
            }    
        }
        stage('build kogito-docs') {
            steps {
                configFileProvider([configFile(fileId: '771ff52a-a8b4-40e6-9b22-d54c7314aa1e', targetLocation: 'jenkins-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                    dir("${WORKSPACE}" + '/kie-docs/doc-content/kogito-docs') {     
                        sh 'mvn clean install -Dfull -s $SETTINGS_XML_FILE -Dkie.maven.settings.custom=$SETTINGS_XML_FILE -Dmaven.test.redirectTestOutputToFile=true -Dmaven.test.failure.ignore=true \\n' +
                           /* clean files created bythe build */
                           'git clean -d -f'
                    }            
                }        
            }
        }
        stage('upload of kogito-docs to filemgmt.jboss.org ') {
            steps {
                sshagent(credentials: ['KogitoDocsUpload']) {
                    dir("${WORKSPACE}" + '/kie-docs/doc-content/kogito-docs') {
                        /* create dir on filemgmt */
                        sh 'touch upload_version \\n' +
                            'echo "mkdir" $currentKogitoDocsVersion > upload_version \\n' +
                            'chmod +x upload_version \\n' +
                            'cat upload_version \\n' +
                            /* upload upload_version to filemgmt for creating a release directory */
                            'sftp -b upload_version $sshKogitoDocsPath \\n' +
                            /* upload of the kogito-docs to filemgmt.jboss.org */
                            'scp -r target/generated-docs/html_single $sshKogitoDocsPath/$currentKogitoDocsVersion \\n' +
                            'rm upload_version \\n' +
                            /* upload latest links */
                            'mkdir filemgmt_link \\n' +
                            'cd filemgmt_link \\n' +
                            'touch $currentKogitoDocsVersion \\n' +
                            'ln -s $currentKogitoDocsVersion latest \\n' +
                            'rsync -a --protocol=28 latest $sshKogitoDocsPath \\n' +
                            'echo "symbolic links uploaded" \\n' +
                            'cd .. \\n' +
                            'rm -rf filemgmt_link'                            
                    }        
                }
            }
        }
        stage('create and push tag') {
            steps {
                sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/kie-docs') {
                        sh 'git tag -a $currentKogitoDocsTagName -m "tagged ${currentKogitoDocsTagName}" \\n' +
                           'git push origin $currentKogitoDocsTagName'
                    }
                }        
            }
        }
        stage('bump up KIE and kogito-docs to next SNAPSHOT') {
            steps {
                dir("${WORKSPACE}" + '/kie-docs') {
                    sh 'sed -i "s/<version>${currentKieSnapshot}<\\\\/version>/<version>${nextKieSnapshot}<\\\\/version>/" pom.xml \\n' +
                       'sed -i "s/<version>${currentKieSnapshot}<\\\\/version>/<version>${nextKieSnapshot}<\\\\/version>/" doc-content/pom.xml \\n' +
                       'sed -i "s/<version>${currentKieSnapshot}<\\\\/version>/<version>${nextKieSnapshot}<\\\\/version>/" doc-content/kogito-docs/pom.xml \\n' +
                       'sed -i "s/<version>${currentKogitoDocsVersion}<\\\\/version>/<version>${nextKogitoDocsSnapshot}<\\\\/version>/" doc-content/kogito-docs/pom.xml \\n' +
                       /* uploading changed poms to kogito-main branch */
                       'git add . \\n' +
                       'git commit -m "upgraded kogito-docs to ${nextKogitoDocsSnapshot}" '
                } 
            }
        }
        stage('push latest commits to origin') {
            steps {
                sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/kie-docs') {
                        sh 'git push origin main-kogito'   
                    }
                }
            }
        }        
        post {
            failure{
                emailext body: 'Build log: ${BUILD_URL}consoleText\\n' +
                               'Failed tests (${TEST_COUNTS,var="fail"}): ${BUILD_URL}testReport\\n' +
                               '(IMPORTANT: For visiting the links you need to have access to Red Hat VPN. In case you do not have access to RedHat VPN please download and decompress attached file.)',
                         subject: 'Build #${BUILD_NUMBER} of kogito docs-upload FAILED',
                         to: 'sterobin@redhat.com, hmanwani@redhat.com, mbiarnes@redhat.com'
                cleanWs()                      
            }
            unstable{
                emailext body: 'Build log: ${BUILD_URL}consoleText\\n' +
                               'Failed tests (${TEST_COUNTS,var="fail"}): ${BUILD_URL}testReport\\n' +
                               '***********************************************************************************************************************************************************\\n' +
                               '${FAILED_TESTS}',
                         subject: 'Build #${BUILD_NUMBER} of kogito docs-upload branch was UNSTABLE',
                         to: 'sterobin@redhat.com, hmanwani@redhat.com, mbiarnes@redhat.com' 
                cleanWs()                            
            }
            fixed {
                emailext body: '',
                     subject: 'Build #${BUILD_NUMBER} of kogito docs-upload was fixed and is SUCCESSFUL',
                     to: 'sterobin@redhat.com, hmanwani@redhat.com, mbiarnes@redhat.com\'
                cleanWs()
            }
            success{
                emailext body: 'Everything worked fine',
                     subject: 'Build #${BUILD_NUMBER} of kogito docs-upload was SUCCESSFUL',
                     to: 'sterobin@redhat.com, hmanwani@redhat.com, mbiarnes@redhat.com'  
                cleanWs()                           
            }        
        }                 
    }  
}
'''

pipelineJob("${folderPath}/uploadKogitoDocs") {

    description('''this job <b>uploadKogitoDocs</b>: <br><br>
1. clones kie-docs - <b>“main-kogito”</b>  branch <br>
2. upgrades the kie-docs/doc-content/kogito-docs/pom.xml to <b>$currentKogitoDocsVersion</b> <br>
3. executes a mvn clean install in kie-docs/doc-content/kogito-docs <br>
4. creates a folder <b>$newKogitoDocsVersion</b> in filemgmt.jboss.org:docs_htdocs/kogito/release <br>
5. uploads target/generated-docs/html_single to filemgmt.jboss.org:docs_htdocs/kogito/release/ <br>
6. removes a symbolic link on filemgmt.jboss.org:docs_htdocs/kogito/release/ pointing to dir latest and upgrades the symbolic link pointing to the directory <b>$currentKogitoDocsVersion/html_single)</b> <br> 
7. creates a tag <b>$currentKogitoDocsTagName</b> and pushes it to GitHub  <br>
8. bumps up kogito-docs to the <b>$nextKogitoDocsSnapshot</b> <br> 
9. pushes to GitHub the commit that bumps up kogito-docs to <b>$kogitoDocsNextSnapshot</b> and all other poms in kogito-docs to the <b>$nextKieSnapshot</b>
''')

    parameters {
        stringParam("currentKieSnapshot","${currentKieSnapshot}","please enter the <b>current kie snapshot version</b> in poms of <b>main-kogito</b> branch<br>look at <br> https://github.com/kiegroup/kie-docs/blob/main-kogito/pom.xml OR <br>https://github.com/kiegroup/kie-docs/blob/main-kogito/doc-content/pom.xml OR <br>https://github.com/kiegroup/kie-docs/blob/main-kogito/doc-content/kogito-docs/pom.xml#L9")
        stringParam("nextKieSnapshot","${nextKieSnapshot}","please enter the <b>next kie snapshot version</b>")
        stringParam("currentKogitoDocsSnaphot", "${currentKogitoDocsSnaphot}", "please enter the <b>current kogito-docs snapshot version</b>")
        stringParam("currentKogitoDocsVersion", "${currentKogitoDocsVersion}", "please enter the <b>current kogito-docs version</b>")
        stringParam("currentKogitoDocsTagName","${currentKogitoDocsTagName}","please enter the name of the tag for this kogito release")
        stringParam("nextKogitoDocsSnapshot","${nextKogitoDocsSnapshot}","please enter the <b>next kogito-docs snapshot version</b>")
        wHideParameterDefinition {
            name('sshKogitoDocsPath')
            defaultValue("${sshKogitoDocsPath}")
            description('Please edit the path to filemgm.jboss.org')
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
        numToKeep(5)
    }

    definition {
        cps {
            script("${uploadDocs}")
            sandbox()
        }
    }
}