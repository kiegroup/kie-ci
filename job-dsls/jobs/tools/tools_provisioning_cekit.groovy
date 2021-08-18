/**
 * Creates provision-job in Jenkins/Tools/provisioning
 * provision-cekit-cacher
 */
def final DEFAULTS = [
        logRot : 10,
        labExp : "ansible",
        params : [ ],
        openJdk : "openjdk1.8_local"
]

def final JOB_NAMES = [
        "provision-cekit-cacher"       : [
                params : [
                        [name: "IMAGE", default: "bxms-packer-rhel7-snapshot-updated", description: "The name of the image to be used for machine creation." ],
                        [name: "FLAVOUR", default: "m1.medium", description: "The flavor (i.e. resources such as CPU cores, RAM, ...) defining the machine. m1.medium = 2 vCPUs, 4 GB RAM, 40 GB HDD" ],
                        [name: "DDNS_HOSTNAME", default: "ba-cekit-cacher", description: "The name of the image to be used for machine creation." ],
                        [name: "DDNS_HASH", default: "d1c2341602998809404776a93d354bd0", description: "The name of the image to be used for machine creation." ],
                        [name: "CEKIT_CACHER_STORAGE_VOLUME_ID",default: "f9f618ac-be95-498d-b817-2ba25c53b313",description: "The OpenStack volume ID containing cekit-cacher data. Must already exist in PSI OpenStack."],
                        [name: "CEKIT_CACHER_STORAGE_VOLUME_UUID",default: "70adb8f7-7643-4bc9-b47d-738e9ca49044",description: "The OpenStack volume partition UUID used for mounting the attached volume. Must already exist on the volume."]
                ],
                jobDescription : "The job for provisioning cekit-cacher node."
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
                    url('git@github.com:jboss-integration/bxms-central-ci.git')
                    credentials("kie-qe-ci-user-key")
                    branch('main')
                }
                extensions {
                    relativeTargetDirectory {
                        // Specify a local directory (relative to the workspace root) where the Git repository will be checked out.
                        relativeTargetDir('bxms-central-ci')
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

            // Deletes files from the workspace before the build starts.
            preBuildCleanup()

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
            "  local FULL_NAME=\"provisioner-job-cekit-cacher-eng-jenkins-\$name\"\n" +
            "\n" +
            "  check_machine \"\$FULL_NAME\"\n" +
            "  \n" +
            "  # boot new machine and wait a while for it to start (to get the network)\n" +
            "  openstack server create --wait --flavor \$FLAVOR --image \$IMAGE --key-name \"\$OS_KEY_NAME\" --nic net-id=\$OS_NETWORK_ID --user-data \"\$CLOUD_INIT\" \$FULL_NAME > boot.log\n" +
            "  echo \$FULL_NAME >> \$MACHINES_FILE\n" +
            "  \n" +
            "  export NETWORKS=`openstack server list -f value -c Networks --name \$FULL_NAME`\n" +
            "  export INSTANCE_IP=\${NETWORKS # *=}\n" +
            "\n" +
            "  # attach volume\n" +
            "  openstack server add volume \"\$FULL_NAME\" \"\$CEKIT_CACHER_STORAGE_VOLUME_ID\"\n" +
            "  \n" +
            "  echo \$INSTANCE_IP >> \$IP_FILE\n" +
            "}\n" +
            "\n" +
            "\n" +
            "provision_machine 'server'"
}

String getScripts_II() {
    return "ansible cekit-cacher -u root -i \$INVENTORY_FILE -m ping\n" +
            "\n" +
            "pushd bxms-central-ci/cekit-cacher\n" +
            "ansible-playbook -i \$INVENTORY_FILE configure-common.yaml\n" +
            "ansible-playbook -i \$INVENTORY_FILE configure-storage.yaml\n" +
            "ansible-playbook -i \$INVENTORY_FILE configure-ddns.yaml\n" +
            "ansible-playbook -i \$INVENTORY_FILE configure-cekit-cacher.yaml\n" +
            "popd"
}
