import org.kie.jenkins.jobdsl.Constants
import org.kie.jenkins.jobdsl.templates.BasicJob

// Job Description
String jobDescription = "Job responsible for building slave image"


String command = """#!/bin/bash +x
cd jenkins-slaves

export ANSIBLE_SCP_IF_SSH=y
/opt/packer/bin/packer build\\
 -var "openstack_endpoint=https://rhos-d.infra.prod.upshift.rdu2.redhat.com:13000/v3"\\
 -var "openstack_username=psi-rhba-jenkins"\\
 -var "openstack_password=\$PSI_PASSWORD"\\
 -var "image_name=kie-rhel7-\$BUILD_NUMBER"\\
 -var "ssh_private_key_file=\$PSI_PRIVATE_KEY"\\
 packer-kie-rhel7.json
"""

// Creates or updates a free style job.
def jobDefinition = job("slave-image-build") {

    // Allows a job to check out sources from an SCM provider.
    scm {

        // Adds a Git SCM source.
        git {

            // Specify the branches to examine for changes and to build.
            branch("master")

            // Adds a remote.
            remote {

                // Sets a remote URL for a GitHub repository.
                github("kiegroup/kie-jenkins-scripts")
            }
        }
    }

    // Adds pre/post actions to the job.
    wrappers {

        // Binds environment variables to credentials.
        credentialsBinding {

            // Sets a variable to the text given in the credentials.
            string("PSI_PASSWORD", "psi-rhba-jenkins-password")

            // Copies the file given in the credentials to a temporary location, then sets the variable to that location.
            file("PSI_PRIVATE_KEY", "kie-jenkins.pem")
        }
    }

    // Label which specifies which nodes this job can run on.
    label("kie-linux&&kie-mem512m")

    // Adds build steps to the jobs.
    steps {

        // Runs a shell script.
        shell(command)
    }
}

BasicJob.addCommonConfiguration(jobDefinition, jobDescription)
