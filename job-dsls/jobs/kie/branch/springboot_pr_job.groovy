/**
 * Creates pullrequest (PR) job for Spring Boot integration tests which are in droolsjbpm-integration repository.
 * This job can be run on demand.
 */
import org.kie.jenkins.jobdsl.Constants

def final CONFIG = [
        ghOrgUnit              : Constants.GITHUB_ORG_UNIT,
        branch                 : Constants.BRANCH,
        timeoutMins            : 120,
        ghAuthTokenId          : "kie-ci-token",
        label                  : "kie-rhel7 && kie-mem8g",
        upstreamMvnArgs        : "-B -e -T1C -s \$SETTINGS_XML_FILE -Dkie.maven.settings.custom=\$SETTINGS_XML_FILE -DskipTests -DskipTests -Dgwt.compiler.skip=true -Dgwt.skipCompilation=true -Denforcer.skip=true -Dcheckstyle.skip=true -Dfindbugs.skip=true -Drevapi.skip=true clean install",
        mvnGoals               : "-B -e -nsu -fae clean install -Pspringboot",
        ITTestsParent          : "kie-server-parent/kie-server-tests",
        skippedITTestsModules  : ["kie-server-integ-tests-case-id-generator",
                                  "kie-server-integ-tests-controller",
                                  "kie-server-integ-tests-router",
                                  "kie-server-test-web-service",
                                  "kie-server-integ-tests-custom-extension",
                                  "kie-server-integ-tests-custom-extension-client",
                                  "kie-server-integ-tests-custom-extension-rest",
                                  "kie-server-integ-tests-custom-extension-services",
                                  "kie-server-integ-tests-custom-extension-test"],
        mvnProps               : [
                "full"                     : "true",
                "container"                : "springboot",
                "container.profile"        : "springboot",
                "integration-tests"        : "true",
                "maven.test.failure.ignore": "true"],
        ircNotificationChannels: [],
        artifactsToArchive     : [
                "**/target/*.log",
                "**/target/testStatusListener*",
                "**/kie-server-integ-tests-all/target/kie-server-spring-boot-integ-tests-sample-*.jar",
                "**/kie-server-integ-tests-all/target/test-classes/application.properties"
        ],
        excludedArtifacts      : [
                "**/target/checkstyle.log"
        ]
]

Closure<Object> get = { String key -> CONFIG[key] }

String repo = "droolsjbpm-integration"
String repoBranch = get("branch")
String ghOrgUnit = get("ghOrgUnit")
String ghAuthTokenId = get("ghAuthTokenId")

// Creation of folders where jobs are stored
folder("KIE")
folder("KIE/${repoBranch}")
folder("KIE/${repoBranch}/" + Constants.PULL_REQUEST_FOLDER){
    displayName(Constants.PULL_REQUEST_FOLDER_DISPLAY_NAME)
}
def folderPath = ("KIE/${repoBranch}/" + Constants.PULL_REQUEST_FOLDER)


// jobs for main branch don't use the branch in the name
String jobName = "${folderPath}/${repo}-springboot-${repoBranch}.pr"

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

    jdk("kie-jdk1.8")

    label(get("label"))

    triggers {
        githubPullRequest {
            useGitHubHooks()
            cron("")
            orgWhitelist(["appformer", "kiegroup"])
            allowMembersOfWhitelistedOrgsAsAdmin()
            triggerPhrase(".*[j|J]enkins,?.*execute springboot build.*")
            onlyTriggerPhrase()
            whiteListTargetBranches([repoBranch])
            extensions {
                commitStatus {
                    context('Pull Request Execution - Spring Boot')
                    addTestResults(true)
                }
            }
        }
    }

    wrappers {
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
        }
        // First build the droolsjbpm-integration like in downstream builds,
        // so we have the fresh springboot app
        maven {
            mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")
            mavenOpts("-Xms1g -Xmx3g -XX:+CMSClassUnloadingEnabled")
            goals("-e -fae -nsu -B -T1C clean install -Dfull -DskipTests")
            providedSettings("settings-local-maven-repo-nexus")
        }

        // Integration tests invocation
        maven {
            mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")
            mavenOpts("-Xms1g -Xmx3g -XX:+CMSClassUnloadingEnabled")
            goals(get("mvnGoals") + " -pl " + CONFIG["skippedITTestsModules"].collect { "!:$it"}.join(","))
            properties(get("mvnProps"))
            rootPOM(CONFIG["ITTestsParent"])
            providedSettings("settings-local-maven-repo-nexus")
        }
    }

    publishers {
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

        // Adds authentication token id for github.
        configure { node ->
            node / 'triggers' / 'org.jenkinsci.plugins.ghprb.GhprbTrigger' <<
                    'gitHubAuthId'(ghAuthTokenId)

        }
    }
}

