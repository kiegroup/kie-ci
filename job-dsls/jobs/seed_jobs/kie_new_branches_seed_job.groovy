import org.kie.jenkins.jobdsl.Constants

// definition of parameters

def javaToolEnv="KIE_JDK1_8"
def baseBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def javadk=Constants.JDK_VERSION

// +++++++++++++++++++++++++++++++++++++++++++ create a seed job ++++++++++++++++++++++++++++++++++++++++++++++++++++

// creation of folder where this seed job should run
folder("KIE")
folder("KIE/${baseBranch}")
def folderPath = "KIE/${baseBranch}"

job("${folderPath}/a-seed-job-${baseBranch}") {

    description("this job creates all needed Jenkins jobs for the ${baseBranch}-branch ")

    label("kie-rhel7 && kie-mem8g")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    scm {
        git {
            remote {
                github("${organization}/kie-jenkins-scripts")
            }
            branch ("${baseBranch}")
        }
    }

    triggers {
        gitHubPushTrigger()
    }

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${javaToolEnv}")
        preBuildCleanup()
    }

    steps {
        jobDsl {
            targets("job-dsls/jobs/kie/main/pr_jobs.groovy\n" +
                    "job-dsls/jobs/kie/main/downstream_pr_jobs.groovy\n" +
                    "job-dsls/jobs/kie/main/compile_downstream_build.groovy\n" +
                    "job-dsls/jobs/kie/main/prod_projects_downstream_production.groovy \n" +
                    "job-dsls/jobs/kie/main/upstream.groovy\n" +
                    "job-dsls/jobs/kie/main/dailyBuild_pipeline.groovy\n" +
                    "job-dsls/jobs/kie/main/dailyBuild_prod_pipeline.groovy\n" +
                    "job-dsls/jobs/kie/main/deploy_jobs.groovy\n" +
                    "job-dsls/jobs/kie/seed-jobs/kie_jenkinsScripts_PR.groovy\n" +
                    "job-dsls/jobs/kie/main/kie_docs_pr.groovy\n" +
                    "job-dsls/jobs/kie/main/pr_droolsjbpm_tools.groovy\n" +
                    "job-dsls/jobs/kie/main/prodTag_pipeline.groovy\n" +
                    "job-dsls/jobs/kie/main/new_branches_seed_job.groovy\n" +
                    "job-dsls/jobs/kie/main/springboot_pr_job.groovy\n" +
                    "job-dsls/jobs/kie/main/srcclr_scan_pipeline.groovy\n" +
                    "job-dsls/jobs/kie/main/srcclr_scan_job.groovy\n" +
                    "job-dsls/jobs/kie/main/jenkins_shared_libs.groovy")
            useScriptText(false)
            sandbox(false)
            ignoreExisting(false)
            ignoreMissingFiles(false)
            failOnMissingPlugin(true)
            unstableOnDeprecation(true)
            removedJobAction('IGNORE')
            removedViewAction('IGNORE')
            removedConfigFilesAction('IGNORE')
            lookupStrategy('JENKINS_ROOT')
            additionalClasspath("job-dsls/src/main/groovy\n" +
                    "job-dsls/src/main/resources")
        }
    }
}