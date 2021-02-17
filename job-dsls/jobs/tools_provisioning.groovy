/**
 * Creates all provision-jobs in Jenkins/Tools/provisioning
 * provision-cekit-cacher
 * provision-docker-registry
 * provision-smee-client
 * provision-verdaccio-service
 */

import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        jobAbr : "",
        folderPath : "Provisioning",
        logRot : 10,
        labExp : "ansible",
        timeOutVar : 30,
        params : [ ],
        defaultValues : [ ],
        paramsDescription : [ ]
]

def final JOB_NAMES = [
        "provision-cekit-cacher"       : [
                jobAbr: "CekitCacher",
                params : [
                        "IMAGE",
                        "FLAVOUR",
                        "DDNS_HOSTNAME",
                        "DDNS_HASH"
                ],
                defaultValues : [
                        "bxms-packer-rhel7-snapshot-updated",
                        "m1.medium",
                        "ba-cekit-cacher",
                        "d1c2341602998809404776a93d354bd0"

                ],
                paramsDescription : [
                        "The name of the image to be used for machine creation.",
                        "The flavor (i.e. resources such as CPU cores, RAM, ...) defining the machine. m1.medium = 2 vCPUs, 4 GB RAM, 40 GB HDD",
                        "Hostname to use in DDNS service",
                        "Hash used as authorization for use of specified hostname."
                ]
        ],
        "provision-docker-registry"    : [
                jobAbr: "DockerReg"
        ],
        "provision-smee-client"        : [
                jobAbr: "SmeeClient"
         ],
        "provision-verdaccio-service"  : [
                jobAbr: "VerdaccioServ"
        ]
]

//create folders
folder("Provisioning")

for (jobNames in JOB_NAMES) {
    Closure<Object> get = { String key -> jobNames.value[key] ?: DEFAULTS[key] }

    String jobName = jobNames.key
    String folderPath = get("folderPath")
    String jobAbr = get("jobAbr")
    String labExp = get("labExp")
    list params = get("params")
    list paramsDesc = get("paramsDescription")
    list defaultValues = get("defaultValues")
    def logRot = get("logRot")
    def timeOutVar = get("timeOutVar")
    def listMax = params.size()

    // jobs for master branch don't use the branch in the name
    String jobN = "$folderPath/$jobName"

    job(jobN) {
        description(getDescription(jobAbr))

        logRotator {
            numToKeep(logRot)
        }

        label(labExp)

        if ( params != "") {
            for (i = 0; i <=listMax; i++ ) {
                String PARAM_NAME = params[i]
                String PARAM_DEFAULT = defaultValues[i]
                String PARAM_DESC = paramsDesc[i]
                parameters {
                    stringParam("${PARAM_NAME}", "${PARAM_DEFAULT}","${PARAM_DESC}")
                }
            }
        }

        // Adds pre/post actions to the job.
        wrappers {

            // Adds timestamps to the console log.
            timestamps()

            // Adds timeout
            timeout{
                absolute(timeOutVar)
            }

            // Renders ANSI escape sequences, including color, to console output.
            colorizeOutput()

            // Deletes files from the workspace before the build starts.
            preBuildCleanup()

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
            shell(getScripts(jobAbr))
        }
    }
}

// def of variables
String getDescription(String Abr){
    switch(Abr) {
        case "VerdaccioServ":
            return "Destroys verdaccio-service machine from PSI (Upshift) OpenStack.<br>Do NOT run unless you know what you are doing!"
        case "DockerReg":
            return "Destroys local docker-registry machine from PSI (Upshift) OpenStack.<br>Do NOT run unless you know what you are doing!"
        case "SmeeClient":
            return "Destroys smee-client machine from PSI (Upshift) OpenStack.<br>Do NOT run unless you know what you are doing!"
        default:
            return "Destroys cekit-cacher machine from PSI (Upshift) OpenStack.<br>Do NOT run unless you know what you are doing!"
    }
}

String getScripts(String Abr) {
    switch(Abr) {
        case "VerdaccioServ":
            return ". \$OPENRC_FILE\n" +
                    "# OS_PASSWORD gets injected from global passwords\n" +
                    "\n" +
                    "export FULL_NAME=\"verdaccio-service-\$INSTANCE_NUMBER\"\n" +
                    "\n" +
                    "# delete the machine\n" +
                    "openstack server delete \$FULL_NAME\n" +
                    "sleep 10s\n"
        case "DockerReg":
            return ". \$OPENRC_FILE\n" +
                    "# OS_PASSWORD gets injected from global passwords\n" +
                    "\n" +
                    "export FULL_NAME=\"provisioner-job-docker-registry-server\"\n" +
                    "\n" +
                    "# delete the machine\n" +
                    "openstack server delete \$FULL_NAME\n" +
                    "sleep 10s\n"
        case "SmeeClient":
            return ". \$OPENRC_FILE\n" +
                    "# OS_PASSWORD gets injected from global passwords\n" +
                    "\n" +
                    "export FULL_NAME=\"smee-client-\$SMEE_NUMBER\"\n" +
                    "\n" +
                    "# delete the machine\n" +
                    "openstack server delete \$FULL_NAME\n" +
                    "sleep 10s"
        default:
            return ". \$OPENRC_FILE\n" +
                    "# OS_PASSWORD gets injected from global passwords\n" +
                    "\n" +
                    "export FULL_NAME=\"provisioner-job-cekit-cacher-server\"\n" +
                    "\n" +
                    "# delete the machine\n" +
                    "openstack server delete \$FULL_NAME\n" +
                    "sleep 10s"
    }
}
