import org.kie.jenkins.jobdsl.Constants

CURRENT_IMAGE_NAMES = [
        "kie-rhel7-latest",
        "kie-rhel8-latest"
]

ALL_IMAGE_NAMES = CURRENT_IMAGE_NAMES + [
        "kie-rhel7-fallback",
        "kie-rhel8-fallback"
]

RHOS_D_CLOUD_RC_FILE = '$WORKSPACE/rhos-d-rc.sh'
RHOS_01_CLOUD_RC_FILE = '$WORKSPACE/rhos-01-rc.sh'

// Job Description
jobDescription = 'Synces Jenkins agent OpenStack images between the clouds. Do NOT run unless you know what you are doing!'
labelExp = 'ansible'

copyScript = this.getClass().getResource("job-scripts/copyImage.sh").text

folder("Tools")
folder("Tools/Images")
def path = "Tools/Images"

job("$path/agent-image-sync") {
    parameters {
        choiceParam('IMAGE_NAME',
                ALL_IMAGE_NAMES,
                "The name of the image to be copied.")
        choiceParam('SOURCE_CLOUD',
                [ RHOS_D_CLOUD_RC_FILE, RHOS_01_CLOUD_RC_FILE ],
                "The injected variable with RC file of the cloud where to copy the image FROM.")
        choiceParam('TARGET_CLOUD',
                [ RHOS_01_CLOUD_RC_FILE, RHOS_D_CLOUD_RC_FILE ],
                "The injected variable with RC file of the cloud where to copy the image TO.")
    }
    configureAgentImageSyncJob(delegate)
}

matrixJob("$path/agent-image-sync-matrix") {
    parameters {
        choiceParam('SOURCE_CLOUD',
                [ RHOS_D_CLOUD_RC_FILE, RHOS_01_CLOUD_RC_FILE ],
                "The injected variable with RC file of the cloud where to copy the images FROM.")
        choiceParam('TARGET_CLOUD',
                [ RHOS_01_CLOUD_RC_FILE, RHOS_D_CLOUD_RC_FILE ],
                "The injected variable with RC file of the cloud where to copy the images TO.")
    }
    axes {
        labelExpression('label_exp', labelExp)
        text('IMAGE_NAME', CURRENT_IMAGE_NAMES)
    }
    childCustomWorkspace(Constants.MATRIX_SHORT_CHILD_WORKSPACE)
    configureAgentImageSyncJob(delegate)
}

def configureAgentImageSyncJob(def job) {
    job.with {
        description(jobDescription)
        label(labelExp)

        wrappers {
            timestamps()
            colorizeOutput()
            preBuildCleanup()

            configFiles {
                file('openstack-upshift-rc-file') {
                    targetLocation(RHOS_D_CLOUD_RC_FILE)
                }
                file('openstack-rhos-01-rc-file') {
                    targetLocation(RHOS_01_CLOUD_RC_FILE)
                }
            }

            credentialsBinding {
                usernamePassword {
                    // Name of an environment variable to be set to the username during the build
                    usernameVariable('os_user')
                    // Name of an environment variable to be set to the password during the build.
                    passwordVariable('PSI_OS_PASSWORD')
                    // Credentials of an appropriate type to be set to the variable.
                    credentialsId('upshift-openstack-credentials-v3')
                }
            }
        }

        logRotator {
            artifactNumToKeep(3)
        }

        properties {
            ownership {
                // Sets the name of the primary owner of the job.
                primaryOwnerId("mbiarnes")
                // Adds additional users, who have ownership privileges.
                coOwnerIds("anstephe", "mnovotny", "almorale")
            }
        }

        publishers {
            wsCleanup {}
        }

        steps {
            shell(copyScript)
        }
    }
}
