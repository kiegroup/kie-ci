/**
 * Creates job that triggers when a new `<MAJOR>-<MINOR>-<PATCH>-prerelease` branch is
   pushed to kie-tools repository and notifies QE of this by sending and UMB message.
 */
import org.kie.jenkins.jobdsl.Constants

def repo = "kie-tools"
def repoBranch = ''
def ghOrgUnit = "kiegroup"
def ghAuthTokenId = "kie-ci"
def labelName = "kie-rhel7"
def regexpFilterRegexValue = '([0-9]+)\\.([0-9]+)\\.([0-9]+)-prerelease'

// creation of folder
folder ("KIE")
folder ("KIE/kogito")
folder ("KIE/kogito/kie-tools")

def folderPath="KIE/kogito/kie-tools"

String jobName = "${folderPath}/${repo}-prerelease-branch-UMB-trigger"

pipelineJob(jobName) {
 parameters {
  stringParam('ref', '')
  stringParam('ref_type', '')
  stringParam('x_github_event', '')
 }

 properties {
  pipelineTriggers {
   triggers {
    GenericTrigger {
     genericVariables {
      genericVariable {
       key("ref")
       value("\$.ref")
      }
      genericVariable {
       key("ref_type")
       value("\$.ref_type")
      }
     }
     genericHeaderVariables {
      genericHeaderVariable {
       key("x-github-event")
       regexpFilter("")
      }
     }
     printContributedVariables(true)
     printPostContent(true)
     silentResponse(false)
     regexpFilterText("\$ref")
     regexpFilterExpression(regexpFilterRegexValue)
    }
   }
  }
 }

 environmentVariables{
    groovy('''
        if (ref_type.equals("branch") && ref.endsWith("-prerelease")) {
            def kieToolingBranch = ref
            def kieToolingVersion = (ref =~ /[0-9]+\\.[0-9]+\\.[0-9]+/)[ 0 ]
            def kieToolingUmbVersion = kieToolingVersion.replaceAll("\\.", "-")
            
            def result = ["KIE_TOOLS_BRANCH":  kieToolingBranch, 
                          "KIE_TOOLS_VERSION": kieToolingVersion, 
                          "KIE_TOOLS_UMB_VERSION": kieToolingUmbVersion]
            return result;
        } else {
            return null;
        }
    ''')
 }

 definition {
  cps {
   script('''
    node {
        agent {
            label "$labelName"
        }

        stage('Send UMB') {
            when {
                expression {
                    return (\${KIE_TOOLS_VERSION} != null)
                }
            }

            ciMessageBuilder {
                providerData {
                    activeMQPublisher {
                        name('Red Hat Umb')
                        messageContent('
                                        {
                                            \"npmRegistry\": \"\${NPM_REGISTRY_PUBLISH_URL}",
                                            \"kieToolingVersion\": \"\${KIE_TOOLS_VERSION}\",
                                            \"kieToolingBranch\": \"\${KIE_TOOLS_BRANCH}\"
                                        }
                        ')
                        failOnError(false)
                        messageProperties('CI_TYPE=custom label=rhba-ci')
                        messageType('Custom')
                        overrides {
                            topic('VirtualTopic.qe.ci.ba.kie-tools.\${KIE_TOOLS_UMB_VERSION}.CR.trigger')
                        }
                    }
                }
            }  
        }
    }
   ''')
   sandbox()
  }
 }
}
