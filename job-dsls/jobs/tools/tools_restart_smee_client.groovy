/**
 * Creates job for restarting all smee clients in Jenkins/Tools/provisioning
 */
folder("Tools")
folder("Tools/Provisioning")

def folderPath = "Tools/Provisioning"

String labExp = "ansible"
String jobDescription = "The job for provisioning smee client node."
int buildsToKeep = 3

job(folderPath + '/smee-clients-restart') {
    description(jobDescription)

    logRotator {
        numToKeep(buildsToKeep)
    }

    label(labExp)

    // Adds pre/post actions to the job.
    wrappers {

        // Adds timestamps to the console log.
        timestamps()

        // Renders ANSI escape sequences, including color, to console output.
        colorizeOutput()

        // Deletes files from the workspace before the build starts.
        preBuildCleanup()

        // secret text
        credentialsBinding {
            usernamePassword {
                // Name of an environment variable to be set to the username during the build
                usernameVariable('os_user')
                // Name of an environment variable to be set to the password during the build.
                passwordVariable('PSI_OS_PASSWORD')
                // Credentials of an appropriate type to be set to the variable.
                credentialsId('upshift-openstack-credentials-v3')
            }

            sshUserPrivateKey {
                credentialsId('kie-jenkins.pem')
                keyFileVariable('PSI_PRIVATE_KEY')
                usernameVariable('PSI_PRIVATE_KEY_USERNAME')
            }

        }

        // Loads file provider
        configFileProvider {
            managedFiles {
                configFile {
                    fileId('openstack-upshift-rc-file')
                    targetLocation('\$WORKSPACE/openrc.sh')
                    variable('OPENRC_FILE')
                }
            }
        }

        // Injects passwords as environment variables into the job.
        injectPasswords {
            // Injects global passwords provided by Jenkins configuration.
            injectGlobalPasswords(true)
            // Masks passwords provided by build parameters.
            maskPasswordParameters(true)
        }
    }

    // Adds custom properties to the job.
    properties {

        // Allows to configure job ownership.
        ownership {

            // Sets the name of the primary owner of the job.
            primaryOwnerId("mbiarnes")

            // Adds additional users, who have ownership privileges.
            coOwnerIds("anstephe", "mnovotny", "almorale")
        }
    }

    // Adds publishrs o job
    publishers {
        wsCleanup()
    }

    // Adds step that executes a shell script
    steps {
        shell(getScript())
    }
}

String getScript() {
    return '''#!/bin/bash

. $OPENRC_FILE

function restart_one_smee_client {
  local IP=$1
  echo "Restarting smee-client $IP"
  ssh -n root@$IP 'systemctl restart smee; sleep 2; systemctl status smee'
}

# print smee clients info
echo "Looking for smee clients: "
openstack server list --name smee

# get IPs of all smee machines
NETWORKS=`openstack server list -f value -c Networks --name smee`
while IFS= read -r line; do
    INSTANCE_IP=${line#*=}
    restart_one_smee_client $INSTANCE_IP
done <<< "$NETWORKS"
'''
}
