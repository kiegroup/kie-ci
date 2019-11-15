/**
 * Creates pullrequest (PR) jobs for appformer (formerly known as uberfire) and kiegroup GitHub org. units.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : Constants.GITHUB_ORG_UNIT,
        branch                 : Constants.BRANCH,
        timeoutMins            : 90,
        ghAuthTokenId          : "kie-ci2-token",
        label                  : "kie-rhel7 && kie-mem8g",
        upstreamMvnArgs        : "-B -e -T1C -s \$SETTINGS_XML_FILE -Dkie.maven.settings.custom=\$SETTINGS_XML_FILE -DskipTests -Dgwt.compiler.skip=true -Dgwt.skipCompilation=true -Denforcer.skip=true -Dcheckstyle.skip=true -Dspotbugs.skip=true -Drevapi.skip=true clean install",
        mvnGoals               : "-B -e -nsu -fae -Pwildfly clean install",
        mvnProps               : [
                "full"                     : "true",
                "container"                : "wildfly",
                "container.profile"        : "wildfly",
                "integration-tests"        : "true",
                "maven.test.failure.ignore": "true"],
        ircNotificationChannels: [],
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
                artifactsToArchive: DEFAULTS["artifactsToArchive"] + [
                        "**/cypress/screenshots/**",
                        "**/cypress/videos/**"
                ]
        ],
        "optaweb-vehicle-routing" : [
                artifactsToArchive: DEFAULTS["artifactsToArchive"] + [
                        "**/cypress/screenshots/**",
                        "**/cypress/videos/**"
                ]
        ],
        "jbpm"                      : [
                timeoutMins: 120
        ],
        "kie-jpmml-integration"     : [
                label: "kie-rhel7 && kie-mem4g"
        ],
        "droolsjbpm-integration"    : [
                timeoutMins: 180,
                label    : "kie-rhel7 && kie-mem16g",
                artifactsToArchive     : DEFAULTS["artifactsToArchive"] + [
                        "**/target/kie-server-*ee7.war",
                        "**/target/kie-server-*webc.war"
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
                timeoutMins: 120,
                label: "kie-rhel7 && kie-mem16g"
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
                timeoutMins: 30,
        ],
        "jbpm-wb"                   : [
                label: "kie-rhel7 && kie-mem16g",
                artifactsToArchive     : DEFAULTS["artifactsToArchive"] + [
                        "**/target/jbpm-wb-case-mgmt-showcase*.war",
                        "**/target/jbpm-wb-showcase.war"
                ]
        ],
        "kie-wb-distributions"      : [
                label             : "kie-linux && kie-mem24g && gui-testing",
                timeoutMins       : 120,
                mvnGoals          : DEFAULTS["mvnGoals"] + " -Pbusiness-central",
                mvnProps          : DEFAULTS["mvnProps"] + [
                        "gwt.compiler.localWorkers": 1,
                        "webdriver.firefox.bin"    : "/opt/tools/firefox-60esr/firefox-bin",
                        "gwt.memory.settings"      : "-Xmx10g"
                ],
                artifactsToArchive: DEFAULTS["artifactsToArchive"] + [
                        "**/target/screenshots/**",
                        "**/target/business-central*wildfly*.war",
                        "**/target/business-central*eap*.war",
                        "**/target/jbpm-server*dist*.zip"
                ]
        ]

]

