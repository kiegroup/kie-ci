import org.kie.jenkins.jobdsl.Constants

CURRENT_IMAGE_NAMES = [
        "kie-rhel7",
        "kie-rhel8"
]

RHOS_D_CLOUD_RC_FILE = '$WORKSPACE/rhos-d-rc.sh'
RHOS_01_CLOUD_RC_FILE = '$WORKSPACE/rhos-01-rc.sh'

// Job Description
jobDescription = 'Promotes Jenkins agent OpenStack images (renames the LATEST variant of the image to FALLBACK and the NEW variant of the image to LATEST). Do NOT run unless you know what you are doing!'
labelExp = 'ansible'

promoteScript = this.getClass().getResource("job-scripts/promoteImage.sh").text

folder("Tools")
folder("Tools/Images")
def path = "Tools/Images"

job("$path/agent-image-promote") {
    parameters {
        choiceParam('IMAGE_NAME',
                CURRENT_IMAGE_NAMES,
                "The name of the image to be promoted.")
        choiceParam('CLOUD',
                [ RHOS_D_CLOUD_RC_FILE, RHOS_01_CLOUD_RC_FILE ],
                "The injected variable with RC file of the cloud where to promote the image in.")
    }
    configureAgentImagePromoteJob(delegate)
}

matrixJob("$path/agent-image-promote-matrix") {
    parameters {
        choiceParam('CLOUD',
                [ RHOS_D_CLOUD_RC_FILE, RHOS_01_CLOUD_RC_FILE ],
                "The injected variable with RC file of the cloud where to promote the images in.")
    }
    axes {
        labelExpression('label_exp', labelExp)
        text('IMAGE_NAME', CURRENT_IMAGE_NAMES)
    }
    childCustomWorkspace(Constants.MATRIX_SHORT_CHILD_WORKSPACE)
    configureAgentImagePromoteJob(delegate)
}

def configureAgentImagePromoteJob(def job) {
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
            shell(promoteScript)
        }
    }
}
