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

import javaposse.jobdsl.dsl.Job;

/**
 * Basic job template.
 *
 */
class BasicJob {

    /**
     * Adds common configuration to Jenkins job.
     *
     * @param job - Jenkins job object.
     * @param description - Jenkins job description.
     */
    static void addCommonConfiguration(Job job, String jobDescription) {

        job.with {

            // Sets a description for the job.
            description(jobDescription)

            // Adds pre/post actions to the job.
            wrappers {

                // Adds timestamps to the console log.
                timestamps()

                // Renders ANSI escape sequences, including color, to console output.
                colorizeOutput()

                // Deletes files from the workspace before the build starts.
                preBuildCleanup()

            }

            // Manages how long to keep records of the builds.
            logRotator {

                // If specified, only up to this number of build records are kept.
                numToKeep(25)

                // If specified, only up to this number of builds have their artifacts retained.
                artifactNumToKeep(2)
            }

            // Adds post-build actions to the job.
            publishers {

                // Deletes files from the workspace after the build completed.
                wsCleanup {}
            }
        }
    }
}
