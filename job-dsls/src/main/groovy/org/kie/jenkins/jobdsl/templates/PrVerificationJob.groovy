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
import org.kie.jenkins.jobdsl.Constants;

/**
 * PR verification Job Template
 *
 */
class PrVerificationJob extends BasicJob {

    /**
     * Adds common PR verification configuration to the job
     *
     * @param job - Jenkins job object.
     * @param projectName - Project name that PR verification Job is run against
     * @param labelName - Jenkins slave nodes label name
     * @param timeoutValue - Job timeout value in minutes
     */
    static void addPrConfiguration(Job job, String projectName, String labelName, int timeoutValue) {

        //Add common configuration to the job
        String description = String.format("Pull Request Verification job for %s project.", projectName);
        addCommonConfiguration(job, description)

        //Add PR configuration
        job.with {

            // Name of the JDK installation to use for this job.
            jdk(Constants.JDK_VERSION)

            // Label which specifies which nodes this job can run on.
            label(labelName)

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
                    absolute(timeoutValue)
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
    }
}