def final SONARCLOUD_ENABLED_REPOSITORIES = ["optaplanner", "drools", "appformer", "jbpm", "drools-wb", "kie-soup", "droolsjbpm-integration", "kie-wb-common"]

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
                cron("H/7 * * * *")
                whiteListTargetBranches([repoBranch])
                extensions {
                    commitStatus {
                        context('Linux')
                        addTestResults(true)
                    }
                    if (repo == "kie-docs") {
                        buildStatus {
                            completedStatus("SUCCESS",
                                    """|Build successful! See generated HTML docs:
                                       |
                                       |\$BUILD_URL/artifact/docs/drools-docs/target/generated-docs/html_single/index.html
                                       |\$BUILD_URL/artifact/docs/jbpm-docs/target/generated-docs/html_single/index.html
                                       |\$BUILD_URL/artifact/docs/optaplanner-wb-es-docs//target/generated-docs/html_single/index.html
                                       |""".stripMargin())
                        }
                    }
                }
            }
        }

        wrappers {
            if (repo == "kie-wb-distributions") {
                xvnc {
                    useXauthority(false)
                }
            }
            timeout {
                elastic(250, 3, get("timeoutMins"))
            }
            timestamps()
            colorizeOutput()

            configFiles {
                mavenSettings("settings-local-maven-repo-nexus"){
                    variable("SETTINGS_XML_FILE")
                    targetLocation("jenkins-settings.xml")
                }
            }
            
            if (repo in SONARCLOUD_ENABLED_REPOSITORIES) {
                credentialsBinding { // Injects SONARCLOUD_TOKEN credentials into an environment variable.
                    string("SONARCLOUD_TOKEN", "SONARCLOUD_TOKEN")
                }
            }
        }

        steps {
            if (repo != "jbpm-work-items") {
                configure { project ->
                    project / 'builders' << 'org.kie.jenkinsci.plugins.kieprbuildshelper.UpstreamReposBuilder' {
                        mavenBuildConfig {
                            mavenHome("/opt/tools/apache-maven-${Constants.UPSTREAM_BUILD_MAVEN_VERSION}")
                            delegate.mavenOpts("-Xmx3g")
                            mavenArgs(get("upstreamMvnArgs"))
                        }
                    }
                }
            }

            def mavenGoals =
                repo in SONARCLOUD_ENABLED_REPOSITORIES ? "-Prun-code-coverage ${get('mvnGoals')}" : get("mvnGoals")

            maven {
                    mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")
                    mavenOpts("-Xms1g -Xmx3g -XX:+CMSClassUnloadingEnabled")
                    goals(mavenGoals)
                    properties(get("mvnProps"))
                    providedSettings("settings-local-maven-repo-nexus")
            }

            if (repo in SONARCLOUD_ENABLED_REPOSITORIES) { // additional maven build step to report results to SonarCloud
                maven {
                    mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")
                    mavenOpts("-Xms1g -Xmx3g -XX:+CMSClassUnloadingEnabled")
                    goals("-B -e -nsu -fae generate-resources -Psonarcloud-analysis")
                    providedSettings("settings-local-maven-repo-nexus")
                }
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
                recipientList('$ghprbActualCommitAuthorEmail')
                defaultSubject('$DEFAULT_SUBJECT')
                defaultContent('$DEFAULT_CONTENT')
                contentType('default')
                triggers {
                    failure{
                        subject('PR build FAILED: $JOB_BASE_NAME #$ghprbPullId')

                        content('$ghprbPullTitle \nPlease go to $BUILD_URL \n(IMPORTANT: you need have access to Red Hat VPN to access this link) \n\n${BUILD_LOG_REGEX, regex="(?i)\\\\b(error|exception|fatal|fail(ed|ure)|un(defined|resolved))\\\\b", linesBefore=500, linesAfter=250} \n\n${FAILED_TESTS}')

                        sendTo {
                            recipientList()
                        }
                    }
                    unstable {
                        subject('PR build UNSTABLE: $JOB_BASE_NAME #$ghprbPullId')

                        content('$ghprbPullTitle \nPlease go to $BUILD_URL \n(IMPORTANT: you need have access to Red Hat VPN to access this link) \n\n${BUILD_LOG_REGEX, regex="(?i)\\\\b(error|exception|fatal|fail(ed|ure)|un(defined|resolved))\\\\b", linesBefore=500, linesAfter=250} \n\n${FAILED_TESTS}')

                        sendTo {
                            recipientList()
                        }
                    }
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
