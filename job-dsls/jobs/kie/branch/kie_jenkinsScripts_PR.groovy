import org.kie.jenkins.jobdsl.Constants

// definition of parameters

def mvnToolEnv=Constants.MAVEN_TOOL
def jdkToolEnv=Constants.JDK_TOOL
def repoBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def javadk=Constants.JDK_TOOL
def repo="kie-ci"
def ghAuthTokenId="kie-ci-token"
def labelName="kie-rhel7 && kie-mem4g && !built-in"

// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

// create CI PR for kie-ci

def kieJenkins_PR='''#!/bin/bash -e
cd job-dsls
./gradlew clean test'''

def errorSh='''#!/bin/bash -e
cd $WORKSPACE
touch trace.sh
chmod 755 trace.sh
echo "wget  --no-check-certificate ${BUILD_URL}consoleText" >> trace.sh
echo "tail -n 750 consoleText >> error.log" >> trace.sh
echo "gzip error.log" >> trace.sh
cat trace.sh'''

// Creation of folders where jobs are stored
folder("KIE")
folder("KIE/${repoBranch}")
folder("KIE/${repoBranch}/" + Constants.PULL_REQUEST_FOLDER){
    displayName(Constants.PULL_REQUEST_FOLDER_DISPLAY_NAME)
}
def folderPath = ("KIE/${repoBranch}/" + Constants.PULL_REQUEST_FOLDER)


// jobs for main branch don't use the branch in the name
String jobName = "${folderPath}/${repo}-${repoBranch}.pr"

job(jobName) {

    description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |
                    |This job creates a PR for kie-ci
                    |
                    |""".stripMargin())


    logRotator {
        numToKeep(5)
    }

    label(labelName)

    jdk(javadk)

    parameters {
        stringParam("sha1")
    }

    scm {
        git {
            remote {
                github("${organization}/${repo}")
                branch("\${sha1}")
                name("origin")
                credentials("kie-ci-user-key")
                refspec("+refs/pull/*:refs/remotes/origin/pr/*")
            }
            extensions {
                cloneOptions {
                    reference("/home/jenkins/git-repos/${repo}.git")
                }
            }
        }
    }

    concurrentBuild()

    triggers {
        githubPullRequest {
            useGitHubHooks()
            triggerPhrase(".*[j|J]enkins,?.*(retest|test).*")
            cron("")
            orgWhitelist(["kiegroup"])
            allowMembersOfWhitelistedOrgsAsAdmin()
            whiteListTargetBranches([repoBranch])
            extensions {
                commitStatus {
                    context('Pull Request Execution')
                    addTestResults(true)
                }
            }
        }
    }

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${jdkToolEnv}","${mvnToolEnv}")
        preBuildCleanup()
    }

    publishers{
        archiveJunit('job-dsls/build/test-results/**/*.xml') {
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
                    attachmentPatterns('error.log.gz')
                    subject('PR build: #$ghprbPullId $ghprbPullTitle')
                    content('The Pull Request: $ghprbGhRepository #$ghprbPullId $ghprbPullTitle FAILED\n' +
                            'Build log: ${BUILD_URL}consoleText\n' +
                            'Failed tests (${TEST_COUNTS,var="fail"}): ${BUILD_URL}testReport\n' +
                            '(IMPORTANT: For visiting the links you need to have access to Red Hat VPN. In case you don\'t have access to RedHat VPN please download and decompress attached file.)')
                    sendTo {
                        recipientList()
                    }
                }
                unstable {
                    attachmentPatterns('error.log.gz')
                    subject('PR build: #$ghprbPullId $ghprbPullTitle')
                    content('The Pull Request: $ghprbGhRepository #$ghprbPullId $ghprbPullTitle was UNSTABLE\n' +
                            'Build log: ${BUILD_URL}consoleText\n' +
                            'Failed tests (${TEST_COUNTS,var="fail"}): ${BUILD_URL}testReport\n' +
                            '(IMPORTANT: For visiting the links you need to have access to Red Hat VPN. In case you don\'t have access to RedHat VPN please download and decompress attached file.)\n' +
                            '***********************************************************************************************************************************************************\n' +
                            '${FAILED_TESTS}')
                    sendTo {
                        recipientList()
                    }
                }
                fixed {
                    subject('PR build: #$ghprbPullId $ghprbPullTitle')
                    content('The Pull Request: $ghprbGhRepository #$ghprbPullId $ghprbPullTitle is fixed and was SUCCESSFUL')
                    sendTo {
                        recipientList()
                    }
                }
            }
        }
        wsCleanup()
    }

    steps {
        shell(errorSh)
        shell(kieJenkins_PR)
    }
}