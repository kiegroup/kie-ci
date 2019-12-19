/**
 * Creates downstream pullrequest (PR) jobs for appformer (formerly known as uberfire) and kiegroup GitHub org. units.
 * These jobs execute the full downstream build for a specific PR to make sure the changes do not break the downstream repos.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : Constants.GITHUB_ORG_UNIT,
        branch                 : Constants.BRANCH,
        timeoutMins            : 600,
        label                  : "kie-rhel7 && kie-mem24g",
        ghAuthTokenId          : "kie-ci9-token",
        upstreamMvnArgs        : "-B -e -T1C -s \$SETTINGS_XML_FILE -Dkie.maven.settings.custom=\$SETTINGS_XML_FILE -DskipTests -Dgwt.compiler.skip=true -Dgwt.skipCompilation=true -Denforcer.skip=true -Dcheckstyle.skip=true -Dspotbugs.skip=true -Drevapi.skip=true clean install",
        downstreamMvnGoals     : "-B -e -nsu -fae -s \$SETTINGS_XML_FILE -Dkie.maven.settings.custom=\$SETTINGS_XML_FILE -Pbusiness-central,wildfly,sourcemaps,no-showcase clean install",
        downstreamMvnProps     : [
                "full"                               : "true",
                "container"                          : "wildfly",
                "container.profile"                  : "wildfly",
                "integration-tests"                  : "true",
                "maven.test.failure.ignore"          : "true",
                "maven.test.redirectTestOutputToFile": "true",
                "gwt.compiler.localWorkers"          : 1,
                "webdriver.firefox.bin"              : "/opt/tools/firefox-60esr/firefox-bin"

        ],
        artifactsToArchive     : [
                "**/target/*.log",
                "**/target/testStatusListener*",
                "**/target/screenshots/**",
                "**/target/business-central*wildfly*.war",
                "**/target/business-central*eap*.war",
                "**/target/kie-server-*ee7.war",
                "**/target/kie-server-*webc.war",
                "**/target/jbpm-server*dist*.zip"
        ],
        excludedArtifacts      : [
                "**/target/checkstyle.log"
        ]
]
// override default config for specific repos (if needed)
def final REPO_CONFIGS = [
        "lienzo-core"               : [],
        "lienzo-tests"              : [],
        "droolsjbpm-build-bootstrap": [],
        "kie-soup"                  : [],
        "appformer"                 : [],
        "droolsjbpm-knowledge"      : [],
        "drools"                    : [],
        "optaplanner"               : [],
        "jbpm"                      : [],
        "kie-jpmml-integration"     : [],
        "droolsjbpm-integration"    : [
                downstreamMvnGoals  : DEFAULTS["downstreamMvnGoals"] + " -Pjenkins-pr-builder "
                                      ],
        //"droolsjbpm-tools"          : [], // no other repo depends on droolsjbpm-tools
        "kie-uberfire-extensions"   : [],
        "kie-wb-playground"         : [],
        "kie-wb-common"             : [],
        "drools-wb"                 : [],
        "optaplanner-wb"            : [],
        "jbpm-designer"             : [],
        "jbpm-work-items"           : [],
        "jbpm-wb"                   : []
        //"kie-wb-distributions"      : [] // kie-wb-distributions is the last repo in the chain
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
    String jobName = (repoBranch == "master") ? Constants.PULL_REQUEST_FOLDER + "/$repo-downstream-pullrequests" : Constants.PULL_REQUEST_FOLDER + "/$repo-downstream-pullrequests-$repoBranch"
    job(jobName) {

        disabled()

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
                coOwnerIds("almorale", "anstephe")
            }
        }

        jdk("kie-jdk1.8")

        label(get("label"))

        triggers {
            githubPullRequest {
                orgWhitelist(["appformer", "kiegroup"])
                allowMembersOfWhitelistedOrgsAsAdmin()
                cron("H/5 * * * *")
                triggerPhrase(".*[j|J]enkins,?.*execute full downstream build.*")
                onlyTriggerPhrase()
                whiteListTargetBranches([repoBranch])
                extensions {
                    commitStatus {
                        context('Linux - full downstream')
                        addTestResults(true)
                    }

                }
            }
        }

        wrappers {
            xvnc {
                useXauthority(true)
            }

            timeout {
                elastic(200, 3, get("timeoutMins"))
            }

            configFiles {
                mavenSettings("settings-local-maven-repo-nexus"){
                    variable("SETTINGS_XML_FILE")
                    targetLocation("jenkins-settings.xml")
                }
            }

            timestamps()
            colorizeOutput()
        }

        steps {
            configure { project ->
                project / 'builders' << 'org.kie.jenkinsci.plugins.kieprbuildshelper.UpstreamReposBuilder' {
                    mavenBuildConfig {
                        mavenHome("/opt/tools/apache-maven-${Constants.UPSTREAM_BUILD_MAVEN_VERSION}")
                        delegate.mavenOpts("-Xmx3g")
                        mavenArgs(get("upstreamMvnArgs"))
                    }
                }
                project / 'builders' << 'hudson.tasks.Maven' {
                    mavenName("kie-maven-${Constants.MAVEN_VERSION}")
                    jvmOptions("-Xms1g -Xmx3g -XX:+CMSClassUnloadingEnabled")
                    targets("-e -fae -nsu -B -T1C clean install -s \$SETTINGS_XML_FILE -Dkie.maven.settings.custom=\$SETTINGS_XML_FILE -Dfull -DskipTests")
                }
                project / 'builders' << 'org.kie.jenkinsci.plugins.kieprbuildshelper.DownstreamReposBuilder' {
                    mavenBuildConfig {
                        mavenHome("/opt/tools/apache-maven-${Constants.MAVEN_VERSION}")
                        delegate.mavenOpts("-Xmx3g")
                        mavenArgs(get("downstreamMvnGoals") + " " + get("downstreamMvnProps").collect { k, v -> "-D$k=$v" }.join(" "))
                    }
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
        }

        // Adds authentication token id for github.
        configure { node ->
            node / 'triggers' / 'org.jenkinsci.plugins.ghprb.GhprbTrigger' <<
                    'gitHubAuthId'(ghAuthTokenId)

        }
    }
}
