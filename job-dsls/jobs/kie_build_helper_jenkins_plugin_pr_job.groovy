import org.kie.jenkins.jobdsl.Constants

// Creates or updates a free style job.
def jobDefinition = job("kie-build-helper-jenkins-plugin-pullrequests") {

    // Sets a description for the job.
    description("This job is responsible for verifing kie-build-helper-jenkins-plugin pull requests.")

    // Adds custom properties to the job.
    properties {

        // Allows to configure job ownership.
        ownership {

            // Sets the name of the primary owner of the job.
            primaryOwnerId("pszubiak")

            // Adds additional users, who have ownership privileges.
            coOwnerIds("mbiarnes", "anstephe")
        }
    }

    // Manages how long to keep records of the builds.
    logRotator {

        // If specified, build records are only kept up to this number of days.
        daysToKeep(7)
    }

    // Name of the JDK installation to use for this job.
    jdk("jdk1.8")

    // Label which specifies which nodes this job can run on.
    label("rhel7&&mem4g")

    // Allows a job to check out sources from an SCM provider.
    scm {

        // Adds a Git SCM source.
        git {

            // Specify the branches to examine for changes and to build.
            branch("\${sha1}")

            // Adds a remote.
            remote {

                // Sets a remote URL for a GitHub repository.
                github("kiegroup/kie-build-helper-jenkins-plugin")

                // Sets a name for the remote.
                name("origin")

                // Sets a refspec for the remote.
                refspec("+refs/pull/*:refs/remotes/origin/pr/*")
            }

            // Adds additional behaviors.
            extensions {

                // Specifies behaviors for cloning repositories.
                cloneOptions {

                    // Specify a folder containing a repository that will be used by Git as a reference during clone operations.
                    reference("/home/jenkins/git-repos/kie-build-helper-jenkins-plugin.git")
                }
            }
        }
    }

    // Adds build triggers to the job.
    triggers {

        // This plugin builds pull requests in github and report results.
        githubPullRequest {

            // List of organizations. Their members will be whitelisted.
            orgWhitelist(["appformer", "kiegroup"])

            // Use this option to allow members of whitelisted organisations to behave like admins, i.e. whitelist users and trigger pull request testing.
            allowMembersOfWhitelistedOrgsAsAdmin()

            //  This field follows the syntax of cron (with minor differences). Specifically, each line consists of 5 fields separated by TAB or whitespace
            cron("H/10 * * * *")

            // Adding branches to this whitelist allows you to selectively test pull requests destined for these branches only.
            // Supports regular expressions (e.g. 'master', 'feature-.*').
            whiteListTargetBranches(["master"])

            extensions {

                // Update commit status during build.
                commitStatus {

                    // A string label to differentiate this status from the status of other systems. Default: "default"
                    context('Linux')

                    // Add test result one liner
                    addTestResults(true)
                }
            }
        }
    }

    // Adds pre/post actions to the job.
    wrappers {

        // Add a timeout to the build job.
        timeout {

            // Aborts the build based on a fixed time-out.
            absolute(60)
        }

        // Adds timestamps to the console log.
        timestamps()

        // Renders ANSI escape sequences, including color, to console output.
        colorizeOutput()
    }

    // Adds build steps to the jobs.
    steps {

        // Invokes a Maven build.
        maven {

            // Specifies the Maven installation for executing this step.
            mavenInstallation("apache-maven-${Constants.MAVEN_VERSION}")

            // Specifies the goals to execute including other command line options.
            goals("-B clean install")
        }
    }

    // Adds post-build actions to the job.
    publishers {

        //Archives artifacts with each build.
        archiveArtifacts("**target/*.hpi")

        // Publishes JUnit test result reports.
        archiveJunit('**/target/*-reports/TEST-*.xml') {

            // If set, does not fail the build on empty test results.
            allowEmptyResults()
        }
    }
}