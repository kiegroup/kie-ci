/**
 * Creates all the turtle tests jobs for drools and optaplanner
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : Constants.GITHUB_ORG_UNIT,
        branch                 : Constants.BRANCH,
        timeoutMins            : 90,
        label                  : "kie-rhel7 && kie-mem16g && !built-in",
        upstreamMvnArgs        : "-B -e -T1C -DskipTests -Dgwt.compiler.skip=true -Dgwt.skipCompilation=true -Denforcer.skip=true -Dcheckstyle.skip=true -Dspotbugs.skip=true -Drevapi.skip=true clean install",
        mvnGoals               : "-e -B -T1C -fae -Dfull -DrunTurtleTests clean install",
        javadk                 : Constants.JDK_TOOL,
        trigga                 : "H 22 * * *",
        jobDesc                : "Slow (turtle) tests which are disabled for default Drools build, as they take considerable amount of time (tens of minutes). The job is executed once a day. \n" +
                "Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated. \n" +
                "Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info."
]

// used to override default config for specific repos (if needed)
def final REPO_CONFIGS = [
        "drools"                    : [],

        "optaplanner"               : [
                mvnGoals            : ["-B -U -e -fae -Dfull -Dmaven.test.failure.ignore=true -DrunTurtleTests=true clean install"],
                trigga              : ["H H * * 2"],
                jobDesc             : ["The turtleTests take days to run. Therefor, this job doesn't run often. \n" +
                                               "Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated. \n" +
                                               "Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info."]
        ]
]

for (repoConfig in REPO_CONFIGS) {
    Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

    String repo = repoConfig.key
    String repoBranch = get("branch")
    String ghOrgUnit = get("ghOrgUnit")

    // Creation of folders where jobs are stored
    folder("KIE")
    folder("KIE/${repoBranch}")
    folder("KIE/${repoBranch}/" + Constants.DEPLOY_FOLDER)
    def folderPath = ("KIE/${repoBranch}/" + Constants.DEPLOY_FOLDER)

    // jobs for main branch don't use the branch in the name
    String jobName = (repoBranch == "main") ? "${folderPath}/${repo}-turtleTests" : "${folderPath}/${repo}-${repoBranch}-turtleTests"

    mavenJob(jobName) {

        description(get("jobDesc"))

        logRotator {
            numToKeep(5)
        }

        scm {
            git {
                remote {
                    github("${ghOrgUnit}/${repo}")
                    branch("*/${repoBranch}")
                }
                extensions {
                    cloneOptions {
                        // git repo cache which is present on the agents
                        // it significantly reduces the clone time and also saves a lot of bandwidth
                        reference("/home/jenkins/git-repos/${repo}.git")
                    }
                }
            }
        }

        jdk(get("javadk"))

        label(get("label"))

        triggers {
            scm(get("trigga"))
        }

        wrappers {
            timestamps()
            colorizeOutput()
            preBuildCleanup()
        }

        preBuildSteps {
            if ( "${repo}" == "drools") {

                configure { node ->
                    node / 'prebuilders' << 'org.kie.jenkinsci.plugins.kieprbuildshelper.StandardBuildUpstreamReposBuilder' {
                        baseRepository("${ghOrgUnit}/${repo}")
                        branch("${repoBranch}")
                        mavenBuildConfig {
                            mavenHome("/opt/tools/apache-maven-${Constants.UPSTREAM_BUILD_MAVEN_VERSION}")
                            mavenOpts("-Xmx2g")
                            mavenArgs(get("upstreamMvnArgs"))
                        }
                    }
                }
            }
        }

        mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")
        mavenOpts("-Xms1g -Xmx3g -XX:+CMSClassUnloadingEnabled")
        rootPOM("pom.xml")
        goals(get("mvnGoals"))
        archivingDisabled(true)
        siteArchivingDisabled(true)
        providedSettings("7774c60d-cab3-425a-9c3b-26653e5feba1")

        configure { node ->
            node/ 'reporters' << 'hudson.maven.reporters.MavenMailer' {
                recipients()
                dontNotifyEveryUnstableBuild(false)
                sendToIndividuals(false)
                perModuleEmail(true)
            }
        }

    }
}