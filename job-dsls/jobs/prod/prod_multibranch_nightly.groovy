/**
 * Creates jobs for multibranch-pipelines
 * - nightly
 * - kogito.nightly
 * - kogito-tooling.nightly (will be dropped soon)
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        prodJobName : 'rhba.nightly',
        scrPath : '.ci/jenkins/Jenkinsfile.nightly',
        repo : 'droolsjbpm-build-bootstrap',
        repUrl : 'https://github.com/kiegroup/droolsjbpm-build-bootstrap',
        organization : 'kiegroup',
        credId : 'kie-ci',
        jobId : '00121',
        libName : 'jenkins-pipeline-shared-libraries',
        libBranch : 'main',
        remoteName : 'https://github.com/kiegroup/jenkins-pipeline-shared-libraries.git'
        ]

def final REPO_CONFIGS = [
        "nightly"  : [],
        "kogito.nightly"  : [
            prodJobName : 'kogito.nightly',
            repo : 'kogito-pipelines',
            repUrl : 'https://github.com/kiegroup/kogito-pipelines',
            scrPath : '.ci/jenkins/Jenkinsfile.prod.nightly',
            jobId : '00242'
        ],
        "rhbop.nightly" : [
           prodJobName : 'rhbop.nightly',
           scrPath : '.ci/jenkins/Jenkinsfile.prod.nightly',
           repo : 'optaplanner',
           repUrl : 'https://github.com/kiegroup/optaplanner',
           jobId : '00484'
        ],
        "drools-ansible-integration.nightly" : [
           prodJobName : 'drools-ansible-integration.nightly',
           scrPath : '.ci/jenkins/Jenkinsfile.prod.nightly',
           repo : 'drools-ansible-rulebook-integration',
           repUrl : 'https://github.com/kiegroup/drools-ansible-rulebook-integration',
           jobId : '00968'
        ],
]

for (repoConfig in REPO_CONFIGS) {
    Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

    String prodJobName = get('prodJobName')
    String repo = get('repo')
    String scrPath = get('scrPath')
    String organization = get('organization')
    String repUrl = get('repUrl')
    String credId = get('credId')
    String jobId = get ('jobId')
    String libName = get ('libName')
    String remoteName = get ('remoteName')
    String libBranch = get ('libBranch')

    // create needed folder(s) for where the jobs are created
    folder("PROD")
    def folderPath = "PROD"

    String jobName = "${folderPath}/${prodJobName}"

    multibranchPipelineJob("${jobName}") {

        description('nightly build for RHBA')

        branchSources {
            branchSource {
                source {
                    github {
                        id("${jobId}")
                        credentialsId("${credId}")
                        repoOwner("${organization}")
                        repository("${repo}")
                        repositoryUrl("${repUrl}")
                        configuredByUrl(false)
                        traits {
                            gitHubBranchDiscovery {
                                strategyId(1)
                            }
                            gitHubPullRequestDiscovery {
                                strategyId(1)
                            }
                            cloneOptionTrait {
                                extension {
                                    noTags(false)
                                    shallow(true)
                                    depth(1)
                                    reference('')
                                    timeout(20)
                                }
                            }
                        }
                    }
                }
                strategy {
                    namedBranchesDifferent {
                        defaultProperties {
                            suppressAutomaticTriggering()
                        }
                    }
                }
            }
        }

        configure {
            def traits = it / 'sources' / 'data' / 'jenkins.branch.BranchSource' / 'source' / 'traits'
            traits << 'org.jenkinsci.plugins.github__branch__source.ForkPullRequestDiscoveryTrait' {
                strategyId(1)
                //trust(class: 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait$TrustPermission')
                // trust(class: 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait$TrustNobody')
                trust(class: 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait$TrustContributors')
                // trust(class: 'org.jenkinsci.plugins.github_branch_source.ForkPullRequestDiscoveryTrait$TrustEveryone')
            }
        }

        factory {
            workflowBranchProjectFactory {
                // Relative location within the checkout of your Pipeline script.
                scriptPath("${scrPath}")
            }
        }

        orphanedItemStrategy {
            discardOldItems {
                daysToKeep(20)
                numToKeep(7)
            }
        }

        triggers {
            periodicFolderTrigger {
                interval('1d')
            }
        }

        properties {
            folderLibraries {
                libraries {
                    libraryConfiguration {
                        name("${libName}")
                        defaultVersion("${libBranch}")
                        allowVersionOverride(true)
                        includeInChangesets(true)
                        retriever {
                            modernSCM {
                                scm {
                                    git {
                                        remote("${remoteName}")
                                        traits {
                                            gitBranchDiscovery()
                                        }
                                        credentialsId(Constants.CREDENTIALS_ID)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
