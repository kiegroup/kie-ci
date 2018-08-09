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
projectName = "kie-build-helper-jenkins-plugin"
labelName = "rhel7&&mem4g"
timeoutValue = 60
mavenGoals = "-B clean install"

// Creates or updates a free style job.
def jobDefinition = job("${projectName}-pullrequests") {

    // Adds post-build actions to the job.
    publishers {

        //Archives artifacts with each build.
        archiveArtifacts("**target/*.hpi")

    }
}

PrVerificationJob.addPrConfiguration(job = jobDefinition,
        projectName = projectName,
        githubGroup = Constants.GITHUB_ORG_UNIT,
        githubCredentialsId = "",
        githubAuthTokenId = Constants.GITHUB_AUTH_TOKEN,
        branchName = Constants.BRANCH,
        labelName = labelName,
        timeoutValue = timeoutValue,
        mavenGoals = mavenGoals)