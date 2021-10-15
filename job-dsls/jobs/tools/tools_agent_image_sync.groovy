import org.kie.jenkins.jobdsl.templates.BasicJob
import org.kie.jenkins.jobdsl.Constants

CURRENT_IMAGE_NAMES = [
        "kie-rhel7-latest",
        "kie-rhel8-latest"
]

RHOS_D_CLOUD_RC_FILE = '$WORKSPACE/rhos-d-rc.sh'
RHOS_01_CLOUD_RC_FILE = '$WORKSPACE/rhos-01-rc.sh'

NODE_LABEL = 'kie-linux&&kie-mem512m'

// Job Description
String jobDescription = "Job responsible for syncing Jenkins agent image(s) between clouds"

SCRIPT_CONTENTS = this.getClass().getResource("job-scripts/images/copyImage.sh").text

// create needed folder(s) for where the jobs are created
def folderPath = "Tools/images"
folder(folderPath)

// Creates or updates a free style job.
def singleJobDefinition = job("${folderPath}/agent-image-sync") {

    parameters {
        choiceParam('IMAGE_NAME',
                CURRENT_IMAGE_NAMES,
                "The name of the image to be copied.")
        choiceParam('SOURCE_CLOUD',
                [RHOS_D_CLOUD_RC_FILE, RHOS_01_CLOUD_RC_FILE],
                "The injected variable with RC file of the cloud where to copy the image FROM.")
        choiceParam('TARGET_CLOUD',
                [RHOS_01_CLOUD_RC_FILE, RHOS_D_CLOUD_RC_FILE],
                "The injected variable with RC file of the cloud where to copy the image TO.")
    }

    configureAgentImageSyncJob(delegate)
}

def matrixJobDefinition = matrixJob("${folderPath}/agent-image-sync-matrix") {
    childCustomWorkspace(Constants.MATRIX_SHORT_CHILD_WORKSPACE)
    parameters {
        choiceParam('SOURCE_CLOUD',
                [ RHOS_D_CLOUD_RC_FILE, RHOS_01_CLOUD_RC_FILE ],
                "The injected variable with RC file of the cloud where to copy the images FROM.")
        choiceParam('TARGET_CLOUD',
                [ RHOS_01_CLOUD_RC_FILE, RHOS_D_CLOUD_RC_FILE ],
                "The injected variable with RC file of the cloud where to copy the images TO.")
    }
    axes {
        labelExpression('label_exp', NODE_LABEL)
        text('IMAGE_NAME', CURRENT_IMAGE_NAMES)
    }
    configureAgentImageSyncJob(delegate)
}

def configureAgentImageSyncJob(def job) {
    job.with {
        // Label which specifies which nodes this job can run on.
        label(NODE_LABEL)

        steps {
            shell(SCRIPT_CONTENTS)
        }

        wrappers {
            credentialsBinding {
                usernamePassword('PSI_OS_USERNAME', 'PSI_OS_PASSWORD', 'psi-os-password')
            }

            configFiles {
                file('openstack-upshift-rc-file') {
                    targetLocation(RHOS_D_CLOUD_RC_FILE)
                }
                file('openstack-rhos-01-rc-file') {
                    targetLocation(RHOS_01_CLOUD_RC_FILE)
                }
            }
        }
    }
}

BasicJob.addCommonConfiguration(singleJobDefinition, jobDescription)
BasicJob.addCommonConfiguration(matrixJobDefinition, jobDescription)
