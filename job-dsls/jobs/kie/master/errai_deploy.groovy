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
import org.kie.jenkins.jobdsl.templates.ErraiDeployJob

// Job parameters values
projectName = "errai"
labelName = "kie-rhel7&&kie-mem16g"
timeoutValue = 180
mavenGoals = "-B -e -fae -Dfull -Dmaven.test.failure.ignore=true -Pintegration-test clean deploy -Derrai.codegen.details=true"
branchName = Constants.BRANCH
githubGroup = "errai"

// Adds required folders
ErraiDeployJob.addFolders(this)

// Creates or updates a free style job.
def jobDefinition = job("KIE/${branchName}/" + Constants.DEPLOY_FOLDER + "/${projectName}")

ErraiDeployJob.addDeployConfiguration(job = jobDefinition,
        projectName = projectName,
        githubGroup = githubGroup,
        branchName = Constants.BRANCH,
        labelName = labelName,
        timeoutValue = timeoutValue,
        mavenGoals = mavenGoals
)