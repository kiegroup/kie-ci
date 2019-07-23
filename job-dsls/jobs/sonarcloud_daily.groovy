/**
 * Creates jobs running tests with code coverage measurement enabled and reporting results together with code quality
 * statistics into SonarCloud.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : Constants.GITHUB_ORG_UNIT,
        branch                 : Constants.BRANCH,
        timeoutMins            : 90,
        ghAuthTokenId          : "kie-ci2-token",
        label                  : "kie-rhel7 && kie-mem8g",
        upstreamMvnArgs        : "-B -e -T1C -DskipTests -Dgwt.compiler.skip=true -Dgwt.skipCompilation=true -Denforcer.skip=true -Dcheckstyle.skip=true -Dspotbugs.skip=true -Drevapi.skip=true clean install",
        mvnGoals               : "-B -e -nsu -fae -Pwildfly -Prun-code-coverage clean install",
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
        "drools"     : [
                mvnProps: [
                        "runTurtleTests": "true"
                ]
        ],
        "optaplanner": [],
        "appformer" : []
]

for (repoConfig in REPO_CONFIGS) {
    Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

    String repo = repoConfig.key
    String ghOrgUnit = get("ghOrgUnit")

    // Creation of folders where jobs are stored
    folder(Constants.SONARCLOUD_FOLDER)

    String jobName = Constants.SONARCLOUD_FOLDER + "/$repo-sonarcloud-periodic"
    job(jobName) {

        description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |""".stripMargin())

        logRotator {
            daysToKeep(7)
        }

        scm {
            git {
                remote {
                    github("${ghOrgUnit}/${repo}")
                    branch("master")
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
            cron("H 20 * * 1-5")
        }

        wrappers {
            timeout {
                elastic(250, 3, get("timeoutMins"))
            }
            timestamps()
            colorizeOutput()

            credentialsBinding { // Injects SONARCLOUD_TOKEN credentials into an environment variable.
                string("SONARCLOUD_TOKEN", "SONARCLOUD_TOKEN")
            }
        }

        steps {

            maven { // run tests with code coverage measurement enabled
                    mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")
                    mavenOpts("-Xms1g -Xmx3g -XX:+CMSClassUnloadingEnabled")
                    goals(get('mvnGoals'))
                    properties(get("mvnProps"))
            }

            maven {
                mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")
                mavenOpts("-Xms1g -Xmx3g -XX:+CMSClassUnloadingEnabled")
                goals("-B -e -nsu -fae generate-resources -Psonarcloud-analysis")
            }
        }

        publishers {

            archiveJunit('**/target/*-reports/TEST-*.xml') {
                allowEmptyResults()
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
        }
    }
}
