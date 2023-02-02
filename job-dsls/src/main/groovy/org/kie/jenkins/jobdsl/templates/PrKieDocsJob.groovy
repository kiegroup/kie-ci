/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.jenkins.jobdsl.templates

import javaposse.jobdsl.dsl.Job
import org.kie.jenkins.jobdsl.Constants

/**
 * PR verification Job Template
 *
 */
class PrKieDocsJob extends BasicJob {

    /**
     * Adds folder structure for PR job.
     *
     * @param context - Jenkins DLS context.
     */
    static void addFolders(context) {
        context.with {

            // Creation of folders where jobs are stored
            folder("KIE")
            folder("KIE/${branchName}")
            folder("KIE/${branchName}/" + Constants.PULL_REQUEST_FOLDER){
                displayName(Constants.PULL_REQUEST_FOLDER_DISPLAY_NAME)
            }
        }
    }

    /**
     * Adds common PR verification configuration to the job.
     *
     * @param job - Jenkins job object.
     * @param projectName - Project name that PR verification Job is run against
     * @param githubGroup - GitHub group name
     * @param githubCredentialsId - GitHub credentials id.
     * @param branchName - Branch name for PR job (default is ${sha1})
     * @param labelName - Jenkins agent nodes label name
     * @param timeoutValue - Job timeout value in minutes
     * @param mavenGoals - Build maven goals
     * @param archiveArtifactsPattern - regexp that matches artifacts to archive
     */
    static void addPrConfiguration(Job job,
            String projectName,
            String githubGroup,
            String githubCredentialsId,
            String branchName,
            String labelName,
            int timeoutValue,
            String mavenGoals,
            String archiveArtifactsPattern = null) {

        //Add common configuration to the job
        String description = "Pull Request Verification job for ${projectName} project."
        addCommonConfiguration(job, description)

        //Add PR configuration
        job.with {

            // Name of the JDK installation to use for this job.
            jdk(Constants.JDK_TOOL)

            // Label which specifies which nodes this job can run on.
            label(labelName)

            // Allows to parameterize the job.
            parameters {
                stringParam("sha1")
            }

            logRotator {
                numToKeep(10)
                daysToKeep(20)
            }

            // Allows Jenkins to schedule and execute multiple builds concurrently.
            concurrentBuild()

            // Allows a job to check out sources from an SCM provider.
            scm {

                // Adds a Git SCM source.
                git {

                    // Specify the branches to examine for changes and to build.
                    branch("\${sha1}")

                    // Adds a remote.
                    remote {

                        // Sets a remote URL for a GitHub repository.
                        github("${githubGroup}/${projectName}")

                        // Set credentials if passed.
                        if (githubCredentialsId != "") {
                            // Sets credentials for authentication with the remote repository.
                            credentials(githubCredentialsId)
                        }

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
                            reference("/home/jenkins/git-repos/${projectName}.git")
                        }
                    }
                }
            }

            // Adds build triggers to the job.
            triggers {

                // This plugin builds pull requests in github and report results.
                githubPullRequest {

                    // List of organizations. Their members will be whitelisted.
                    orgWhitelist(["kiegroup", "jboss-integration"])

                    // Use this option to allow members of whitelisted organisations to behave like admins, i.e. whitelist users and trigger pull request testing.
                    allowMembersOfWhitelistedOrgsAsAdmin(true)

                    //not cron - this is important
                    cron("")

                    // This field determines if webhooks are used
                    useGitHubHooks(true)

                    // Adding branches to this whitelist allows you to selectively test pull requests destined for these branches only.
                    // Supports regular expressions (e.g. 'main', 'feature-.*').
                    if (projectName == "kie-docs") {
                        whiteListTargetBranches([branchName, "main-kogito"])
                    } else {
                        whiteListTargetBranches([branchName])
                    }

                    // trigger phrase for re-triggering the job
                    triggerPhrase(".*[j|J]enkins,?.*(retest|test).*")
                    
                    extensions {

                        // Update commit status during build.
                        commitStatus {

                            // A string label to differentiate this status from the status of other systems. Default: "default"
                            context('Linux - Pull Request')

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
                    absolute(timeoutValue)
                }
            }

            // Adds build steps to the jobs.
            steps {

                // Invokes a Maven build.
                maven {

                    // Specifies the Maven installation for executing this step.
                    mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")

                    // Specifies the goals to execute including other command line options.
                    goals(mavenGoals)
                }
            }

            steps {
                shell('zip -r kie-docs.zip doc-content/*/target/generated-docs/html_single/**/*')
            }

            // Adds post-build actions to the job.
            publishers {

                // Publishes JUnit test result reports.
                archiveJunit('**/target/*-reports/TEST-*.xml') {

                    // If set, does not fail the build on empty test results.
                    allowEmptyResults()
                }

                if (archiveArtifactsPattern != null) {
                    archiveArtifacts(archiveArtifactsPattern)
                }
            }

            // Adds authentication token id.
            configure { node ->
                node / 'triggers' / 'org.jenkinsci.plugins.ghprb.GhprbTrigger' <<
                        'gitHubAuthId'("kie-ci-token")
            }
        }
    }
}