// pipeline DSL job for uploading kogito-docs from main-kogito branch

import org.kie.jenkins.jobdsl.Constants

def currentKieSnapshot = "7.69.0-SNAPSHOT"
def nextKieSnapshot = "7.70.0-SNAPSHOT"
def currentKogitoDocsSnaphot = "1.21.0-SNAPSHOT"
def currentKogitoDocsVersion = "1.21.0"
def currentKogitoDocsTagName = "1.21.0-kogito"
def nextKogitoDocsSnapshot = "1.22.0-SNAPSHOT"
def sshKogitoDocsPath = "kogito@filemgmt-prod.jboss.org"
def rsync_KogitoDocsPath="kogito@filemgmt-prod-sync.jboss.org"
def javadk=Constants.JDK_TOOL
def mvnToolEnv=Constants.MAVEN_TOOL
def BASE_BRANCH=Constants.BRANCH
def GH_ORG_UNIT=Constants.GITHUB_ORG_UNIT
def AGENT_LABEL="kie-rhel7 && kie-mem4g"
def JENKINSFILE_REPO = 'kie-docs'
def JENKINSFILE_PATH = '.ci/jenkins/Jenkinsfile.upload'
def JENKINSFILE_URL = "https://github.com/${GH_ORG_UNIT}/${JENKINSFILE_REPO}"
def JENKINSFILE_PWD= 'kie-ci'
def SSH_KOGITO_DOCS_PATH="${sshKogitoDocsPath}:/docs_htdocs/kogito/release"
def LATEST_LINKS_PATH="${rsync_KogitoDocsPath}:/docs_htdocs/kogito/release"
def RSYNC_KOGITO_DOCS_PATH="${rsync_KogitoDocsPath}:/docs_htdocs/kogito/release"


// creation of folder
folder("KIE")
folder ("KIE/kogito")
folder ("KIE/kogito/kogito-docs")

def folderPath="KIE/kogito/kogito-docs"


pipelineJob("${folderPath}/uploadKogitoDocs") {

    description('''this job uploads the docs for a kogito version:
1. clones kie-docs - main-kogito  branch
2. creates a branch for a PR
3. upgrades the kie-docs/doc-content/kogito-docs/pom.xml to $currentKogitoDocsVersion
4. executes a mvn clean install in kie-docs/doc-content/kogito-docs
5. creates a folder $newKogitoDocsVersion on filemgmt-prod.jboss.org:docs_htdocs/kogito/release
6. uploads target/generated-docs/html_single to filemgmt-prod-sync.jboss.org:docs_htdocs/kogito/release
7. removes a symbolic link on filemgmt-prod-sync.jboss.org:docs_htdocs/kogito/release pointing to dir latest and upgrades the symbolic link pointing to the directory $currentKogitoDocsVersion/html_single) 
8. creates a tag $currentKogitoDocsTagName and pushes it to GitHub
9. bumps up kogito-docs to the $nextKogitoDocsSnapshot
10. pushes a PR to GitHub with all changes''')

    parameters {
        stringParam("currentKieSnapshot","${currentKieSnapshot}","""please enter the current kie snapshot version in poms of main-kogito branch. 
Look at https://github.com/kiegroup/kie-docs/blob/main-kogito/pom.xml OR 
https://github.com/kiegroup/kie-docs/blob/main-kogito/doc-content/pom.xml OR 
https://github.com/kiegroup/kie-docs/blob/main-kogito/doc-content/kogito-docs/pom.xml""")
        stringParam("nextKieSnapshot","${nextKieSnapshot}","please enter the next kie snapshot version to bump up to")
        stringParam("currentKogitoDocsSnaphot", "${currentKogitoDocsSnaphot}", """please enter the current kogito-docs snapshot version. 
Look at https://github.com/kiegroup/kie-docs/blob/main-kogito/doc-content/kogito-docs/pom.xml#L14""")
        stringParam("currentKogitoDocsVersion", "${currentKogitoDocsVersion}", """please enter the <b>current kogito-docs version. 
i.e. ${currentKogitoDocsSnaphot} without -SNAPSHOT """)
        stringParam("currentKogitoDocsTagName","${currentKogitoDocsTagName}","""please enter the name of the tag for this kogito release. 
i.e. ${currentKogitoDocsSnaphot} replace -SNAPSHOT with -kogito""")
        stringParam("nextKogitoDocsSnapshot","${nextKogitoDocsSnapshot}","please enter the next kogito-docs snapshot version to bump up to")
        wHideParameterDefinition {
            name('sshKogitoDocsPath')
            defaultValue("${sshKogitoDocsPath}")
            description('Please edit the path to filemgmt-prod.jboss.org')
        }
        wHideParameterDefinition {
            name('rsync_KogitoDocsPath')
            defaultValue("${rsync_KogitoDocsPath}")
            description('Please edit the path to filemgm-prod-sync.jboss.org')
        }
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
        wHideParameterDefinition {
            name('LATEST_LINKS_PATH')
            defaultValue("${LATEST_LINKS_PATH}")
            description('path on server for the latest links')
        }
        wHideParameterDefinition {
            name('SSH_KOGITO_DOCS_PATH')
            defaultValue("${SSH_KOGITO_DOCS_PATH}")
            description('path on server to kogito-docs')
        }
        wHideParameterDefinition {
            name('RSYNC_KOGITO_DOCS_PATH')
            defaultValue("${RSYNC_KOGITO_DOCS_PATH}")
            description('path on server to kogito-docs')
        }
    }

    logRotator {
        numToKeep(8)
    }

    definition {
        cpsScm {
            scm {
                gitSCM {
                    userRemoteConfigs {
                        userRemoteConfig {
                            url(JENKINSFILE_URL)
                            credentialsId(JENKINSFILE_PWD)
                            name('')
                            refspec('')
                        }
                    }
                    branches {
                        branchSpec {
                            name("*/${BASE_BRANCH}")
                        }
                    }
                    browser {}
                    doGenerateSubmoduleConfigurations(false)
                    gitTool('')
                }
            }
            scriptPath(JENKINSFILE_PATH)
        }
    }
}
