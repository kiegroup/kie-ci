/**
 * Creates provision-job in Jenkins/Tools/provisioning for
 * provision-smee-client
 */
def final DEFAULTS = [
        logRot : 10,
        labExp : "ansible",
        params : [ ],
        openJdk : "openjdk1.8_local"
]

def final JOB_NAMES = [
        "provision-smee-client"        : [
                params : [
                        [name: "IMAGE", default: "rhel-7.9-server-x86_64-released", description: "The name of the image to be used for machine creation." ],
                        [name: "FLAVOUR", default: "ci.m1.medium", description: "The flavor (i.e. resources such as CPU cores, RAM, ...) defining the machine. m1.medium = 2 vCPUs, 4 GB RAM, 40 GB HDD" ],
                        [name: "WEBHOOK_URL", default: "https://smee.io/qy64t6RpCcqIFR3", description: ""],
                        [name: "TARGET_URL", default: "https://eng-jenkins-csb-business-automation.apps.ocp4.prod.psi.redhat.com/ghprbhook/", description: ""]
                ],
                jobDescription : "The job for provisioning smee client node."
        ]
]

// create needed folder(s) for where the jobs are created
folder("Tools")
folder("Tools/Provisioning")

def folderPath = "Tools/Provisioning"

for (jobNames in JOB_NAMES) {
    Closure<Object> get = { String key -> jobNames.value[key] ?: DEFAULTS[key] }

    String jobName = jobNames.key
    String labExp = get("labExp")
    String openJdk = get("openJdk")
    String jobDescription = get("jobDescription")
    def params = get("params")
    def logRot = get("logRot")

    // jobs for main branch don't use the branch in the name
    String jobN = "$folderPath/$jobName"

    job(jobN) {
        description(jobDescription)

        logRotator {
            numToKeep(logRot)
        }

        label(labExp)

        jdk(openJdk)

        for ( param in params ) {
            parameters {
                stringParam(param.name, param.default, param.description)
            }
        }

        // Allows a job to check out sources from an SCM provider.
        scm {
            git {
                remote {
                    url('git@github.com:kiegroup/kie-jenkins-scripts.git')
                    credentials("kie-qe-ci-user-key")
                    branch('main')
                }
                extensions {
                    relativeTargetDirectory {
                        // Specify a local directory (relative to the workspace root) where the Git repository will be checked out.
                        relativeTargetDir('kie-jenkins-scripts')
                    }
                }
            }
        }

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
            }

            // Loads file provider
            configFileProvider {
                managedFiles {
                    configFile {
                        fileId('jenkins.plugins.openstack.compute.UserDataConfig.1454073256508')
                        targetLocation('\$WORKSPACE/rhel-cloud-init')
                        variable('CLOUD_INIT')
                    }
                    configFile {
                        fileId('openstack-upshift-rc-file')
                        targetLocation('\$WORKSPACE/openrc.sh')
                        variable('OPENRC_FILE')
                    }
                }
            }

            environmentVariables {
                // Adds an environment variable to the build.
                env("INVENTORY_FILE", "\$WORKSPACE/cekit-cacher-inventory.txt")
                env("MACHINES_FILE","\$WORKSPACE/machines.txt")
                env("IP_FILE","\$WORKSPACE/ips.txt")
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
            // Archives artifacts with each build.
            archiveArtifacts('*.txt')
            wsCleanup()
        }

        // Adds step that executes a shell script
        steps {
            shell(getScripts())
            shell(getScripts_II())
        }
    }
}

String getScripts() {
    return "#!/bin/bash\n" +
            "\n" +
            ". \$OPENRC_FILE\n" +
            "# OS_PASSWORD gets injected from global passwords\n" +
            "\n" +
            "function check_machine {\n" +
            "  local FULL_NAME=\$1\n" +
            "\n" +
            "  openstack server list | grep \"\$FULL_NAME\"\n" +
            "  if [ \$? -ne 1 ]; then\n" +
            "    echo \"An instance with name \$FULL_NAME already exists, please tear it down before running this job.\"\n" +
            "    exit 1\n" +
            "  fi\n" +
            "}\n" +
            "\n" +
            "function provision_machine {\n" +
            "  local name=\$1\n" +
            "  local VOLUME_ID=\$2\n" +
            "  local FULL_NAME=\"\$name\"\n" +
            "\n" +
            "  check_machine \"\$FULL_NAME\"\n" +
            "  \n" +
            "  # boot new machine and wait a while for it to start (to get the network)\n" +
            "  openstack server create --wait --flavor \$FLAVOUR --image \$IMAGE --key-name \"\$OS_KEY_NAME\" --nic net-id=\$OS_NETWORK_ID --user-data \"\$CLOUD_INIT\" \$FULL_NAME > boot.log\n" +
            "  echo \$FULL_NAME >> \$MACHINES_FILE\n" +
            "  \n" +
            "  export NETWORKS=`openstack server list -f value -c Networks --name \$FULL_NAME`\n" +
            "  export INSTANCE_IP=\${NETWORKS#*=}\n" +
            "\n" +
            "  echo \$INSTANCE_IP >> \$IP_FILE\n" +
            "}\n" +
            "\n" +
            "\n" +
            "provision_machine \"smee-client-eng-jenkins-\$BUILD_ID\"\n" +
            "echo \"[smee-client]\" > \$INVENTORY_FILE\n" +
            "echo \"\$INSTANCE_IP\" >> \$INVENTORY_FILE\n" +
            "\n" +
            "\n" +
            "sleep 120"
}

String getScripts_II() {
    return "ansible smee-client -u root -i \$INVENTORY_FILE -m ping\n" +
            "\n" +
            "pushd kie-jenkins-scripts/jenkins-slaves/ansible\n" +
            "ansible-playbook -i \$INVENTORY_FILE smee-client.yml -e WEBHOOK_URL=\$WEBHOOK_URL -e TARGET_URL=\$TARGET_URL\n" +
            "popd"
}
