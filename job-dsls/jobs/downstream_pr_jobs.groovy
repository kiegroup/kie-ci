/**
 * Creates downstream pullrequest (PR) jobs for appformer (formerly known as uberfire), dashbuilder and kiegroup GitHub org. units.
 * These jobs execute the full downstream build for a specific PR to make sure the changes do not break the downstream repos.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : "kiegroup",
        branch                 : "master",
        timeoutMins            : 180,
        label                  : "rhel7 && mem16g",
        downstreamMvnGoals     : "-e -nsu -fae -B -T1C -Pwildfly10 clean install",
        downstreamMvnProps     : [
                "full"                               : "true",
                "container"                          : "wildfly10",
                "container.profile"                  : "wildfly10",
                "integration-tests"                  : "true",
                "maven.test.failure.ignore"          : "true",
                "maven.test.redirectTestOutputToFile": "true",
                "gwt.compiler.localWorkers"          : 1
        ],
        artifactsToArchive     : [
                "**/target/testStatusListener*",
                "**/target/screenshots/**",
                "**/target/kie-wb-*.war",
                "**/target/kie-drools-wb-*.war",
        ]
]

// override default config for specific repos (if needed)
def final REPO_CONFIGS = [
        "uberfire"                  : [
                ghOrgUnit: "appformer",
        ],
        "dashbuilder"               : [
                ghOrgUnit: "dashbuilder",
        ],
        "droolsjbpm-build-bootstrap": [],
        "droolsjbpm-knowledge"      : [],
        "drools"                    : [],
        "optaplanner"               : [],
        "jbpm"                      : [],
        "droolsjbpm-integration"    : [],
        //"droolsjbpm-tools"          : [], // no other repo depends on droolsjbpm-tools
        "kie-uberfire-extensions"   : [],
        "guvnor"                    : [],
        "kie-wb-playground"         : [],
        "kie-wb-common"             : [],
        "jbpm-form-modeler"         : [],
        "drools-wb"                 : [],
        "optaplanner-wb"            : [],
        "jbpm-designer"             : [],
        "jbpm-wb"                   : [],
        //"kie-docs"                  : [], // no other repo depends on kie-docs
        //"kie-wb-distributions"      : [] // kie-wb-distributions is the last repo in the chain
]


for (repoConfig in REPO_CONFIGS) {
    Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

    String repo = repoConfig.key
    String repoBranch = get("branch")
    String ghOrgUnit = get("ghOrgUnit")

    // jobs for master branch don't use the branch in the name
    String jobName = (repoBranch == "master") ? "$repo-downstream-pullrequests" : "$repo-downstream-pullrequests-$repoBranch"
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
                primaryOwnerId("psiroky")
                coOwnerIds("mbiarnes")
            }
        }

        jdk("jdk1.8")

        label(get("label"))

        triggers {
            githubPullRequest {
                orgWhitelist(["appformer", "dashbuilder", "kiegroup"])
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
                useXauthority(false)
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
                }
                project / 'builders' << 'hudson.tasks.Maven' {
                    mavenName("apache-maven-${Constants.MAVEN_VERSION}")
                    jvmOptions("-Xms1g -Xmx2g -XX:+CMSClassUnloadingEnabled")
                    targets("-e -fae -nsu -B -T1C clean install -Dfull -DskipTests")
                }
                project / 'builders' << 'org.kie.jenkinsci.plugins.kieprbuildshelper.DownstreamReposBuilder' {
                    mvnArgLine(get("downstreamMvnGoals") + " " + get("downstreamMvnProps").collect { k, v -> "-D$k=$v" }.join(" "))
                }
            }
        }

        publishers {
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