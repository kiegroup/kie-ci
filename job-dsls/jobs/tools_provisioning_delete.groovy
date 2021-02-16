/**
 * Creates all detele-jobs in Jenkins/Tools/provisioning
 * delete-cekit-cacher
 * delete-docker-registry
 * delete-smee-client
 * delete-verdaccio-service
 */

import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        desCr : "",
        shScr : "",
        jobAbr : "",
        folderPath : "Provisioning",
        logRot : 10,
        labExp : "ansible",
        timeOutVar : 30,
        param : "",
        paramDescription: ""
]
def final JOB_NAMES = [
        "delete-cekit-cacher"       : [
                jobAbr: "CekitCacher"
        ],
        "delete-docker-registry"    : [
                jobAbr: "DockerReg"
        ],
        "delete-smee-client"        : [
                jobAbr: "SmeeClient",
                param : "SMEE_NUMBER",
                paramDescription : "The number related with the BUILD_ID from provision-smee-client job when the machine was provisioned. 14 for instance"
        ],
        "delete-verdaccio-service"  : [
                jobAbr: "VerdaccioServ",
                param: "INSTANCE_NUMBER",
                paramDescription: "The number related with the BUILD_ID from provision-verdaccio-service job when the machine was provisioned. 14 for instance"
        ]
]

//create folders
folder("Provisioning")

for (jobNames in JOB_NAMES) {
    Closure<Object> get = { String key -> jobNames.value[key] ?: DEFAULTS[key] }

    String jobName = jobNames.key
    String folderPath = get("folderPath")
    String desCr = get("desCr")
    String shSrc = get("shScr")
    String jobAbr = get("jobAbr")
    String labExp = get("labExp")
    String param = get("param")
    String paramDesc = get("paramDescription")
    def logRot = get("logRot")
    def timeOutVar = get("timeOutVar")

    // jobs for master branch don't use the branch in the name
    String jobN = "$folderPath/$jobName"

    job(jobN) {
        description(getDescription(jobAbr))

        logRotator {
            numToKeep(logRot)
        }

        label(labExp)

        if ( param != "") {
            parameters {
                stringParam ("${param}","","${paramDesc}")
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
            return "Destroys verdaccio-service machine from PSI (Upshift) OpenStack.\n" +
                    "<br>Do NOT run unless you know what you are doing!"
        case "DockerReg":
            return "Destroys local docker-registry machine from PSI (Upshift) OpenStack.\n" +
                    "Do NOT run unless you know what you are doing!"
        case "SmeeClient":
            return "Destroys smee-client machine from PSI (Upshift) OpenStack. \n" +
                    "Do NOT run unless you know what you are doing!"
        default:
            return "Destroys cekit-cacher machine from PSI (Upshift) OpenStack. \n" +
                    "Do NOT run unless you know what you are doing!"
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
