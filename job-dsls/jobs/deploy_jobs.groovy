 /**
 * Creates all the standard "deploy" jobs for appformer (formerly known as uberfire) and kiegroup GitHub org. units.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : Constants.GITHUB_ORG_UNIT,
        branch                 : Constants.BRANCH,
        timeoutMins            : 90,
        label                  : "kie-rhel7 && kie-mem8g",
        upstreamMvnArgs        : "-B -e -T1C -DskipTests -Dgwt.compiler.skip=true -Dgwt.skipCompilation=true -Denforcer.skip=true -Dcheckstyle.skip=true -Dfindbugs.skip=true -Drevapi.skip=true clean install",
        mvnGoals               : "-e -nsu -fae -B -T1C -Pwildfly11 clean deploy findbugs:findbugs",
        mvnProps: [
                "full"                     : "true",
                "container"                : "wildfly11",
                "integration-tests"        : "true",
                "maven.test.failure.ignore": "true"
        ],
        ircNotificationChannels: [],
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
        "kie-soup"                  : [
                label                  : "kie-rhel7 && kie-mem4g",
                ircNotificationChannels: ["#logicabyss", "#appformer"],
                downstreamRepos        : ["appformer"]
        ],
        "appformer"                 : [
                label                  : "kie-linux && kie-mem16g",
                mvnProps               : DEFAULTS["mvnProps"] + [
                        "gwt.compiler.localWorkers": "2"
                ],
                ircNotificationChannels: ["#appformer"],
                downstreamRepos        : ["droolsjbpm-build-bootstrap"]
        ],
        "droolsjbpm-build-bootstrap": [
                timeoutMins            : 20,
                label                  : "kie-rhel7 && kie-mem4g",
                ircNotificationChannels: ["#logicabyss"],
                downstreamRepos        : ["droolsjbpm-knowledge"]
        ],
        "droolsjbpm-knowledge"      : [
                timeoutMins            : 40,
                ircNotificationChannels: ["#droolsdev"],
                downstreamRepos        : ["drlx-parser"]
        ],
        "drlx-parser"               : [
                timeoutMins            : 20,
                label                  : "kie-rhel7 && kie-mem4g",
                ircNotificationChannels: ["#droolsdev"],
                downstreamRepos        : ["drools"]
        ],
        "drools"                    : [
                ircNotificationChannels: ["#droolsdev"],
                downstreamRepos        : ["optaplanner", "jbpm"],
                artifactsToArchive     : ["**/target/testStatusListener*"]
        ],
        "optaplanner"               : [
                ircNotificationChannels: ["#optaplanner-dev"],
                downstreamRepos        : ["optaplanner-wb"]
        ],
        "jbpm"                      : [
                timeoutMins            : 120,
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Dcontainer.profile=wildfly11",
                ircNotificationChannels: ["#jbpmdev"],
                downstreamRepos        : ["droolsjbpm-integration", "jbpm-work-items"]
        ],
        "droolsjbpm-integration"    : [
                timeoutMins            : 120,
                ircNotificationChannels: ["#droolsdev", "#jbpmdev"],
                downstreamRepos        : ["droolsjbpm-tools", "kie-uberfire-extensions"]
        ],
        "droolsjbpm-tools"          : [
                ircNotificationChannels: ["#logicabyss"],
                downstreamRepos        : []
        ],
        "kie-uberfire-extensions"   : [
                timeoutMins            : 40,
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["kie-wb-playground"]
        ],
        "kie-wb-playground"         : [
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["kie-wb-common"]
        ],
        "kie-wb-common"             : [
                label                  : "kie-rhel7 && kie-mem16g",
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["drools-wb"]
        ],
        "drools-wb"                 : [
                label                  : "kie-rhel7 && kie-mem16g",
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["jbpm-designer", "optaplanner-wb"]
        ],
        "optaplanner-wb"            : [
                label                  : "kie-rhel7 && kie-mem16g",
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["jbpm-wb"]
        ],
        "jbpm-designer"             : [
                mvnProps               : DEFAULTS["mvnProps"] + [
                        "gwt.compiler.localWorkers": "1"
                ],
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["jbpm-wb"]
        ],
        "jbpm-wb"                   : [
                label                  : "kie-rhel7 && kie-mem16g",
                mvnProps               : DEFAULTS["mvnProps"] + [
                        "gwt.compiler.localWorkers": "1"
                ],
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["kie-wb-distributions", "kie-docs"]
        ],
        "kie-docs"                  : [
                ircNotificationChannels: ["#logicabyss"],
                artifactsToArchive     : ["**/generated-docs/**"],
                downstreamRepos        : ["kie-wb-distributions"]
        ],
        "kie-wb-distributions"      : [
                timeoutMins            : 120,
                label                  : "kie-rhel7 && kie-mem16g",
                mvnGoals               : DEFAULTS["mvnGoals"].replace("-T1C", "-T2") + " -Pkie-wb",
                mvnProps               : DEFAULTS["mvnProps"] + [
                        "gwt.compiler.localWorkers": "1",
                        "webdriver.firefox.bin"    : "/opt/tools/firefox-60esr/firefox-bin",
                        "gwt.memory.settings"      : "-Xmx10g"
                ],
                ircNotificationChannels: ["#guvnordev"],
                artifactsToArchive     : DEFAULTS["artifactsToArchive"] + [
                        "kie-wb-tests/kie-wb-tests-gui/target/screenshots/**"
                ],
                downstreamRepos        : []
        ],
        // following repos are not in repository-list.txt, but we want a deploy jobs for them
        "jbpm-work-items"           : [
                label      : "kie-linux && kie-mem4g",
                timeoutMins: 30,
                ircNotificationChannels: ["#jbpmdev"],
                downstreamRepos        : ["optaweb-employee-rostering"]
        ],
        "optaweb-employee-rostering" : [
                ircNotificationChannels: ["#optaplanner-dev"],
                artifactsToArchive     : DEFAULTS["artifactsToArchive"] + [
                        "**/target/configurations/cargo-profile/profile-log.txt"
                ]
        ],
]

