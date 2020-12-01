import org.kie.jenkins.jobdsl.Constants
import org.kie.jenkins.jobdsl.templates.BasicJob

// Job Description
String jobDescription = "Job responsible for building Jenkins agent image"


String command = """#!/bin/bash +x
cd jenkins-slaves

# include functionality for osbs builds
# clone from gerrit moved to scm, not needed here: ./add-osbs.sh https://code.engineering.redhat.com/gerrit/bxms-jenkins

rsync -av bxms-jenkins/jenkins-image-extra-bits/rhba-osbs/ansible/ ansible
rsync -av bxms-jenkins/jenkins-image-extra-bits/rhba-sourceclear-integration/ansible/ ansible

wget --no-check-certificate https://rhba-jenkins.rhev-ci-vms.eng.rdu2.redhat.com/userContent/packer
chmod u+x packer

export ANSIBLE_SCP_IF_SSH=y
./packer build\\
 -var "openstack_endpoint=https://rhos-d.infra.prod.upshift.rdu2.redhat.com:13000/v3"\\
 -var "openstack_username=psi-rhba-jenkins"\\
 -var "openstack_password=\$PSI_PASSWORD"\\
 -var "image_name=kie-rhel7-with-osbs-\$BUILD_NUMBER"\\
 -var "ssh_private_key_file=\$PSI_PRIVATE_KEY"\\
 -on-error=cleanup \\
 -var-file \$PACKER_VAR_FILE \\
 packer-kie-rhel-jenkins-agent.json
"""

// Creates or updates a free style job.
def jobDefinition = job("slave-image-build") {

    parameters {
        choiceParam('PACKER_VAR_FILE', ['packer-kie-rhel7-vars.json', 'packer-kie-rhel8-vars.json'], 'The file defining variables specific for different RHEL versions.')
    }

    // Allows a job to check out sources from an SCM provider.
    multiscm {

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

        git {

            // Specify the branches to examine for changes and to build.
            branch("master")

            // Adds a remote.
            remote {
                // Sets a remote URL for a GitHub repository.
                url('ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com/bxms-jenkins')
                credentials('code.engineering.redhat.com')
            }
            extensions {
                relativeTargetDirectory('jenkins-slaves/bxms-jenkins')
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
            file("PSI_PRIVATE_KEY", "kie-jenkis.pem")
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
