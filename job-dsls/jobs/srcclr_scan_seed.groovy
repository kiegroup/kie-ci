import org.kie.jenkins.jobdsl.Constants
import org.kie.jenkins.jobdsl.templates.BasicJob

String jobDescription = "Job responsible for SourceClear verification"

def jobDefinition = job("srcclr-scan") {

    label("kie-rhel7")

    wrappers {
        credentialsBinding {
            string("SRCCLR_API_TOKEN", "SRCCLR_API_TOKEN")
        }
    }

    steps {
        sh "mvn -version"
    }


}

BasicJob.addCommonConfiguration(jobDefinition, jobDescription)