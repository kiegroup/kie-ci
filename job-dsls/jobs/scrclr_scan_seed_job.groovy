job('srcclr_scan_seed_job') {
    description('Scan job, which generates scanning jobs for upstream projects')
    parameters {
        stringParam('REPO_FILE_URL','https://raw.githubusercontent.com/akoufoudakis/droolsjbpm-build-bootstrap/BXMSPROD-533/script/repository-list.txt','URL of the rpository-list.txt file')
        stringParam('KIE_JENKINS_SCRIPTS_REPO', 'https://github.com/akoufoudakis/kie-jenkins-scripts.git', '')
        stringParam('KIE_JENKINS_SCRIPTS_BRANCH', 'master', '')
    }
    scm {
        git {
            remote {
                name('origin')
                url("${KIE_JENKINS_SCRIPTS_REPO}")
            }
            branch("${KIE_JENKINS_SCRIPTS_BRANCH}")
        }
    }

    steps{
        shell('curl ${REPO_FILE_URL} -o repository-list.txt')
        dsl{
            external('srcclr_scan_job.groovy','srcclr_scan_pipeline.groovy')
        }
    }

}
