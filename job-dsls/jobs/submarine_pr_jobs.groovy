/**
 * Creates pullrequest (PR) jobs for submarine units.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : Constants.GITHUB_ORG_UNIT,
        branch                 : Constants.BRANCH,
        timeoutMins            : 90,
        ghAuthTokenId          : "kie-ci2-token",
        label                  : "kie-rhel7 && kie-mem16g",
        upstreamMvnArgs        : "-B -e clean install -DskipTests",
        mvnGoals               : "-B -e -nsu -fae clean install",
        mvnProps               : [
                "maven.test.failure.ignore": "true"],
        artifactsToArchive     : [
                "**/target/*.log",
                "**/target/testStatusListener*"
        ],
        excludedArtifacts      : [
                "**/target/checkstyle.log"
        ]
]

// override default config for specific repos (if needed)
def final REPO_CONFIGS = [
        "submarine-runtimes"            : []
]


for (repoConfig in REPO_CONFIGS) {
        Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

        String repo = repoConfig.key
        String repoBranch = get("branch")
        String ghOrgUnit = get("ghOrgUnit")
        String ghAuthTokenId = get("ghAuthTokenId")

        // Creation of folders where jobs are stored
        folder(Constants.PULL_REQUEST_FOLDER)


        // jobs for master branch don't use the branch in the name
        String jobName = (repoBranch == "master") ? Constants.PULL_REQUEST_FOLDER + "/$repo-pullrequests" : Constants.PULL_REQUEST_FOLDER + "/$repo-pullrequests-$repoBranch"
        job(jobName) {

                description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |""".stripMargin())

                logRotator {
                        daysToKeep(7)
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

                jdk("kie-jdk1.8")

                label(get("label"))

                triggers {
                        githubPullRequest {
                                orgWhitelist(["appformer", "kiegroup"])
                                allowMembersOfWhitelistedOrgsAsAdmin()
                                cron("H/10 * * * *")
                                whiteListTargetBranches([repoBranch])
                                extensions {
                                        commitStatus {
                                                context('Linux')
                                                addTestResults(true)
                                        }
                                }
                        }
                }

                wrappers {
                        timeout {
                                elastic(250, 3, get("timeoutMins"))
                        }
                        timestamps()
                        colorizeOutput()
                }

                steps {
                        maven {
                                mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")
                                mavenOpts("-Xms1g -Xmx3g -XX:+CMSClassUnloadingEnabled")
                                goals(get("mvnGoals"))
                                properties(get("mvnProps"))

                        }
                }

                publishers {
                        if (repo == "submarine-runtimes") {
                                archiveJunit('**/target/*-reports/TEST-*.xml') {
                                        allowEmptyResults()
                                }
                        }

                        wsCleanup()
                        configure { project ->
                                project / 'publishers' << 'org.jenkinsci.plugins.emailext__template.ExtendedEmailTemplatePublisher' {
                                        'templateIds' {
                                                'org.jenkinsci.plugins.emailext__template.TemplateId' {
                                                        'templateId'('emailext-template-1441717935622')
                                                }
                                        }
                                }
                        }

                        // Adds authentication token id for github.
                        configure { node ->
                                node / 'triggers' / 'org.jenkinsci.plugins.ghprb.GhprbTrigger' <<
                                        'gitHubAuthId'(ghAuthTokenId)

                        }
                }
        }
}