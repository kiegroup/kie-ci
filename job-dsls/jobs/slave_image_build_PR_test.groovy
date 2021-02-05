import org.kie.jenkins.jobdsl.Constants

// definition of parameters
def javadk=Constants.JDK_VERSION
def label_="kie-linux&&kie-mem512m"
def organization=Constants.GITHUB_ORG_UNIT

def jenkinsSlaves='''
#!/bin/bash +x
cd jenkins-slaves

# include functionality for osbs builds
# clone from gerrit moved to scm, not needed here: ./add-osbs.sh https://code.engineering.redhat.com/gerrit/bxms-jenkins
https://redhatbxms.slack.com/archives/CC7CY8GCT
rsync -av bxms-jenkins/jenkins-image-extra-bits/rhba-osbs/ansible/ ansible
rsync -av bxms-jenkins/jenkins-image-extra-bits/rhba-sourceclear-integration/ansible/ ansible

wget --no-check-certificate https://rhba-jenkins.rhev-ci-vms.eng.rdu2.redhat.com/userContent/packer
chmod u+x packer

export ANSIBLE_SCP_IF_SSH=y
./packer build\\
 -var "openstack_endpoint=https://rhos-d.infra.prod.upshift.rdu2.redhat.com:13000/v3"\\
 -var "openstack_username=psi-rhba-jenkins"\\
 -var "openstack_password=$PSI_PASSWORD"\\
 -var "image_name=kie-rhel7-PR-test-$BUILD_NUMBER"\\
 -var "ssh_private_key_file=$PSI_PRIVATE_KEY"\\
 -on-error=cleanup \\
 -var-file $PACKER_VAR_FILE \\
 packer-kie-rhel-jenkins-agent.json
'''

// Creation of folders where jobs are stored
folder("PROD")
def folderPath="PROD"

//job name
String jobName="$folderPath/slave-image-build-PR-test"

String OWNER=""
String BRANCH=""
String PACKER_VAR_FILE=""

job(jobName) {

    description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files.
                    |
                    |Job responsible for building slave image from a specified PR for testing purposes.
                    |
                    |""".stripMargin())

    parameters {
        stringParam ("OWNER","kiegroup","")
        stringParam ("BRANCH","master","")
        stringParam ("PACKER_VAR_FILE","packer-kie-rhel7-vars.json","")

    }

    label("${label_}")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    scm {
        git {
            remote {
                github("${organization}/kie-jenkins-scripts")
            }
            branch=("${BRANCH}")
        }
    }

    steps{
        shell(jenkinsSlaves)
    }
}