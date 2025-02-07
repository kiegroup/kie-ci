/**
 * Creates pullrequest (PR) jobs for kiegroup GitHub org. units.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : Constants.GITHUB_ORG_UNIT,
        branch                 : Constants.BRANCH,
        timeoutMins            : 120,
        ghAuthTokenId          : 'kie-ci-token',
        ghJenkinsfilePwd       : 'kie-ci',
        label                  : 'kie-rhel8 && kie-mem8g && !built-in',
        executionNumber        : 10,
        numDaysKeep            : 20,
        artifactsToArchive     : '',
        excludedArtifacts      : '',
        checkstyleFile         : Constants.CHECKSTYLE_FILE,
        findbugsFile           : Constants.FINDBUGS_FILE,
        buildJDKTool           : '',
        buildMavenTool         : '',
        excludedRegions        : [],
        gitHubJenkinsfileRepository : 'droolsjbpm-build-bootstrap'
]

// override default config for specific repos (if needed)
def final REPO_CONFIGS = [
        'lienzo-core'               : [
                timeoutMins: 30,
                label: 'kie-rhel8 && kie-mem4g && !built-in',
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt']
        ],
        'lienzo-tests'              : [
                timeoutMins: 30,
                label: 'kie-rhel8 && kie-mem4g && !built-in',
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt']
        ],
        'droolsjbpm-build-bootstrap': [
                timeoutMins: 30,
                label      : 'kie-rhel8 && kie-mem4g && !built-in',
                executionNumber : 25,
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt', 'docs/.*', 'ide-configuration/.*', 'script/.*']
        ],
        'kie-soup'                  : [
                label: 'kie-rhel8 && kie-mem4g && !built-in',
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt', 'scripts/.*']
        ],
        "drools"                    : [
                timeoutMins: 150,
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt']
        ],
        "droolsjbpm-knowledge"      : [
                label: "kie-rhel8 && kie-mem4g && !built-in",
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt']
        ],
        'appformer'                 : [
                label    : 'kie-rhel8 && kie-mem16g && !built-in',
                artifactsToArchive: [
                        '**/dashbuilder-runtime.war'
                ],
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt']
        ],
        "jbpm"                      : [
                timeoutMins: 150,
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt', 'docsimg/.*']
        ],
        "kie-jpmml-integration"     : [
                label: "kie-rhel8 && kie-mem4g && !built-in",
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt']
        ],
        "droolsjbpm-integration"    : [
                timeoutMins: 300,
                label: "kie-rhel8 && kie-mem24g && !built-in",
                artifactsToArchive: [
                        "**/gclog" // this is a temporary file used to do some analysis: Once https://github.com/kiegroup/kie-ci/pull/652 is reverted this will disappear
                ],
                executionNumber : 25,
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt']
        ],
        "openshift-drools-hacep"    : [
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt', 'docs/.*']
        ],
        "kie-uberfire-extensions"   : [
                label: "kie-rhel8 && kie-mem4g && !built-in",,
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt']
        ],
        "kie-wb-playground"         : [
                label: "kie-rhel8 && kie-mem4g && !built-in",,
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt']
        ],
        "kie-wb-common"             : [
                timeoutMins: 300,
                label: "kie-rhel8 && kie-mem16g && gui-testing && !built-in",
                executionNumber : 25,
                artifactsToArchive: [
                        "**/target/screenshots/**"
                ],
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt']
        ],
        "drools-wb"                 : [
                label: "kie-rhel8 && kie-mem16g && !built-in",
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt']
        ],
        "jbpm-work-items"           : [
                label      : "kie-rhel8 && kie-mem4g && !built-in",
                timeoutMins: 120,
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt', '\\.idea/.*']
        ],
        "jbpm-wb"                   : [
                label: "kie-rhel8 && kie-mem16g && !built-in",
                artifactsToArchive: [
                        "**/target/jbpm-wb-case-mgmt-showcase*.war",
                        "**/target/jbpm-wb-showcase.war"
                ],
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt']
        ],
        "kie-wb-distributions"      : [
                label             : "kie-rhel8 && kie-mem24g && gui-testing && !built-in",
                timeoutMins       : 200,
                artifactsToArchive: [
                        "**/target/screenshots/**",
                        "**/target/business-monitoring-webapp.war",
                        "**/target/business-central*eap*.war",
                        "**/target/business-central*wildfly*.war",
                        "**/target/jbpm-server*dist*.zip"
                ],
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt']
        ],
        "kogito-rhba"             : [
                label: 'kie-rhel8 && kie-mem16g && !built-in',
                ghOrgUnit : 'jboss-integration',
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt'],
                gitHubJenkinsfileRepository : 'kogito-rhba'
        ],
        "process-migration-service" : [
                label : 'kie-rhel8 && kie-mem16g && !built-in',
                excludedRegions: ['LICENSE.*', '\\.gitignore', '.*\\.md', '.*\\.adoc', '.*\\.txt'],
                buildJDKTool: 'kie-jdk11'
        ]
]

for (repoConfig in REPO_CONFIGS) {
    Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

    String repo = repoConfig.key
    String repoBranch = get('branch')
    String ghOrgUnit = get('ghOrgUnit')
    String ghAuthTokenId = get('ghAuthTokenId')
    String ghJenkinsfilePwd = get('ghJenkinsfilePwd')
    String additionalLabel = get('label')
    def exeNum = get('executionNumber')
    int buildsDaysToKeep = get('numDaysKeep')
    String additionalArtifacts = get('artifactsToArchive')
    additionalArtifacts = additionalArtifacts.replaceAll('[\\[\\]]', '')
    String addtionalExcludeArtifacts = get('excludedArtifacts')
    addtionalExcludeArtifacts = addtionalExcludeArtifacts.replaceAll('[\\[\\]]', '')
    String timeout = get('timeoutMins')
    String gitHubJenkinsfileRepUrl = "https://github.com/${ghOrgUnit}/${get('gitHubJenkinsfileRepository')}/"
    String findbugsFile = get('findbugsFile')
    String checkstyleFile = get('checkstyleFile')
    String buildJDKTool = get('buildJDKTool')
    String buildMavenTool = get('buildMavenTool')
    String excludedRegionsValue = get('excludedRegions')

    // Creation of folders where jobs are stored
    folder('KIE')
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
                                name("*/${repoBranch}")
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
