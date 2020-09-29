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
import org.kie.jenkins.jobdsl.templates.PrVerificationJob

// Job parameters values
projectName = "kie-docs"
labelName = "kie-rhel7&&kie-mem4g"
timeoutValue = 60
mavenGoals = "-B clean install"
branchName = Constants.BRANCH

// Adds required folders
PrVerificationJob.addFolders(this)

// Creates or updates a free style job.
def jobDefinition = job(Constants.PULL_REQUEST_FOLDER + "/${projectName}-${branchName}.pullrequests")

PrVerificationJob.addPrConfiguration(job = jobDefinition,
        projectName = projectName,
        githubGroup = Constants.GITHUB_ORG_UNIT,
        githubCredentialsId = "",
        branchName = Constants.BRANCH,
        labelName = labelName,
        timeoutValue = timeoutValue,
        mavenGoals = mavenGoals,
        archiveArtifactsPattern = "**/target/generated-docs/html_single/**/*"
)