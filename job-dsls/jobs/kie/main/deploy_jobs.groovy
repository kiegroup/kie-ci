/**
 * Creates all the standard "deploy" jobs for appformer (formerly known as uberfire) and kiegroup GitHub org. units.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : Constants.GITHUB_ORG_UNIT,
        branch                 : Constants.BRANCH,
        ghAuthKey              : "kie-ci-user-key",
        timeoutMins            : 90,
        buildHistory           : 3,
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
        "droolsjbpm-build-bootstrap": [
                timeoutMins            : 30,
                label                  : "kie-rhel7 && kie-mem4g",
                downstreamRepos        : ["kie-soup-" + Constants.BRANCH]
        ],
        "kie-soup"                  : [
                label                  : "kie-rhel7 && kie-mem4g",
                downstreamRepos        : ["appformer-" + Constants.BRANCH, "droolsjbpm-knowledge-" + Constants.BRANCH]
        ],
        "droolsjbpm-knowledge"      : [
                timeoutMins            : 40,
                downstreamRepos        : ["drools-" + Constants.BRANCH]
        ],
        "drools"                    : [
                downstreamRepos        : ["optaplanner-" + Constants.BRANCH, "jbpm-" + Constants.BRANCH],
                artifactsToArchive     : ["**/target/testStatusListener*"]
        ],
        "optaplanner"               : [
                mvnGoals: "-e -fae -B clean deploy com.github.spotbugs:spotbugs-maven-plugin:spotbugs",
                mvnProps: [
                        "full"                     : "true",
                        "integration-tests"        : "true",
                        "maven.test.failure.ignore": "true"
                ],
                downstreamRepos        : ["optaplanner-employee-rostering-" + Constants.BRANCH, "optaweb-vehicle-routing-" + Constants.BRANCH]
        ],
        "lienzo-core"                  : [
                timeoutMins            : 20,
                label                  : "kie-rhel7 && kie-mem4g",
                downstreamRepos        : ["lienzo-tests-" + Constants.BRANCH]
        ],
        "lienzo-tests"              : [
                timeoutMins            : 20,
                label                  : "kie-rhel7 && kie-mem4g",
                downstreamRepos        : ["appformer-" + Constants.BRANCH]
        ],
        "appformer"                 : [
                label                  : "kie-rhel7 && kie-mem16g",
                mvnProps               : DEFAULTS["mvnProps"] + [
                        "gwt.compiler.localWorkers": "2"
                ],
                downstreamRepos        : ["kie-uberfire-extensions-" + Constants.BRANCH]
        ],
        "kie-uberfire-extensions"   : [
                timeoutMins            : 40,
                downstreamRepos        : ["kie-wb-common-" + Constants.BRANCH]
        ],
        "jbpm"                      : [
                timeoutMins            : 120,
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Dcontainer.profile=wildfly",
                downstreamRepos        : ["kie-jpmml-integration-" + Constants.BRANCH]
        ],
        "kie-jpmml-integration"     :[
                downstreamRepos        : ["droolsjbpm-integration-" + Constants.BRANCH]
        ],
        "droolsjbpm-integration"    : [
                timeoutMins            : 240,
                downstreamRepos        : ["openshift-drools-hacep-" + Constants.BRANCH,"kie-wb-playground-" + Constants.BRANCH,"jbpm-work-items-" + Constants.BRANCH,"process-migration-service-" + Constants.BRANCH]
        ],
        "kie-wb-playground"         : [
                downstreamRepos        : ["kie-wb-common-" + Constants.BRANCH]
        ],
        "kie-wb-common"             : [
                timeoutMins            : 180,
                label                  : "kie-rhel7 && kie-mem16g",
                downstreamRepos        : ["drools-wb-" + Constants.BRANCH,"jbpm-designer-" + Constants.BRANCH]
        ],
        "drools-wb"                 : [
                label                  : "kie-rhel7 && kie-mem16g",
                downstreamRepos        : ["jbpm-wb-" + Constants.BRANCH, "optaplanner-wb-" + Constants.BRANCH]
        ],
        "jbpm-designer"             : [
                mvnProps               : DEFAULTS["mvnProps"] + [
                        "gwt.compiler.localWorkers": "1"
                ],
                downstreamRepos        : ["jbpm-work-items-" + Constants.BRANCH]
        ],
        "jbpm-work-items"           : [
                label      : "kie-rhel7 && kie-mem4g",
                timeoutMins: 30,
                downstreamRepos        : ["jbpm-wb-" + Constants.BRANCH]
        ],
        "jbpm-wb"                   : [
                label                  : "kie-rhel7 && kie-mem16g",
                mvnProps               : DEFAULTS["mvnProps"] + [
                        "gwt.compiler.localWorkers": "1"
                ],
                downstreamRepos        : ["kie-wb-distributions-" + Constants.BRANCH]
        ],
        "optaplanner-wb"            : [
                label                  : "kie-rhel7 && kie-mem16g",
                downstreamRepos        : ["kie-wb-distributions-" + Constants.BRANCH]
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
        ],
        "openshift-drools-hacep"       : [:],
        "process-migration-service"    : [:],
        "kie-docs"                  : [
                artifactsToArchive     : [],
                downstreamRepos        : ["optaweb-employee-rostering-" + Constants.BRANCH],
                mvnGoals               : "-e -B clean deploy -Dfull",
                mvnProps               : []
        ],
        "optaweb-employee-rostering" : [
                artifactsToArchive     : DEFAULTS["artifactsToArchive"] + [
                        "**/target/configurations/cargo-profile/profile-log.txt"
                ],
                downstreamRepos        : ["optaweb-vehicle-routing-" + Constants.BRANCH]
        ],
        "optaweb-vehicle-routing" : [
                artifactsToArchive     : DEFAULTS["artifactsToArchive"] + [
                        "**/target/configurations/cargo-profile/profile-log.txt"
                ],
                downstreamRepos        : []
        ]

]

for (repoConfig in REPO_CONFIGS) {
    Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

    String repo = repoConfig.key
    String repoBranch = get("branch")
    String ghOrgUnit = get("ghOrgUnit")
    String ghAuthKey = get("ghAuthKey")
    String zulipStream = get("zulipNotificationStream")

    // Creation of folders where jobs are stored
    folder("KIE")
    folder("KIE/${repoBranch}")
    folder("KIE/${repoBranch}/" + Constants.DEPLOY_FOLDER)

    def folderPath = ("KIE/${repoBranch}/" + Constants.DEPLOY_FOLDER)

    // jobs for main branch don't use the branch in the name
    String jobName = (repoBranch == "main") ? "${folderPath}/$repo" : "${folderPath}/$repo-$repoBranch"

    job(jobName) {

        description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |""".stripMargin())

        logRotator {
            numToKeep(get("buildHistory"))
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
                        // git repo cache which is present on the agents
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

        if (repo == "optaplanner") {
            jdk("kie-jdk11")
        } else {
            jdk("kie-jdk1.8")}

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
                mavenSettings("7774c60d-cab3-425a-9c3b-26653e5feba1"){
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
                recordIssues {
                    tools {
                        checkStyle {
                            pattern("${Constants.CHECKSTYLE_FILE}")
                        }
                    }
                }
                recordIssues {
                    tools {
                        findBugs {
                            pattern("${Constants.FINDBUGS_FILE}")
                        }
                    }
                }
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

            def downstreamRepos = get("downstreamRepos")
            if (downstreamRepos) {
                downstream(downstreamRepos, 'UNSTABLE')
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
