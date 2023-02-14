/**
 * Creates pullrequest (PR) jobs for kiegroup GitHub org. units.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : Constants.GITHUB_ORG_UNIT,
        branch                 : "7.x",
        timeoutMins            : 120,
        ghAuthTokenId          : "kie-ci-token",
        ghJenkinsfilePwd       : "kie-ci",
        label                  : "kie-rhel7 && kie-mem8g && !built-in",
        executionNumber        : 10,
        numDaysKeep            : 20,
        artifactsToArchive     : '',
        excludedArtifacts      : '',
        checkstyleFile         : Constants.CHECKSTYLE_FILE,
        findbugsFile           : Constants.FINDBUGS_FILE,
        buildJDKTool           : '',
        buildMavenTool         : '',
        excludedRegions        : [],
        buildChainGroup        : 'kiegroup',
        buildChainBranch       : 'main'
]

// override default config for specific repos (if needed)

def final REPO_CONFIGS = [
        "drools"                    : [
                timeoutMins: 150,
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt']
        ],
        "droolsjbpm-knowledge"      : [
                label: "kie-rhel7 && kie-mem4g && !built-in",
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt']
        ],
        "optaplanner"               : [
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt', 'build/.*', 'ide-configuration/.*']
        ]
]

for (repoConfig in REPO_CONFIGS) {
    Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

    String repo = repoConfig.key
    String repoBranch = get("branch")
    String ghOrgUnit = get("ghOrgUnit")
    String ghAuthTokenId = get("ghAuthTokenId")
    String ghJenkinsfilePwd = get("ghJenkinsfilePwd")
    String additionalLabel = get("label")
    def exeNum = get("executionNumber")
    int buildsDaysToKeep = get('numDaysKeep')
    String additionalArtifacts = get("artifactsToArchive")
    additionalArtifacts = additionalArtifacts.replaceAll("[\\[\\]]", '')
    String addtionalExcludeArtifacts = get("excludedArtifacts")
    addtionalExcludeArtifacts = addtionalExcludeArtifacts.replaceAll("[\\[\\]]", '')
    String timeout = get("timeoutMins")
    String gitHubJenkinsfileRepUrl = "https://github.com/${ghOrgUnit}/droolsjbpm-build-bootstrap/"
    String findbugsFile = get("findbugsFile")
    String checkstyleFile = get("checkstyleFile")
    String buildJDKTool = get("buildJDKTool")
    String buildMavenTool = get("buildMavenTool")
    String excludedRegionsValue = get('excludedRegions')
    String buildChainGroup = get('buildChainGroup')
    String buildChainBranch = get('buildChainBranch')

    // Creation of folders where jobs are stored
    folder("KIE")
    folder("KIE/${repoBranch}")
    folder("KIE/${repoBranch}/" + Constants.PULL_REQUEST_FOLDER){
        displayName(Constants.PULL_REQUEST_FOLDER_DISPLAY_NAME)
    }
    def folderPath = ("KIE/${repoBranch}/" + Constants.PULL_REQUEST_FOLDER)


    // jobs for main branch don't use the branch in the name
    String jobName = "${folderPath}/${repo}-${repoBranch}.pr"

    pipelineJob(jobName) {

        description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |""".stripMargin())

        logRotator {
            numToKeep(exeNum)
            daysToKeep(buildsDaysToKeep)
        }

        properties {
            githubProjectUrl("https://github.com/${ghOrgUnit}/${repo}")
        }

        parameters {
            stringParam ("sha1",'',"this parameter will be provided by the PR")
            stringParam ("ADDITIONAL_LABEL","${additionalLabel}","this parameter is provided by the job")
            stringParam ("ADDITIONAL_ARTIFACTS_TO_ARCHIVE","${additionalArtifacts}","this parameter is provided by the job")
            stringParam ("ADDITIONAL_EXCLUDED_ARTIFACTS","${addtionalExcludeArtifacts}","this parameter is provided by the job")
            stringParam ("ADDITIONAL_TIMEOUT","${timeout}","this parameter is provided by the job")
            stringParam ("CHECKSTYLE_FILE","${checkstyleFile}",'')
            stringParam ("FINDBUGS_FILE","${findbugsFile}",'')
            stringParam ("PR_TYPE","Pull Request",'')
            stringParam ("BUILD_JDK_TOOL","${buildJDKTool}",'')
            stringParam ("BUILD_MAVEN_TOOL","${buildMavenTool}",'')
            stringParam ("BUILDCHAIN_GROUP","${buildChainGroup}",'')
            stringParam ("BUILDCHAIN_BRANCH","${buildChainBranch}",'')
        }

        definition {
            cpsScm {
                scm {
                    gitSCM {
                        userRemoteConfigs {
                            userRemoteConfig {
                                url("${gitHubJenkinsfileRepUrl}")
                                credentialsId("${ghJenkinsfilePwd}")
                                name('')
                                refspec('')
                            }
                        }
                        branches {
                            branchSpec {
                                name("*/main")
                            }
                        }
                        browser { }
                        doGenerateSubmoduleConfigurations(false)
                        gitTool('')
                    }
                }
                scriptPath(".ci/jenkins/Jenkinsfile.buildchain")
            }
        }

        properties {
            pipelineTriggers {
                triggers {
                    ghprbTrigger {
                        onlyTriggerPhrase(false)
                        gitHubAuthId("${ghAuthTokenId}")
                        adminlist('')
                        orgslist("${ghOrgUnit}")
                        whitelist('')
                        cron('')
                        triggerPhrase(".*[j|J]enkins,?.*(retest|test).*?.*(this).*")
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
                        blackListCommitAuthor('')
                        commentFilePath('')
                        skipBuildPhrase('')
                        msgSuccess("Success")
                        msgFailure("Failure")
                        commitStatusContext('')
                        buildDescTemplate('')
                        blackListLabels('')
                        whiteListLabels('')
                        extensions {
                            ghprbSimpleStatus {
                                commitStatusContext("Linux - Pull Request")
                                addTestResults(true)
                                showMatrixStatus(false)
                                statusUrl('')
                                triggeredStatus('')
                                startedStatus('')
                            }
                            ghprbCancelBuildsOnUpdate {
                                overrideGlobal(true)
                            }
                        }
                        includedRegions('')
                        excludedRegions("${excludedRegionsValue.join('\n')}")
                    }
                }
            }
        }
    }
}
