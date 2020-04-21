def SRCCLR_FOLDER = 'custom/akoufoud/srcclr'

folder('custom/akoufoud/srcclr')

job('srcclr_scan_seed_job') {
    description('Scan job, which generates scanning jobs for upstream projects')
    parameters {
        stringParam('REPO_FILE_URL','https://raw.githubusercontent.com/kiegroup/droolsjbpm-build-bootstrap/master/script/repository-list.txt','URL of the repository-list.txt file')
    }
    scm {
        git {
            remote {
                name('origin')
                url('https://github.com/akoufoudakis/kie-jenkins-scripts.git')
            }
            branch('BXMSPROD-533')
        }
    }

    steps{
        shell('curl ${REPO_FILE_URL} -o repository-list.txt')
        jobDsl {
            targets("job-dsls/jobs/**/srcclr_scan_job.groovy\n" +
                    "job-dsls/jobs/**/srcclr_scan_pipeline.groovy")
            useScriptText(false)
            sandbox(false)
            ignoreExisting(false)
            ignoreMissingFiles(false)
            failOnMissingPlugin(true)
            unstableOnDeprecation(true)
            removedJobAction('DELETE')
            removedViewAction('DELETE')
            lookupStrategy('SEED_JOB')
            additionalClasspath("job-dsls/src/main/groovy")
        }
    }

}
