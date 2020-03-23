import org.kie.jenkins.jobdsl.Constants
import org.kie.jenkins.jobdsl.templates.BasicJob

String jobDescription = "Job responsible for SourceClear verification"

def jobDefinition = job("srcclr-scan") {


    parameters {
        choiceParam('SCAN_TYPE', ['scm', 'binary'])
        stringParam('URL','')
        stringParam('VERSION', '')
        stringParam('PACKAGE','')
        stringParam('NAME', '')
        stringParam('MVNPARAMS', '')
        choiceParam('PROCESSOR_TYPE', ['cve', 'cvss'])
        booleanParam('RECURSIVE', false)
        booleanParam('DEBUGGING', false)
        booleanParam('TRACING', false)
    }


    label("kie-rhel7")

    wrappers {
        credentialsBinding {
            string("SRCCLR_API_TOKEN", "SRCCLR_API_TOKEN")
        }
    }

    String params = $MVNPARAMS.trim()
    steps {
        shell('echo $params')
        maven {
            goals("-version")
        }
    }


}

BasicJob.addCommonConfiguration(jobDefinition, jobDescription)