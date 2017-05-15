/**
 * Creates all the standard "deploy" jobs for appformer (formerly known as uberfire), dashbuilder and kiegroup GitHub org. units.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        branch                 : "7.0.x",
        timeoutMins            : 60,
        label                  : "rhel7 && mem8g",
        ghOrgUnit              : "kiegroup",
        mvnGoals               : "-e -nsu -fae -B -T1C -Dfull -Pwildfly10 -Dcontainer=wildfly10 -Dintegration-tests -Dmaven.test.failure.ignore=true clean deploy findbugs:findbugs",
        ircNotificationChannels: [],
        artifactsToArchive     : [],
        downstreamRepos        : []
]

// used to override default config for specific repos (if needed)
def final REPO_CONFIGS = [
        "uberfire"                  : [
                ghOrgUnit              : "appformer",
                label                  : "linux && mem16g",
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Dgwt.compiler.localWorkers=2",
                ircNotificationChannels: ["#uberfire"],
                downstreamRepos        : ["dashbuilder-0.6.x"],
                branch                 : "1.0.x"
        ],
        "dashbuilder"               : [
                ghOrgUnit              : "dashbuilder",
                label                  : "linux && mem16g",
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Dgwt.compiler.localWorkers=2",
                ircNotificationChannels: ["#dashbuilder"],
                downstreamRepos        : ["droolsjbpm-build-bootstrap-7.0.x"],
                branch                 : "0.6.x"
        ],
        "droolsjbpm-build-bootstrap": [
                timeoutMins            : 20,
                label                  : "rhel7 && mem4g",
                ircNotificationChannels: ["#logicabyss"],
                downstreamRepos        : ["droolsjbpm-knowledge-7.0.x"]
        ],
        "droolsjbpm-knowledge"      : [
                timeoutMins            : 40,
                ircNotificationChannels: ["#droolsdev"],
                downstreamRepos        : ["drools-7.0.x"]
        ],
        "drools"                    : [
                ircNotificationChannels: ["#droolsdev"],
                downstreamRepos        : ["optaplanner-7.0.x", "jbpm-7.0.x"],
                artifactsToArchive     : ["**/target/testStatusListener*"]
        ],
        "optaplanner"               : [
                ircNotificationChannels: ["#optaplanner-dev"],
                downstreamRepos        : ["@optaplanner-wb-7.0.x"]
        ],
        "jbpm"                      : [
                timeoutMins            : 120,
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Dcontainer.profile=wildfly10",
                ircNotificationChannels: ["#jbpmdev"],
                downstreamRepos        : ["droolsjbpm-integration-7.0.x"]
        ],
        "droolsjbpm-integration"    : [
                timeoutMins            : 120,
                ircNotificationChannels: ["#droolsdev", "#jbpmdev"],
                downstreamRepos        : ["droolsjbpm-tools-7.0.x", "kie-uberfire-extensions-7.0.x"]
        ],
        "droolsjbpm-tools"          : [
                ircNotificationChannels: ["#logicabyss"],
                downstreamRepos        : []
        ],
        "kie-uberfire-extensions"   : [
                timeoutMins            : 40,
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["guvnor-7.0.x"]
        ],
        "guvnor"                    : [
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["kie-wb-playground-7.0.x"]
        ],
        "kie-wb-playground"         : [
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["kie-wb-common-7.0.x"]
        ],
        "kie-wb-common"             : [
                label                  : "rhel7 && mem16g",
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["jbpm-form-modeler-7.0.x"]
        ],
        "jbpm-form-modeler"         : [
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["drools-wb-7.0.x"]
        ],
        "drools-wb"                 : [
                label                  : "rhel7 && mem16g",
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["jbpm-designer-7.0.x", "optaplanner-wb-7.0.x"]
        ],
        "optaplanner-wb"            : [
                label                  : "rhel7 && mem16g",
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["jbpm-wb-7.0.x"]
        ],
        "jbpm-designer"             : [
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Dgwt.compiler.localWorkers=1",
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["jbpm-wb-7.0.x"]
        ],
        "jbpm-wb"                   : [
                label                  : "rhel7 && mem16g",
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Dgwt.compiler.localWorkers=1",
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["kie-wb-distributions-7.0.x", "kie-docs-7.0.x"]
        ],
        "kie-docs"                  : [
                ircNotificationChannels: ["#logicabyss"],
                artifactsToArchive     : ["**/generated-docs/**"],
                downstreamRepos        : ["kie-wb-distributions-7.0.x"]
        ],
        "kie-wb-distributions"      : [
                timeoutMins            : 120,
                label                  : "rhel7 && mem16g",
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Dgwt.compiler.localWorkers=1 -Dwebdriver.firefox.bin=/opt/tools/firefox-38esr/firefox-bin -Pkie-wb,wildfly10",
                ircNotificationChannels: ["#guvnordev"],
                artifactsToArchive     : ["kie-wb-tests/kie-wb-tests-gui/target/screenshots/**"],
                downstreamRepos        : []
        ]
]

for (repoConfig in REPO_CONFIGS) {
    Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

    String repo = repoConfig.key
    String repoBranch = get("branch")
    String ghOrgUnit = get("ghOrgUnit")

    // jobs for master branch don't use the branch in the name
    String jobName = (repoBranch == "master") ? repo : "$repo-$repoBranch"

    mavenJob(jobName) {

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
                primaryOwnerId("psiroky")
                coOwnerIds("mbiarnes")
            }
        }

        jdk("jdk1.8")

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
                absolute(get("timeoutMins"))
            }
            timestamps()
            colorizeOutput()
        }

        configure { project ->
            project / 'prebuilders' << 'org.kie.jenkinsci.plugins.kieprbuildshelper.StandardBuildUpstreamReposBuilder' {
                baseRepository "$repo"
                branch "$repoBranch"
            }
        }

        archivingDisabled(true)
        providedSettings("ci-snapshots-deploy")
        goals(get("mvnGoals"))
        mavenOpts("-Xms1g -Xmx2g -XX:+CMSClassUnloadingEnabled")
        mavenInstallation("apache-maven-${Constants.MAVEN_VERSION}")

        publishers {
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
            if (artifactsToArchive) {
                archiveArtifacts {
                    allowEmpty(true)
                    for (artifactPattern in artifactsToArchive) {
                        pattern(artifactPattern)
                    }
                    onlyIfSuccessful(false)
                }
            }

            def downstreamRepos = get("downstreamRepos")
            if (downstreamRepos) {
                downstream(downstreamRepos, 'UNSTABLE')
            }

        }
    }
}
