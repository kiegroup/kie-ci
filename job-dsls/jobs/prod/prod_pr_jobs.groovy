import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : "jboss-integration",
        branch                 : "master",
        ghAuthTokenId          : "kie-ci-token",
        ghJenkinsfilePwd       : "kie-ci",
        ghJenkinsfileSrc       : "rhba-installers",
        label                  : "kie-rhel7 && !master"
]

// override default config for specific repos (if needed)
def final REPO_CONFIGS = [
        "izpack" : [
                "branch" : "bxms-7.0"
        ],
        "installer-commons" : [:],
        "rhba-installers" : [:],
        "bxms-patch-tools" : [
                "ghJenkinsfileSrc" : "bxms-patch-tools"
        ]
]

// Creation of folders where jobs are stored
def folderPath = ("PROD/" + Constants.PULL_REQUEST_FOLDER)
folder(folderPath)

for (repoConfig in REPO_CONFIGS) {
    Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

    String repo = repoConfig.key
    String repoBranch = get("branch")
    String ghOrgUnit = get("ghOrgUnit")
    String ghAuthTokenId = get("ghAuthTokenId")
    String ghJenkinsfilePwd = get("ghJenkinsfilePwd")
    String ghJenkinsfileSrc = get("ghJenkinsfileSrc")
    String gitHubJenkinsfileRepUrl = "https://github.com/${ghOrgUnit}/${ghJenkinsfileSrc}"
    String jobName = "${folderPath}/${repo}-${repoBranch}.pr"

    pipelineJob(jobName) {

        description("Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.\n" +
                    "Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.")

        logRotator {
            numToKeep(10)
        }

        properties {
            githubProjectUrl("https://github.com/${ghOrgUnit}/${repo}")
        }

        parameters {
            stringParam ("NIGHTLY_NEXUS_REPOSITORY",
                    "http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/groups/rhba-master-nightly",
                    "Maven repository where the nightly artifacts are fetched from.")
            stringParam ("NIGHTLY_STAGING_PATH",
                    "http://rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging",
                    "Root staging server folder where nightly builds are staged.")
        }

        definition {
            cpsScm {
                scm {
                    gitSCM {
                        userRemoteConfigs {
                            userRemoteConfig {
                                url("${gitHubJenkinsfileRepUrl}")
                                credentialsId("${ghJenkinsfilePwd}")
                                name("")
                                refspec("")
                            }
                        }
                        branches {
                            branchSpec {
                                name("*/${repoBranch}")
                            }
                        }
                        browser { }
                        doGenerateSubmoduleConfigurations(false)
                        gitTool("")
                    }
                }
                scriptPath(".ci/jenkins/Jenkinsfile")
            }
        }

        properties {
            pipelineTriggers {
                triggers {
                    ghprbTrigger {
                        onlyTriggerPhrase(false)
                        gitHubAuthId("${ghAuthTokenId}")
                        adminlist("")
                        orgslist("${ghOrgUnit}")
                        whitelist("")
                        cron("")
                        triggerPhrase(".*[j|J]enkins,?.*(retest|test).*")
                        allowMembersOfWhitelistedOrgsAsAdmin(true)
                        whiteListTargetBranches {
                            ghprbBranch {
                                branch("${repoBranch}")
                            }
                        }
                        useGitHubHooks(true)
                        permitAll(false)
                        autoCloseFailedPullRequests(false)
                        displayBuildErrorsOnDownstreamBuilds(false)
                        blackListCommitAuthor("")
                        commentFilePath("")
                        skipBuildPhrase("")
                        msgSuccess("Success")
                        msgFailure("Failure")
                        commitStatusContext("")
                        buildDescTemplate("")
                        blackListLabels("")
                        whiteListLabels("")
                        extensions {
                            ghprbSimpleStatus {
                                commitStatusContext("Linux - Pull Request")
                                addTestResults(true)
                                showMatrixStatus(false)
                                statusUrl("")
                                triggeredStatus("")
                                startedStatus("")
                            }
                            ghprbCancelBuildsOnUpdate {
                                overrideGlobal(true)
                            }
                        }
                        includedRegions("")
                        excludedRegions("")
                    }
                }
            }
        }
    }
}
