/**
 * Creates all the standard "deploy" jobs for appformer (formerly known as uberfire) and kiegroup GitHub org. units.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : Constants.GITHUB_ORG_UNIT,
        branch                 : Constants.BRANCH,
        kie_ci_token           : "kie-ci-user-key",
        timeoutMins            : 90,
        label                  : "kie-rhel7 && kie-mem8g",
        mvnGoals               : "-e -nsu -fae -B -Pwildfly clean deploy com.github.spotbugs:spotbugs-maven-plugin:spotbugs",
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
        "lienzo-core"                  : [
                timeoutMins            : 20,
                label                  : "kie-rhel7 && kie-mem4g",
                downstreamRepos        : ["lienzo-tests"]
        ],
        "lienzo-tests"              : [
                timeoutMins            : 20,
                label                  : "kie-rhel7 && kie-mem4g",
                downstreamRepos        : ["kie-soup"]
        ],
        "droolsjbpm-build-bootstrap": [
                timeoutMins            : 30,
                label                  : "kie-rhel7 && kie-mem4g",
                downstreamRepos        : ["kie-soup"]
        ],
        "kie-soup"                  : [
                label                  : "kie-rhel7 && kie-mem4g",
                downstreamRepos        : ["appformer"]
        ],
        "appformer"                 : [
                label                  : "kie-linux && kie-mem16g",
                mvnProps               : DEFAULTS["mvnProps"] + [
                        "gwt.compiler.localWorkers": "2"
                ],
                downstreamRepos        : ["droolsjbpm-knowledge"]
        ],
        "droolsjbpm-knowledge"      : [
                timeoutMins            : 40,
                downstreamRepos        : ["drools"]
        ],
        "drools"                    : [
                downstreamRepos        : ["optaplanner", "jbpm", "kie-jpmml-integration"],
                artifactsToArchive     : ["**/target/testStatusListener*"]
        ],
        "optaplanner"               : [
                downstreamRepos        : ["optaplanner-wb", "optaweb-employee-rostering", "optaweb-vehicle-routing"],
                mvnGoals: "-e -nsu -fae -B clean deploy com.github.spotbugs:spotbugs-maven-plugin:spotbugs",
                mvnProps: [
                        "full"                     : "true",
                        "integration-tests"        : "true",
                        "maven.test.failure.ignore": "true"
                ]
        ],
        "jbpm"                      : [
                timeoutMins            : 120,
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Dcontainer.profile=wildfly",
                downstreamRepos        : ["jbpm-work-items", "kie-jpmml-integration"]
        ],
        "kie-jpmml-integration"     :[
                downstreamRepos        : ["droolsjbpm-integration"]
        ],
        "droolsjbpm-integration"    : [
                timeoutMins            : 120,
                downstreamRepos        : ["droolsjbpm-tools", "kie-uberfire-extensions", "openshift-drools-hacep"]
        ],
        "openshift-drools-hacep"       : [:],
        "droolsjbpm-tools"          : [
                label                  : "kie-linux && kie-mem24g",
                downstreamRepos        : []
        ],
        "kie-uberfire-extensions"   : [
                timeoutMins            : 40,
                downstreamRepos        : ["kie-wb-playground"]
        ],
        "kie-wb-playground"         : [
                downstreamRepos        : ["kie-wb-common"]
        ],
        "kie-wb-common"             : [
                label                  : "kie-rhel7 && kie-mem16g",
                downstreamRepos        : ["drools-wb"]
        ],
        "drools-wb"                 : [
                label                  : "kie-rhel7 && kie-mem16g",
                downstreamRepos        : ["jbpm-designer", "optaplanner-wb"]
        ],
        "optaplanner-wb"            : [
                label                  : "kie-rhel7 && kie-mem16g",
                downstreamRepos        : ["jbpm-wb"]
        ],
        "jbpm-designer"             : [
                mvnProps               : DEFAULTS["mvnProps"] + [
                        "gwt.compiler.localWorkers": "1"
                ],
                downstreamRepos        : ["jbpm-work-items"]
        ],
        "jbpm-work-items"           : [
                label      : "kie-linux && kie-mem4g",
                timeoutMins: 30,
                downstreamRepos        : ["jbpm-wb"]
        ],
        "jbpm-wb"                   : [
                label                  : "kie-rhel7 && kie-mem16g",
                mvnProps               : DEFAULTS["mvnProps"] + [
                        "gwt.compiler.localWorkers": "1"
                ],
                downstreamRepos        : ["kie-wb-distributions", "kie-docs"]
        ],
        "kie-docs"                  : [
                artifactsToArchive     : [],
                downstreamRepos        : ["optaweb-employee-rostering"],
                mvnGoals               : "-e -B clean deploy -Dfull",
                mvnProps               : []
        ],
        "optaweb-employee-rostering" : [
                artifactsToArchive     : DEFAULTS["artifactsToArchive"] + [
                        "**/target/configurations/cargo-profile/profile-log.txt"
                ],
                downstreamRepos        : ["optaweb-vehicle-routing"]
        ],
        "optaweb-vehicle-routing" : [
                artifactsToArchive     : DEFAULTS["artifactsToArchive"] + [
                        "**/target/configurations/cargo-profile/profile-log.txt"
                ],
                downstreamRepos        : ["kie-wb-distributions"]
        ],
        "kie-wb-distributions"      : [
                timeoutMins            : 120,
                label                  : "kie-rhel7 && kie-mem16g",
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Pbusiness-central",
                mvnProps               : DEFAULTS["mvnProps"] + [
                        "gwt.compiler.localWorkers": "1",
                        "webdriver.firefox.bin"    : "/opt/tools/firefox-60esr/firefox-bin",
                        "gwt.memory.settings"      : "-Xmx10g"
                ],
                artifactsToArchive     : DEFAULTS["artifactsToArchive"] + [
                        "business-central-tests/business-central-tests-gui/target/screenshots/**"
                ],
                downstreamRepos        : []
        ]
]

