import org.kie.jenkins.jobdsl.Constants

// definition of parameters
def javadk=Constants.JDK_TOOL
def label_="kie-linux&&kie-mem512m"
def organization=Constants.GITHUB_ORG_UNIT
def packerUrl=Constants.PACKER_URL

def jenkinsSlaves="""
#!/bin/bash +x
cd jenkins-agents

# include functionality for osbs builds
# clone from gerrit moved to scm, not needed here: ./add-osbs.sh https://code.engineering.redhat.com/gerrit/bxms-jenkins
rsync -av bxms-jenkins/jenkins-image-extra-bits/rhba-osbs/ansible/ ansible
rsync -av bxms-jenkins/jenkins-image-extra-bits/rhba-sourceclear-integration/ansible/ ansible

wget $packerUrl -O packer.zip
unzip packer.zip
chmod u+x packer

export ANSIBLE_SCP_IF_SSH=y
./packer build\\
 -var "openstack_endpoint=https://rhos-d.infra.prod.upshift.rdu2.redhat.com:13000/v3"\\
 -var "openstack_username=psi-rhba-jenkins"\\
 -var "openstack_password=\$PSI_PASSWORD"\\
 -var "image_name=\$TARGET_IMAGE_NAME"\\
 -var "ssh_private_key_file=\$PSI_PRIVATE_KEY"\\
 -on-error=cleanup \\
 -var-file \$PACKER_VAR_FILE \\
 packer-kie-rhel-jenkins-agent.json
"""

// Creation of folders where jobs are stored
folder("Tools")
folder("Tools/Images")
def folderPath = "Tools/Images"

//job name
String jobName="$folderPath/agent-image-build-PR-test"

String OWNER=""
String BRANCH=""
String PACKER_VAR_FILE=""

job(jobName) {

    description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files.
                    |
                    |Job responsible for building agent image from a specified PR for testing purposes.
                    |
                    |""".stripMargin())

    parameters {
        stringParam ("OWNER","kiegroup","")
        stringParam ("BRANCH","main","")
        stringParam ("BXMS_JENKINS_BRANCH","master","")
        choiceParam('BASE_RHEL_VERSION', ['7', '8'], 'RHEL version of the base image to be used as the source image.')
        stringParam ("TARGET_IMAGE_NAME","kie-rhel\${BASE_RHEL_VERSION}-PR-test","Keep the default to have it pre-assigned to image-test or rhel8-image-test labels, or override and assign by yourself")
    }

    label("${label_}")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    multiscm {
        git {
            remote {
                github("${organization}/kie-jenkins-scripts")
                url("https://github.com/\${OWNER}/kie-jenkins-scripts")
            }
            branch ("\${BRANCH}")
            browser{
                githubWeb {
                    repoUrl("https://github.com/\${OWNER}/kie-jenkins-scripts/")
                }
            }
        }
        git {
            remote {
                url('ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com/bxms-jenkins')
                credentials('code.engineering.redhat.com')
            }
            branch ('${BXMS_JENKINS_BRANCH}')
            extensions {
                relativeTargetDirectory {
                    relativeTargetDir('jenkins-agents/bxms-jenkins')
                }
            }
        }
    }

    wrappers {
        preBuildCleanup()
        credentialsBinding {
            string {
                variable('PSI_PASSWORD')
                credentialsId('psi-rhba-jenkins-password')
            }
            sshUserPrivateKey {
                keyFileVariable('PSI_PRIVATE_KEY')
                credentialsId('kie-jenkins.pem')
                usernameVariable('PSI_PRIVATE_KEY_USERNAME')
            }
        }

        environmentVariables {
            env("PACKER_VAR_FILE", 'packer-kie-rhel${BASE_RHEL_VERSION}-vars.json')
        }

        colorizeOutput()
        timestamps()
    }

    publishers {
        wsCleanup()
    }

    steps{
        shell(jenkinsSlaves)
    }
}