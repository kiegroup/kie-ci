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

import org.kie.jenkins.jobdsl.Constants
import org.kie.jenkins.jobdsl.templates.BasicJob
import org.kie.jenkins.jobdsl.templates.PrVerificationJob

// Job parameters values
projectName = "kie-build-helper-jenkins-plugin"
labelName = "rhel7&&mem4g"
timeoutValue = 60

// Creates or updates a free style job.
def jobDefinition = job("kie-build-helper-jenkins-plugin-pullrequests") {

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
}

PrVerificationJob.addPrConfiguration(jobDefinition, projectName, labelName, timeoutValue)