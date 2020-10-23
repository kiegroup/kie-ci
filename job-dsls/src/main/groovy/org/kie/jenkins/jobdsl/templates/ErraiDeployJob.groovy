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
 * Deploy Job Template for errai
 *
 */
class ErraiDeployJob extends BasicJob {

    /**
     * Adds folder structure for PR job.
     *
     * @param context - Jenkins DLS context.
     */
    static void addFolders(context) {
        context.with {

            // Creates or updates a folder.
            folder(Constants.DEPLOY_FOLDER) {
                displayName(Constants.DEPLOY_FOLDER)
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
     * @param labelName - Jenkins slave nodes label name
     * @param timeoutValue - Job timeout value in minutes
     * @param mavenGoals - Build maven goals
     */
    static void addDeployConfiguration(Job job,
                                   String projectName,
                                   String githubGroup,
                                   String branchName,
                                   String labelName,
                                   int timeoutValue,
                                   String mavenGoals) {

        //Add common configuration to the job
        String description = String.format("deploy job for ${projectName} ${branchName} project.")
        addCommonConfiguration(job, description)

        //Add PR configuration
        job.with {

            // Name of the JDK installation to use for this job.
            jdk(Constants.JDK_VERSION)

            // Label which specifies which nodes this job can run on.
            label(labelName)

            // specifies the builds that have to be stored
            logRotator {
                numToKeep(5)
            }

            // Allows a job to check out sources from an SCM provider.
            scm {
                git {
                    remote {
                        github("${githubGroup}/${projectName}")
                        branch "$branchName"
                    }
                    extensions {
                        cloneOptions {
                            // git repo cache which is present on the slaves
                            // it significantly reduces the clone time and also saves a lot of bandwidth
                            reference("/home/jenkins/git-repos/${projectName}.git")
                        }
                    }
                }
            }

            // Adds build triggers to the job.
            triggers {
                scm('H/10 * * * *')
            }

            // Adds pre/post actions to the job.

            wrappers {
                timeout {
                    elastic(150, 3, timeoutValue)
                }
                timestamps()
                colorizeOutput()
            }

            // Adds build steps to the jobs.
            steps {

                // Invokes a Maven build.
                maven {

                    // Specifies the Maven installation for executing this step.
                    mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")

                    // Specifies the goals to execute including other command line options.
                    goals(mavenGoals)

                    // Specifies the JVM Options
                    mavenOpts("-Xmx2500m -XX:+CMSClassUnloadingEnabled")

                    // Specifies the settings.xml to take
                    providedSettings("7774c60d-cab3-425a-9c3b-26653e5feba1")

                    // Specifies the path to the root pom
                    rootPOM("pom.xml")
                    
                }
            }

            // Adds post-build actions to the job.
            publishers {

                // Publishes JUnit test result reports.
                archiveJunit('**/target/*-reports/TEST-*.xml') {

                    // If set, does not fail the build on empty test results.
                    allowEmptyResults()
                }
            }
        }
    }
}