/**
 * Creates pullrequest (PR) jobs for appformer (formerly known as uberfire) and kiegroup GitHub org. units.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : "kiegroup",
        branch                 : "7.5.x",
        timeoutMins            : 90,
        label                  : "rhel7 && mem8g",
        upstreamMvnArgs        : "-B -e -T1C -DskipTests -Dgwt.compiler.skip=true -Denforcer.skip=true -Dcheckstyle.skip=true -Dfindbugs.skip=true -Drevapi.skip=true clean install",
        mvnGoals               : "-e -nsu -fae -B -T1C -Pwildfly10 clean install",
        mvnProps               : [
                "full"                     : "true",
                "container"                : "wildfly10",
                "container.profile"        : "wildfly10",
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
        "kie-soup"                  : [
                label: "rhel7 && mem4g"
        ],
        "appformer"                  : [
                label    : "rhel7 && mem16g"
        ],
        "droolsjbpm-build-bootstrap": [
                timeoutMins: 30,
                label      : "rhel7 && mem4g"
        ],
        "droolsjbpm-knowledge"      : [
                label: "rhel7 && mem4g"
        ],
        "drools"                    : [],
        "optaplanner"               : [],
        "optaweb-employee-rostering" : [
                artifactsToArchive     : DEFAULTS["artifactsToArchive"] + [
                        "**/target/configurations/cargo-profile/profile-log.txt"
                ]
        ],
        "jbpm"                      : [
                timeoutMins: 120
        ],
        "droolsjbpm-integration"    : [
                timeoutMins: 120
        ],
        "droolsjbpm-tools"          : [],
        "kie-uberfire-extensions"   : [
                label: "rhel7 && mem4g"
        ],
        "kie-wb-playground"         : [
                label: "rhel7 && mem4g"
        ],
        "kie-wb-common"             : [
                label: "rhel7 && mem16g"
        ],
        "drools-wb"                 : [
                label: "rhel7 && mem16g"
        ],
        "optaplanner-wb"            : [],
        "jbpm-designer"             : [
                label: "rhel7 && mem16g"
        ],
        "jbpm-wb"                   : [
                label: "rhel7 && mem16g"
        ],
        "kie-docs"                  : [
                label             : "rhel7 && mem4g",
                artifactsToArchive: DEFAULTS["artifactsToArchive"] + [
                        "**/target/generated-docs/**"
                ]
        ],
        "kie-wb-distributions"      : [
                label             : "linux && mem16g && gui-testing",
                timeoutMins       : 120,
                mvnGoals          : DEFAULTS["mvnGoals"].replace("-T1C", "-T2") + " -Pkie-wb",
                mvnProps          : DEFAULTS["mvnProps"] + [
                        "gwt.compiler.localWorkers": 1,
                        "webdriver.firefox.bin"    : "/opt/tools/firefox-45esr/firefox-bin"
                ],
                artifactsToArchive: DEFAULTS["artifactsToArchive"] + [
                        "**/target/screenshots/**",
                        "**/target/kie-wb*wildfly*.war",
                        "**/target/kie-wb*eap*.war",
                        "**/target/kie-wb*tomcat*.war",
                        "**/target/kie-drools-wb*wildfly*.war",
                        "**/target/kie-drools-wb*eap*.war",
                        "**/target/kie-drools-wb*tomcat*.war"
                ]
        ],
        "jbpm-work-items"           : [
                label      : "linux && mem4g",
                timeoutMins: 30,
        ]
]


for (repoConfig in REPO_CONFIGS) {
    Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

    String repo = repoConfig.key
    String repoBranch = get("branch")
    String ghOrgUnit = get("ghOrgUnit")

    // jobs for master branch don't use the branch in the name
    String jobName = (repoBranch == "master") ? "$repo-pullrequests" : "$repo-pullrequests-$repoBranch"
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

        jdk("jdk1.8")

        label(get("label"))

        triggers {
            githubPullRequest {
                orgWhitelist(["appformer", "kiegroup"])
                allowMembersOfWhitelistedOrgsAsAdmin()
                cron("H/30 * * * *")
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
                elastic(200, 3, get("timeoutMins"))
            }
            timestamps()
            colorizeOutput()
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
            maven {
                mavenInstallation("apache-maven-${Constants.MAVEN_VERSION}")
                mavenOpts("-Xms1g -Xmx3g -XX:+CMSClassUnloadingEnabled")
                goals(get("mvnGoals"))
                properties(get("mvnProps"))

            }
        }

        publishers {
            wsCleanup()
            archiveJunit('**/target/*-reports/TEST-*.xml') {
                allowEmptyResults()
            }
            findbugs("**/findbugsXml.xml")

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
            configure { project ->
                project / 'publishers' << 'org.jenkinsci.plugins.emailext__template.ExtendedEmailTemplatePublisher' {
                    'templateIds' {
                        'org.jenkinsci.plugins.emailext__template.TemplateId' {
                            'templateId'('emailext-template-1441717935622')
                        }
                    }
                }
            }
        }
    }
}
