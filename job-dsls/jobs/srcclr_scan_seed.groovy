import org.kie.jenkins.jobdsl.Constants
import org.kie.jenkins.jobdsl.templates.BasicJob

String jobDescription = "Job responsible for SourceClear verification"

def jobDefinition = job("srcclr-scan") {


    String debug = config.debug?.toBoolean() ? '-d' : ''
    String scanType = config.scanType?.trim()
    String product = config.product?.trim() ?: ''
    String version = config.version?.trim() ?: ''
    String packageName = config.packageName ? "--package=${config.packageName.trim()}" : ''
    String url = config.url?.trim()
    String extra = config.extra ?: ''
    String recurse = config.recurse?.trim() ? '--recursive' : ''
    String scmVersionParam = ''


    label("kie-rhel7")

    wrappers {
        credentialsBinding {
            string("SRCCLR_API_TOKEN", "SRCCLR_API_TOKEN")
        }
    }

    steps {
        maven {
            goals("-version")
        }
    }


}

BasicJob.addCommonConfiguration(jobDefinition, jobDescription)