for (repoConfig in REPO_CONFIGS) {
    Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

    String repo = repoConfig.key
    String repoBranch = get("branch")
    String ghOrgUnit = get("ghOrgUnit")

    // Creation of folders where jobs are stored
    folder(Constants.DEPLOY_FOLDER)

    // jobs for master branch don't use the branch in the name
    String jobName = (repoBranch == "master") ? Constants.DEPLOY_FOLDER + "/$repo" : Constants.DEPLOY_FOLDER + "/$repo-$repoBranch"

    job(jobName) {

        disabled()

        description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |""".stripMargin())

        logRotator {
            numToKeep(10)
        }

        scm {
            git {
                remote {
                    github("${ghOrgUnit}/${repo}")
                    branch(repoBranch)
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
                coOwnerIds("pszubiak", "anstephe")
            }
        }

        jdk("kie-jdk1.8")

        label(get("label"))

        triggers {
            scm('H/10 * * * *')
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
            configure { project ->
                project / 'builders' << 'org.kie.jenkinsci.plugins.kieprbuildshelper.StandardBuildUpstreamReposBuilder' {
                    if (repo == "jbpm-work-items") {
                        // jbpm-work-items is _not_ in repository-list.txt, so the default config won't work
                        // this is a workaround to make sure we build all repos until and including jbpm
                        baseRepository "$ghOrgUnit/droolsjbpm-integration"
                    } else {
                        baseRepository "$ghOrgUnit/$repo"
                    }
                    branch "$repoBranch"
                    mavenBuildConfig {
                        mavenHome("/opt/tools/apache-maven-${Constants.UPSTREAM_BUILD_MAVEN_VERSION}")
                        delegate.mavenOpts("-Xmx3g")
                        mavenArgs(get("upstreamMvnArgs"))
                    }
                }
            }
            maven {
                mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")
                mavenOpts("-Xms1g -Xmx3g -XX:+CMSClassUnloadingEnabled")
                goals(get("mvnGoals"))
                properties(get("mvnProps"))
                providedSettings("7774c60d-cab3-425a-9c3b-26653e5feba1")
            }

        }

        publishers {
            wsCleanup()
            archiveJunit('**/target/*-reports/TEST-*.xml') {
                allowEmptyResults()
            }
            findbugs("**/findbugsXml.xml")

            checkstyle("**/checkstyle-result.xml")

            mailer("", false, true)

            irc {
                for (ircChannel in get("ircNotificationChannels")) {
                    channel(name: ircChannel, password: "", notificationOnly: true)
                }
                strategy("FAILURE_AND_FIXED")
                notificationMessage("Default")
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
                def jobNames = downstreamRepos.collect { downstreamRepo ->
                    if (repoBranch == "master") {
                        downstreamRepo
                    } else {
                        // non-master job names are in the format <repo>-<branch>
                        def downstreamRepoBranch = REPO_CONFIGS.get(downstreamRepo).get("branch", DEFAULTS["branch"])
                        "$downstreamRepo-$downstreamRepoBranch"
                    }
                }
                downstream(jobNames, 'UNSTABLE')
            }

        }
    }
}