for (repoConfig in REPO_CONFIGS) {
    Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

    String repo = repoConfig.key
    String repoBranch = get("branch")
    String ghOrgUnit = get("ghOrgUnit")
    String kie_ci_token = get("kie_ci_token")
    String zulipStream = get("zulipNotificationStream")

    // Creation of folders where jobs are stored
    folder(Constants.DEPLOY_FOLDER)

    // jobs for master branch don't use the branch in the name
    String jobName = (repoBranch == "master") ? Constants.DEPLOY_FOLDER + "/$repo" : Constants.DEPLOY_FOLDER + "/$repo-$repoBranch"

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
                    credentials("${kie_ci_token}")
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
            if (repo == "kie-wb-distributions") {
                xvnc {
                    useXauthority(false)
                }
            }
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

        if ( "${repo}" != "kie-docs" ) {
            steps {
                maven {
                    mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")
                    mavenOpts("-Xms1g -Xmx3g -XX:+CMSClassUnloadingEnabled")
                    goals(get("mvnGoals"))
                    properties(get("mvnProps"))
                    providedSettings("7774c60d-cab3-425a-9c3b-26653e5feba1")
                }
            }
        } else {
            steps {
                maven {
                    mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")
                    mavenOpts("-Xms1g -Xmx3g -XX:+CMSClassUnloadingEnabled")
                    goals(get("mvnGoals"))
                    providedSettings("7774c60d-cab3-425a-9c3b-26653e5feba1")
                }
            }
        }

        publishers {
            if ( "${repo}" != "kie-docs") {
                archiveJunit('**/target/*-reports/TEST-*.xml') {
                    allowEmptyResults()
                }
                findbugs("**/spotbugsXml.xml")

                checkstyle("**/checkstyle-result.xml")
            }
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

                        content('\n\nStatus of deploy kiegroup/$JOB_BASE_NAME was: $BUILD_STATUS\n\nPlease go to $BUILD_URL/consoleText\n(IMPORTANT: you need have access to Red Hat VPN to access this link)')

                        sendTo {
                            recipientList("${zulipStream}")
                        }
                    }
                    unstable {
                        subject('kiegroup/$JOB_BASE_NAME deploy $BUILD_STATUS')

                        content('\n\nStatus of deploy kiegroup/$JOB_BASE_NAME was: $BUILD_STATUS\n\nPlease go to $BUILD_URL/consoleText\n(IMPORTANT: you need have access to Red Hat VPN to access this link)\n\n${FAILED_TESTS}')

                        sendTo {
                            recipientList("${zulipStream}")
                        }
                    }
                    success{
                        subject('kiegroup/$JOB_BASE_NAME deploy $BUILD_STATUS')

                        content('\n\nStatus of deploy kiegroup/$JOB_BASE_NAME was: $BUILD_STATUS')

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