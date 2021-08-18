/**
 * Creates all detele-jobs in Jenkins/Tools/provisioning
 * delete-cekit-cacher
 * delete-docker-registry
 * delete-smee-client
 * delete-verdaccio-service
 */

def final DEFAULTS = [
        jobAbr : "",
        logRot : 10,
        labExp : "ansible",
        timeOutVar : 30,
        param : "",
        paramDescription: "",
        jobDescription: ""
]
def final JOB_NAMES = [
        "delete-cekit-cacher"       : [
                jobAbr: "CekitCacher",
                jobDescription : "Destroys cekit-cacher machine from PSI (Upshift) OpenStack.<br>Do NOT run unless you know what you are doing!"
        ],
        "delete-docker-registry"    : [
                jobAbr: "DockerReg",
                jobDescription : "Destroys local docker-registry machine from PSI (Upshift) OpenStack.<br>Do NOT run unless you know what you are doing!"
        ],
        "delete-smee-client"        : [
                jobAbr: "SmeeClient",
                jobDescription : "Destroys smee-client machine from PSI (Upshift) OpenStack.<br>Do NOT run unless you know what you are doing!",
                param : "SMEE_NUMBER",
                paramDescription : "The number related with the BUILD_ID from provision-smee-client job when the machine was provisioned. 14 for instance"
        ],
        "delete-verdaccio-service"  : [
                jobAbr: "VerdaccioServ",
                jobDescription : "Destroys verdaccio-service machine from PSI (Upshift) OpenStack.<br>Do NOT run unless you know what you are doing!",
                param: "INSTANCE_NUMBER",
                paramDescription: "The number related with the BUILD_ID from provision-verdaccio-service job when the machine was provisioned. 14 for instance"
        ]
]

// create needed folder(s) for where the jobs are created
folder("Tools")
folder("Tools/Provisioning")

def folderPath = "Tools/Provisioning"

for (jobNames in JOB_NAMES) {
    Closure<Object> get = { String key -> jobNames.value[key] ?: DEFAULTS[key] }

    String jobName = jobNames.key
    String jobAbr = get("jobAbr")
    String labExp = get("labExp")
    String jobDescription = get('jobDescription')
    String param = get("param")
    String paramDesc = get("paramDescription")
    def logRot = get("logRot")
    def timeOutVar = get("timeOutVar")

    String fullName = getFullName(jobAbr)

    // jobs for main branch don't use the branch in the name
    String jobN = "$folderPath/$jobName"

    job(jobN) {

        description(jobDescription)

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
            shell(getScript(fullName))
        }
    }
}

def getFullName(String jobAbr) {
    switch (jobAbr) {
        case "VerdaccioServ":
            return "\"verdaccio-service-eng-jenkins-\$INSTANCE_NUMBER\""
        case "DockerReg":
            return "\"provisioner-job-docker-registry-eng-jenkins-server\""
        case "SmeeClient":
            return "\"smee-client-eng-jenkins-\$SMEE_NUMBER\""
        default:
            return "\"provisioner-job-cekit-cacher-eng-jenkins-server\""
    }
}

def getScript(String fullName) {
    return ". \$OPENRC_FILE\n" +
            "# OS_PASSWORD gets injected from global passwords\n" +
            "\n" +
            "export FULL_NAME=$fullName\n" +
            "\n" +
            "# delete the machine\n" +
            "openstack server delete \$FULL_NAME\n" +
            "sleep 10s\n"
}

