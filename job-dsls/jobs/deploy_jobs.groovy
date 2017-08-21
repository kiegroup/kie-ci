/**
 * Creates all the standard "deploy" jobs for appformer (formerly known as uberfire), dashbuilder and kiegroup GitHub org. units.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        branch                 : "6.5.x",
        timeoutMins            : 60,
        label                  : "rhel7 && mem8g",
        ghOrgUnit              : "kiegroup",
        upstreamMvnArgs        : "-B -e -T1C -DskipTests -Dgwt.compiler.skip=true -Denforcer.skip=true -Dcheckstyle.skip=true -Dfindbugs.skip=true -Drevapi.skip=true clean install",
        mvnGoals               : "-e -nsu -fae -B -T1C -Dfull -Pwildfly10 -Dcontainer=wildfly10 -Dintegration-tests -Dmaven.test.failure.ignore=true clean deploy findbugs:findbugs",
        mvnOpts                : "-Xms1g -Xmx2g -XX:+CMSClassUnloadingEnabled",
        ircNotificationChannels: [],
        artifactsToArchive     : [],
        downstreamRepos        : []
]

// used to override default config for specific repos (if needed)
def final REPO_CONFIGS = [
        "uberfire"                     : [
                ghOrgUnit              : "appformer",
                branch                 : "0.9.x",
                label                  : "linux && mem16g",
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Dgwt.compiler.localWorkers=2",
                ircNotificationChannels: ["#appformer"],
                downstreamRepos        : ["uberfire-extensions-0.9.x"]
        ],
        "uberfire-extensions"          : [
                ghOrgUnit              : "appformer",
                branch                 : "0.9.x",
                label                  : "linux && mem16g",
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Dgwt.compiler.localWorkers=2",
                ircNotificationChannels: ["#appformer"],
                downstreamRepos        : ["dashbuilder-0.5.x"]
        ],
        "dashbuilder"                  : [
                ghOrgUnit              : "dashbuilder",
                branch                 : "0.5.x",
                label                  : "linux && mem16g",
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Dgwt.compiler.localWorkers=2",
                ircNotificationChannels: ["#dashbuilder"],
                downstreamRepos        : ["droolsjbpm-build-bootstrap-6.5.x"]
        ],
        "droolsjbpm-build-bootstrap"   : [
                timeoutMins            : 20,
                label                  : "rhel7 && mem4g",
                ircNotificationChannels: ["#logicabyss"],
                downstreamRepos        : ["droolsjbpm-knowledge-6.5.x"]
        ],
        "droolsjbpm-knowledge"         : [
                timeoutMins            : 40,
                ircNotificationChannels: ["#droolsdev"],
                downstreamRepos        : ["drools-6.5.x"]
        ],
        "drools"                       : [
                ircNotificationChannels: ["#droolsdev"],
                downstreamRepos        : ["optaplanner-6.5.x", "jbpm-6.5.x"],
                artifactsToArchive     : ["**/target/testStatusListener*"]
        ],
        "optaplanner"                  : [
                ircNotificationChannels: ["#optaplanner-dev"],
                downstreamRepos        : ["droolsjbpm-integration-6.5.x", "optaplanner-wb-6.5.x"]
        ],
        "jbpm"                         : [
                timeoutMins            : 120,
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Dcontainer.profile=wildfly10",
                ircNotificationChannels: ["#jbpmdev"],
                downstreamRepos        : ["droolsjbpm-integration-6.5.x"]
        ],
        "droolsjbpm-integration"       : [
                timeoutMins            : 120,
                ircNotificationChannels: ["#droolsdev", "#jbpmdev"],
                downstreamRepos        : ["droolsjbpm-tools-6.5.x", "kie-uberfire-extensions-6.5.x"]
        ],
        "droolsjbpm-tools"             : [
                ircNotificationChannels: ["#logicabyss"],
                downstreamRepos        : []
        ],
        "kie-uberfire-extensions"      : [
                timeoutMins            : 40,
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["guvnor-6.5.x"]
        ],
        "guvnor"                       : [
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["kie-wb-common-6.5.x"]
        ],
        "kie-wb-common"                : [
                label                  : "rhel7 && mem16g",
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["jbpm-form-modeler-6.5.x"]
        ],
        "jbpm-form-modeler"            : [
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["drools-wb-6.5.x"]
        ],
        "drools-wb"                    : [
                label                  : "rhel7 && mem16g",
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["jbpm-designer-6.5.x"]
        ],
        "jbpm-designer"                : [
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Dgwt.compiler.localWorkers=1",
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["jbpm-wb-6.5.x"]
        ],
        "jbpm-wb"                      : [
                label                  : "rhel7 && mem16g",
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Dgwt.compiler.localWorkers=1",
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["optaplanner-wb-6.5.x"]
        ],
        "optaplanner-wb"               : [
                label                  : "rhel7 && mem16g",
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["dashboard-builder-6.5.x"]
        ],
        "dashboard-builder"            : [
                label                  : "rhel7 && mem8g",
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["jbpm-dashboard-6.5.x"]
        ],
        "jbpm-dashboard"               : [
                label                  : "rhel7 && mem8g",
                ircNotificationChannels: ["#guvnordev"],
                downstreamRepos        : ["kie-wb-distributions-6.5.x, kie-docs-6.5.x"]
        ],
        "kie-docs"                     : [
                ircNotificationChannels: ["#logicabyss"],
                artifactsToArchive     : ["**/generated-docs/**"],
                downstreamRepos        : []
        ],
        "kie-wb-distributions"         : [
                timeoutMins            : 120,
                label                  : "rhel7 && mem16g",
                mvnGoals               : DEFAULTS["mvnGoals"] + " -Dgwt.compiler.localWorkers=1 -Dwebdriver.firefox.bin=/opt/tools/firefox-38esr/firefox-bin -Pkie-wb,wildfly10",
                ircNotificationChannels: ["#guvnordev"],
                artifactsToArchive     : ["kie-wb-tests/kie-wb-tests-gui/target/screenshots/**"],
                downstreamRepos        : ["droolsjbpm-build-distribution-6.5.x"]
        ],
        "droolsjbpm-build-distribution": [
                label                  : "rhel7 && mem4g",
                ircNotificationChannels: ["#logicabyss"],
                downstreamRepos        : ["kie-eap-modules-6.5.x"]
        ],
        "kie-eap-modules"              : [
                ghOrgUnit              : "jboss-integration",
                label                  : "rhel7 && mem8g",
                mvnOpts                : "-Xms1g -Xmx4g -XX:+CMSClassUnloadingEnabled",
                ircNotificationChannels: ["#logicabyss"],
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
                baseRepository "$ghOrgUnit/$repo"
                branch "$repoBranch"
                mavenBuildConfig {
                    mavenHome("/opt/tools/apache-maven-${Constants.MAVEN_VERSION}")
                    delegate.mavenOpts("-Xmx2g")
                    mavenArgs(get("upstreamMvnArgs"))
                }
            }
        }

        archivingDisabled(true)
        providedSettings("ci-snapshots-deploy")
        goals(get("mvnGoals"))
        mavenOpts(get("mvnOpts"))
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
