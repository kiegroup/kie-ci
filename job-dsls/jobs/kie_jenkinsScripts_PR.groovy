import org.kie.jenkins.jobdsl.Constants

// definition of parameters

def javaToolEnv="KIE_JDK1_8"
def kieMainBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def javadk=Constants.JDK_VERSION
def repo="kie-jenkins-scripts"
def ghAuthTokenId="kie-ci3-token"
def folderPath=Constants.PULL_REQUEST_FOLDER + "/"

// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

// create CI PR for kie-jenkins-scripts

def kieJenkins_PR='''#!/bin/bash -e
cd job-dsls
./gradlew clean test'''


// Creation of folders where jobs are stored
folder(Constants.PULL_REQUEST_FOLDER)

// jobs for master branch don't use the branch in the name
String jobName = (kieMainBranch == "master") ? folderPath + repo + "-pullrequests" : folderPath + repo + "-pullrequests-" + kieMainBranch

job(jobName) {

    description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |
                    |This job creates a PR for kie-jenkins-scripts
                    |
                    |""".stripMargin())


    logRotator {
        numToKeep(10)
    }

    label("kie-rhel7 && kie-mem4g")

    jdk("${javadk}")

    parameters {
        stringParam("sha1")
    }

    scm {
        git {
            remote {
                github("${organization}/${repo}")
                branch("\${sha1}")
                name("origin")
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

    properties {
        ownership {
            primaryOwnerId("mbiarnes")
            coOwnerIds("mbiarnes")
        }
    }

    triggers {
        githubPullRequest {
            orgWhitelist(["appformer", "kiegroup"])
            allowMembersOfWhitelistedOrgsAsAdmin()
            cron("H/7 * * * *")
            whiteListTargetBranches([kieMainBranch])
            extensions {
                commitStatus {
                    context('Linux')
                    addTestResults(true)
                }
            }
        }
    }

    wrappers {
        timestamps()
        timeout {
            elastic(30, 3, 60)
        }
        colorizeOutput()
        toolenv("${javaToolEnv}")
        preBuildCleanup()
    }

    publishers{
        archiveJunit('job-dsls/build/test-results/**/*.xml') {
            allowEmptyResults()
        }
        // Adds authentication token id for github.
        configure { node ->
            node / 'triggers' / 'org.jenkinsci.plugins.ghprb.GhprbTrigger' <<
                    'gitHubAuthId'(ghAuthTokenId)

        }
        wsCleanup()
    }

    steps {
        shell(kieJenkins_PR)
    }
}