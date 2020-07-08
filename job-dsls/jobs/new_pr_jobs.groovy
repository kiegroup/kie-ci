/**
 * Creates pullrequest (PR) jobs for kiegroup GitHub org. units.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : Constants.GITHUB_ORG_UNIT,
        branch                 : Constants.BRANCH,
        timeoutMins            : 120,
        ghAuthTokenId          : "kie-ci-token",
        ghJenkinsfilePwd       : "kie-ci",
        label                  : "kie-rhel7 && kie-mem8g",
        artifactsToArchive     : "",
        excludedArtifacts      : ""
]

// override default config for specific repos (if needed)
def final REPO_CONFIGS = [
        "lienzo-core"               : [
                timeoutMins: 30,
                label: "kie-rhel7 && kie-mem4g"
        ],
        "lienzo-tests"              : [
                timeoutMins: 30,
                label: "kie-rhel7 && kie-mem4g"
        ],
        "droolsjbpm-build-bootstrap": [
                timeoutMins: 30,
                label      : "kie-rhel7 && kie-mem4g"
        ],
        "kie-soup"                  : [
                label: "kie-rhel7 && kie-mem4g"
        ],
        "appformer"                 : [
                label    : "kie-rhel7 && kie-mem16g"
        ],
        "droolsjbpm-knowledge"      : [
                label: "kie-rhel7 && kie-mem4g"
        ],
        "drools"                    : [],
        "optaplanner"               : [],
        "optaweb-employee-rostering" : [
                artifactsToArchive: [
                        "**/cypress/screenshots/**",
                        "**/cypress/videos/**"
                ]
        ],
        "optaweb-vehicle-routing" : [
                artifactsToArchive: [
                        "**/cypress/screenshots/**",
                        "**/cypress/videos/**"
                ]
        ],
        "jbpm"                      : [
                timeoutMins: 150
        ],
        "kie-jpmml-integration"     : [
                label: "kie-rhel7 && kie-mem4g"
        ],
        "droolsjbpm-integration"    : [
                timeoutMins: 300,
                label: "kie-rhel7 && kie-mem24g",
                artifactsToArchive: [
                        "**/gclog" // this is a temporary file used to do some analysis: Once https://github.com/kiegroup/kie-jenkins-scripts/pull/652 is reverted this will disappear
                ]
        ],
        "openshift-drools-hacep"    : [],
        "droolsjbpm-tools"          : [],
        "kie-uberfire-extensions"   : [
                label: "kie-rhel7 && kie-mem4g"
        ],
        "kie-wb-playground"         : [
                label: "kie-rhel7 && kie-mem4g"
        ],
        "kie-wb-common"             : [
                timeoutMins: 300,
                label: "kie-rhel7 && kie-mem16g && gui-testing",
        ],
        "drools-wb"                 : [
                label: "kie-rhel7 && kie-mem16g"
        ],
        "optaplanner-wb"            : [],
        "jbpm-designer"             : [
                label: "kie-rhel7 && kie-mem16g"
        ],
        "jbpm-work-items"           : [
                label      : "kie-linux && kie-mem4g",
                timeoutMins: 40,
        ],
        "kie-docs"                  : [
                label      : "kie-linux && kie-mem4g",
                timeoutMins: 90,
                artifactsToArchive: [
                        "**/target/generated-docs/html_single/**/*"
                ]
        ],
        "jbpm-wb"                   : [
                label: "kie-rhel7 && kie-mem16g",
                artifactsToArchive: [
                        "**/target/jbpm-wb-case-mgmt-showcase*.war",
                        "**/target/jbpm-wb-showcase.war"
                ]
        ],
        "kie-wb-distributions"      : [
                label             : "kie-linux && kie-mem24g && gui-testing",
                timeoutMins       : 200,
                artifactsToArchive: [
                        "**/target/screenshots/**",
                        "**/target/business-monitoring-webapp.war",
                        "**/target/business-central*eap*.war",
                        "**/target/business-central*wildfly*.war",
                        "**/target/jbpm-server*dist*.zip"
                ]
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
    String additionalArtifacts = get("artifactsToArchive")
    additionalArtifacts = additionalArtifacts.replaceAll("[\\[\\]]", "")
    String addtionalExcludeArtifacts = get("excludedArtifacts")
    addtionalExcludeArtifacts = addtionalExcludeArtifacts.replaceAll("[\\[\\]]", "")
    String timeout = get("timeoutMins")
    String gitHubJenkinsfileRepUrl = "https://github.com/${ghOrgUnit}/droolsjbpm-build-bootstrap/"

    // Creation of folders where jobs are stored
    folder(Constants.PULL_REQUEST_FOLDER)


    // jobs for master branch don't use the branch in the name
    String jobName = Constants.PULL_REQUEST_FOLDER + "/new-PR-$repo-$repoBranch" + ".pullrequests"

    pipelineJob(jobName) {

        description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |""".stripMargin())

        logRotator {
            numToKeep(10)
        }

        properties {
            githubProjectUrl("https://github.com/${ghOrgUnit}/${repo}")
        }

        parameters {
            stringParam ("sha1","","this parameter will be provided by the PR")
            stringParam ("ADDITIONAL_LABEL","${additionalLabel}","this parameter is provided by the job")
            stringParam ("ADDITIONAL_ARTIFACTS_TO_ARCHIVE","${additionalArtifacts}","this parameter is provided by the job")
            stringParam ("ADDITIONAL_EXCLUDED_ARTIFACTS","${addtionalExcludeArtifacts}","this parameter is provided by the job")
            stringParam ("ADDITIONAL_TIMEOUT","${timeout}","this parameter is provided by the job")
            stringParam ("CHECKSTYLE_FILE","**/checkstyle-result.xml","")
            stringParam ("FINDBUGS_FILE","**/spotbugsXml.xml","")
            stringParam ("PR_TYPE","Pull Request","")
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
                scriptPath("Jenkinsfile")
            }
        }

        triggers {
            ghprbTrigger {
                onlyTriggerPhrase(false)
                gitHubAuthId("${ghAuthTokenId}")
                adminlist("")
                orgslist("${ghOrgUnit}")
                whitelist("")
                cron("")
                triggerPhrase(".*[j|J]enkins,?.*(retest|test) this please.*")
                allowMembersOfWhitelistedOrgsAsAdmin(true)
                whiteListTargetBranches {
                    ghprbBranch {
                        branch("${repoBranch}")
                    }
                    ghprbBranch {
                        branch("7.x")
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
                        commitStatusContext("Linux - Pull Request (***)")
                        addTestResults(true)
                        showMatrixStatus(false)
                        statusUrl("")
                        triggeredStatus("")
                        startedStatus("")
                    }
                }
                includedRegions("")
                excludedRegions("")
            }
        }
    }
}
