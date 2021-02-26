/**
 * Creates all the standard "deploy" jobs for appformer (formerly known as uberfire) and kiegroup GitHub org. units.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : Constants.GITHUB_ORG_UNIT,
        branch                 : "7.x",
        ghAuthKey              : "kie-ci-user-key",
        timeoutMins            : 90,
        label                  : "kie-rhel7 && kie-mem8g",
        mvnGoals               : "-e -fae -B -Pwildfly clean deploy com.github.spotbugs:spotbugs-maven-plugin:spotbugs",
        mvnProps: [
                "full"                     : "true",
                "container"                : "wildfly",
                "integration-tests"        : "true",
                "maven.test.failure.ignore": "true"
        ],
        zulipNotificationStream: "kie-ci.5ef0dba1f620d6457ba4c5976533977d.show-sender@streams.zulipchat.com",
        artifactsToArchive     : [
                "**/target/testStatusListener*",
                "**/target/*.log"
        ],
        excludedArtifacts      : [
                "**/target/checkstyle.log"
        ],
        downstreamRepos        : []
]

// used to override default config for specific repos (if needed)
def final REPO_CONFIGS = [
        "optaplanner"               : [
                downstreamRepos        : ["optaplanner-wb"],
                mvnGoals: "-e -fae -B clean deploy com.github.spotbugs:spotbugs-maven-plugin:spotbugs",
                mvnProps: [
                        "full"                     : "true",
                        "integration-tests"        : "true",
                        "maven.test.failure.ignore": "true"
                ]
        ],
        "optaweb-employee-rostering" : [
                artifactsToArchive     : DEFAULTS["artifactsToArchive"] + [
                        "**/target/configurations/cargo-profile/profile-log.txt"
                ],
                downstreamRepos        : ["optaweb-vehicle-routing-7.x"]
        ],
        "optaweb-vehicle-routing" : [
                artifactsToArchive     : DEFAULTS["artifactsToArchive"] + [
                        "**/target/configurations/cargo-profile/profile-log.txt"
                ],
                downstreamRepos        : ["kie-wb-distributions"]
        ]
]

for (repoConfig in REPO_CONFIGS) {
    Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

    String repo = repoConfig.key
    String repoBranch = get("branch")
    String ghOrgUnit = get("ghOrgUnit")
    String ghAuthKey = get ("ghAuthKey")
    String zulipStream = get("zulipNotificationStream")

    // Creation of folders where jobs are stored
    folder("KIE")
    folder("KIE/master")
    folder("KIE/master/" + Constants.DEPLOY_FOLDER)

    def folderPath = ("KIE/master/" + Constants.DEPLOY_FOLDER)

    // jobs for master branch don't use the branch in the name
    String jobName = (repoBranch == "master") ? "${folderPath}/$repo" : "${folderPath}/$repo-$repoBranch"

    job(jobName) {

        description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |""".stripMargin())

        logRotator {
            numToKeep(3)
        }

        scm {
            git {
                remote {
                    github("${ghOrgUnit}/${repo}")
                    branch("$repoBranch")
                    credentials("${ghAuthKey}")
                }
                extensions {
                    cloneOptions {
                        // git repo cache which is present on the slaves
                        // it significantly reduces the clone time and also saves a lot of bandwidth
                        reference("/home/jenkins/git-repos/${repo}.git")
                    }
                }
            }
        }

        properties {
            ownership {
                primaryOwnerId("mbiarnes")
                coOwnerIds("almorale", "anstephe")
            }
        }

        jdk("kie-jdk1.8")

        label(get("label"))

        triggers {
            gitHubPushTrigger()
        }

        wrappers {
            timeout {
                elastic(200, 3, get("timeoutMins"))
            }
            timestamps()
            colorizeOutput()

            configFiles {
                mavenSettings("settings-local-maven-repo-nexus"){
                    variable("SETTINGS_XML_FILE")
                    targetLocation("jenkins-settings.xml")
                }
            }
        }

        steps {
            maven {
                mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")
                mavenOpts("-Xms1g -Xmx3g -XX:+CMSClassUnloadingEnabled")
                goals(get("mvnGoals"))
                properties(get("mvnProps"))
                providedSettings("7774c60d-cab3-425a-9c3b-26653e5feba1")
            }
        }

        publishers {
            archiveJunit('**/target/*-reports/TEST-*.xml') {
                allowEmptyResults()
            }
            findbugs("**/spotbugsXml.xml")

            checkstyle("**/checkstyle-result.xml")

            def artifactsToArchive = get("artifactsToArchive")
            def excludedArtifacts = get("excludedArtifacts")
            if (artifactsToArchive) {
                archiveArtifacts {
                    allowEmpty(true)
                    for (artifactPattern in artifactsToArchive) {
                        pattern(artifactPattern)
                    }
                    onlyIfSuccessful(false)
                    if (excludedArtifacts) {
                        for (excludePattern in excludedArtifacts) {
                            exclude(excludePattern)
                        }
                    }
                }
            }

            extendedEmail {
                recipientList("")
                defaultSubject('$DEFAULT_SUBJECT')
                defaultContent('$DEFAULT_CONTENT')
                contentType('default')
                triggers {
                    failure{
                        subject('kiegroup/$JOB_BASE_NAME deploy $BUILD_STATUS')

                        content('\n\nThe status of deploy kiegroup/$JOB_BASE_NAME was: $BUILD_STATUS\n\nPlease go to $BUILD_URL/consoleText\n(IMPORTANT: you need have access to Red Hat VPN to access this link)')

                        sendTo {
                            recipientList("${zulipStream}")
                        }
                    }
                    unstable {
                        subject('kiegroup/$JOB_BASE_NAME deploy $BUILD_STATUS')

                        content('\n\nThe status of deploy kiegroup/$JOB_BASE_NAME was: $BUILD_STATUS\n\nPlease go to $BUILD_URL/consoleText\n(IMPORTANT: you need have access to Red Hat VPN to access this link)\n\n${FAILED_TESTS}')

                        sendTo {
                            recipientList("${zulipStream}")
                        }
                    }
                    success{
                        subject('kiegroup/$JOB_BASE_NAME deploy $BUILD_STATUS')

                        content('\n\nThe status of deploy kiegroup/$JOB_BASE_NAME was: $BUILD_STATUS')

                        sendTo{
                            recipientList("${zulipStream}")
                        }
                    }
                }
            }

            wsCleanup()

        }
    }
}
