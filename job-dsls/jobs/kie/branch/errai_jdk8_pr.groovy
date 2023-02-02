/**
 * Creates pullrequest (PR) jobs for errai
 */
import org.kie.jenkins.jobdsl.Constants

//creation of script for log compression
def errorSh='''#!/bin/bash -e
cd $WORKSPACE
touch trace.sh
chmod 755 trace.sh
echo "wget  --no-check-certificate  ${BUILD_URL}consoleText" >> trace.sh
echo "tail -n 1000 consoleText >> error.log" >> trace.sh
echo "gzip error.log" >> trace.sh'''

def repo = "errai"
def repoBranch = Constants.BRANCH
def ghOrgUnit = "errai"
def ghAuthTokenId = "kie-ci-token"
def javadk="kie-jdk1.8"
def mvnToolEnv=Constants.MAVEN_TOOL
def mvnGoals = "-B -e -fae -Dmaven.test.failure.ignore=true -Pintegration-test clean install -Derrai.codegen.details=true -Dapt-generators"
def labelName = "kie-rhel7 && kie-mem16g"


// Creation of folders where jobs are stored
folder("KIE")
folder("KIE/${repoBranch}")
folder("KIE/${repoBranch}/" + Constants.PULL_REQUEST_FOLDER){
    displayName(Constants.PULL_REQUEST_FOLDER_DISPLAY_NAME)
}
def folderPath = ("KIE/${repoBranch}/" + Constants.PULL_REQUEST_FOLDER)

// jobs for main branch don't use the branch in the name
String jobName = "${folderPath}/${repo}-${repoBranch}-jdk8.pr"

job(jobName) {

    description("Runs CI build against PRs submitted to the Errai repository (github.com/errai/errai.git) with jdk1.8")

    logRotator {
        numToKeep(10)
        daysToKeep(10)
    }

    parameters {
        stringParam("sha1")
    }

    scm {
        git {
            remote {
                github("${ghOrgUnit}/${repo}")
                branch("\${sha1}")
                name("origin")
                refspec("+refs/pull/*:refs/remotes/origin/pr/*")
            }
            extensions {
                cloneOptions {
                    reference("/home/jenkins/git-repos/${repo}.git")
                }
                relativeTargetDirectory("${repo}")
            }
        }
    }
    concurrentBuild()

    jdk(javadk)

    label(labelName)

    // creates script for building error.log.gz
    steps {
        shell(errorSh)
    }

    triggers {
        githubPullRequest {
            useGitHubHooks(true)
            onlyTriggerPhrase(false)
            triggerPhrase(".*[j|J]enkins,?.*run jdk8.*")
            cron("")
            orgWhitelist(["errai", "kiegroup"])
            allowMembersOfWhitelistedOrgsAsAdmin(true)
            whiteListTargetBranches([repoBranch])
            extensions {
                commitStatus {
                    context('Linux - Pull Request - jdk8')
                    addTestResults(true)
                }
            }
        }
    }

    wrappers {
        timeout {
            elastic(150, 3, 90)
        }
        timestamps()
        colorizeOutput()

        configFiles {
            mavenSettings("settings-local-maven-repo-nexus"){
                variable("SETTINGS_XML_FILE")
                targetLocation("jenkins-settings.xml")
            }
        }

        preBuildCleanup()
    }

    steps {
        maven {
            mavenInstallation("${mvnToolEnv}")
            mavenOpts("-Xms1g -Xmx4g -XX:+CMSClassUnloadingEnabled")
            goals(mvnGoals)
            providedSettings("settings-local-maven-repo-nexus")
            rootPOM("${repo}")
        }
    }
    publishers {

        archiveJunit('**/target/*-reports/TEST-*.xml') {
            allowEmptyResults()
        }


        // adds POST BUILD scripts
        configure { project ->
            project / 'publishers' << 'org.jenkinsci.plugins.postbuildscript.PostBuildScript' {
                'config' {
                    'scriptFiles' {
                        'org.jenkinsci.plugins.postbuildscript.model.ScriptFile '{
                            'results' {
                                'string'('SUCCESS')
                                'string'('FAILURE')
                                'string'('UNSTABLE')
                            }
                            'filePath'('trace.sh')
                            'scriptType'('GENERIC')
                        }
                    }
                    'groovyScripts'()
                    'buildSteps'()
                    'executeOn'('BOTH')
                    'markBuildUnstable'(false)
                    'sandboxed'(true)
                }
            }
        }

        // Adds authentication token id for github.
        configure { node ->
            node / 'triggers' / 'org.jenkinsci.plugins.ghprb.GhprbTrigger' <<
                    'gitHubAuthId'(ghAuthTokenId)

        }

        extendedEmail{
            recipientList('$ghprbActualCommitAuthorEmail')
            defaultSubject('$DEFAULT_SUBJECT')
            defaultContent('$DEFAULT_CONTENT')
            contentType('default')
            triggers {
                failure {
                    subject('Pull request #$ghprbPullId of $ghprbGhRepository: $ghprbPullTitle failed')
                    content('Pull request #$ghprbPullId of $ghprbGhRepository: $ghprbPullTitle  FAILED\n' +
                            'Build log: ${BUILD_URL}consoleText\n' +
                            'Failed tests (${TEST_COUNTS,var="fail"}): ${BUILD_URL}testReport\n' +
                            '(IMPORTANT: For visiting the links you need to have access to Red Hat VPN. In case you don\'t have access to RedHat VPN please download and decompress attached file.)')
                    attachmentPatterns('error.log.gz')
                    sendTo {
                        recipientList()
                    }
                }
                unstable {
                    subject('Pull request #$ghprbPullId of $ghprbGhRepository: $ghprbPullTitle was unstable')
                    content('Pull request #$ghprbPullId of $ghprbGhRepository: $ghprbPullTitle was UNSTABLE\n' +
                            'Build log: ${BUILD_URL}consoleText\n' +
                            'Failed tests (${TEST_COUNTS,var="fail"}): ${BUILD_URL}testReport\n' +
                            '***********************************************************************************************************************************************************\n' +
                            '${FAILED_TESTS}')
                    sendTo {
                        recipientList()
                    }
                }
                fixed {
                    subject('Pull request #$ghprbPullId of $ghprbGhRepository: $ghprbPullTitle is fixed and was SUCCESSFUL')
                    content('')
                    sendTo {
                        recipientList()
                    }
                }
            }
        }
        wsCleanup()
    }
}
