/**
 * Creates pullrequest (PR) jobs for appformer (formerly known as uberfire), dashbuilder and kiegroup GitHub org. units.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : "kiegroup",
        branch                 : "6.5.x",
        timeoutMins            : 60,
        label                  : "rhel7 && mem8g",
        upstreamMvnArgs        : "-B -e -T1C -DskipTests -Dgwt.compiler.skip=true -Denforcer.skip=true -Dcheckstyle.skip=true -Dfindbugs.skip=true -Drevapi.skip=true clean install",
        mvnOpts                : "-Xms1g -Xmx2g -XX:+CMSClassUnloadingEnabled",
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
        downstreamRepos        : []
]

// override default config for specific repos (if needed)
def final REPO_CONFIGS = [
        "uberfire"                     : [
                ghOrgUnit: "appformer",
                branch   : "0.9.x",
                label    : "rhel7 && mem16g"
        ],
        "uberfire-extensions"          : [
                ghOrgUnit: "appformer",
                branch   : "0.9.x",
                label    : "rhel7 && mem16g"
        ],
        "dashbuilder"                  : [
                ghOrgUnit: "dashbuilder",
                label    : "rhel7 && mem16g"
        ],
        "droolsjbpm-build-bootstrap"   : [
                timeoutMins: 30,
                label      : "rhel7 && mem4g"
        ],
        "droolsjbpm-knowledge"         : [
                label: "rhel7 && mem4g"
        ],
        "drools"                       : [],
        "optaplanner"                  : [],
        "jbpm"                         : [
                timeoutMins: 120
        ],
        "droolsjbpm-integration"       : [
                timeoutMins: 120
        ],
        "droolsjbpm-tools"             : [],
        "kie-uberfire-extensions"      : [
                label: "rhel7 && mem4g"
        ],
        "guvnor"                       : [],
        "kie-wb-common"                : [
                label: "rhel7 && mem16g"
        ],
        "jbpm-form-modeler"            : [],
        "drools-wb"                    : [
                label: "rhel7 && mem16g"
        ],
        "jbpm-designer"                : [
                label: "rhel7 && mem16g"
        ],
        "jbpm-wb"                      : [
                label: "rhel7 && mem16g"
        ],
        "optaplanner-wb"               : [
                label: "rhel7 && mem16g",
        ],
        "dashboard-builder"            : [],
        "jbpm-dashboard"               : [],
        "kie-docs"                     : [
                label             : "rhel7 && mem4g",
                artifactsToArchive: DEFAULTS["artifactsToArchive"] + [
                        "**/target/generated-docs/**"
                ]
        ],
        "kie-wb-distributions"         : [
                label             : "linux && mem16g && gui-testing",
                timeoutMins       : 120,
                mvnGoals          : DEFAULTS["mvnGoals"] + " -Pkie-wb",
                mvnProps          : DEFAULTS["mvnProps"] + [
                        "gwt.compiler.localWorkers": 1,
                        "webdriver.firefox.bin"    : "/opt/tools/firefox-45esr/firefox-bin"
                ],
                artifactsToArchive: DEFAULTS["artifactsToArchive"] + [
                        "kie-wb-tests/kie-wb-tests-gui/target/screenshots/**",
                        "kie-wb/kie-wb-distribution-wars/target/kie-wb-*-wildfly10.war",
                        "kie-drools-wb/kie-drools-wb-distribution-wars/target/kie-drools-wb-*-wildfly10.war"
                ]
        ],
        "droolsjbpm-build-distribution": [],
        "kie-eap-modules"              : [
                ghOrgUnit: "jboss-integration",
                mvnOpts  : "-Xms1g -Xmx4g -XX:+CMSClassUnloadingEnabled"
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
            daysToKeep(14)
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
                orgWhitelist(["appformer", "dashbuilder", "kiegroup"])
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
                                       |\$BUILD_URL/artifact/drools-docs/target/docbook/publish/en-US/html_single/index.html
                                       |\$BUILD_URL/artifact/jbpm-docs/target/docbook/publish/en-US/html_single/index.html
                                       |\$BUILD_URL/artifact/optaplanner-wb-es-docs//target/generated-docs/html_single/index.html
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
                absolute(get("timeoutMins"))
            }
            timestamps()
            colorizeOutput()
        }

        steps {
            configure { project ->
                project / 'builders' << 'org.kie.jenkinsci.plugins.kieprbuildshelper.UpstreamReposBuilder' {
                    mavenBuildConfig {
                        mavenHome("/opt/tools/apache-maven-${Constants.MAVEN_VERSION}")
                        delegate.mavenOpts("-Xmx2g")
                        mavenArgs(get("upstreamMvnArgs"))
                    }
                }
            }
            maven {
                mavenInstallation("apache-maven-${Constants.MAVEN_VERSION}")
                mavenOpts(get("mvnOpts"))
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
            if (artifactsToArchive) {
                archiveArtifacts {
                    allowEmpty(true)
                    for (artifactPattern in artifactsToArchive) {
                        pattern(artifactPattern)
                    }
                    onlyIfSuccessful(false)
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