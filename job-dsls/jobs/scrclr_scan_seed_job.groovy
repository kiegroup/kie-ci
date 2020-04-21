job('srcclr_scan_seed_job') {
    description('Scan job, which generates scanning jobs for upstream projects')
    parameters {
        stringParam('REPO_FILE_URL','https://raw.githubusercontent.com/kiegroup/droolsjbpm-build-bootstrap/master/script/repository-list.txt','URL of the repository-list.txt file')
        stringParam('KIE_JENKINS_SCRIPTS_REPO', 'https://github.com/kiegroup/kie-jenkins-scripts', '')
        stringParam('KIE_JENKINS_SCRIPTS_BRANCH', 'master', '')
        stringParam('JOB_PATH', '', '')
    }
    scm {
        git {
            remote {
                name('origin')
                url('$KIE_JENKINS_SCRIPTS_REPO')
            }
            branch("${KIE_JENKINS_SCRIPTS_BRANCH}")
        }
    }

    steps{
        shell('curl ${REPO_FILE_URL} -o repository-list.txt')
        dsl{
            external('job-dsls/jobs/srcclr_scan_job.groovy','job-dsls/jobs/srcclr_scan_pipeline.groovy')
        }
    }

}
