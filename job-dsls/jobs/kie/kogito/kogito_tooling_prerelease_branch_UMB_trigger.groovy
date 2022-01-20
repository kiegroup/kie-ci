/**
 * Creates job that triggers when a new `<MAJOR>-<MINOR>-<PATCH>-prerelease` branch is
   pushed to kogito-tooling repository and notifies QE of this by sending and UMB message.
 */
import org.kie.jenkins.jobdsl.Constants

def repo = "kogito-tooling"
def repoBranch = ''
def ghOrgUnit = "kiegroup"
def ghAuthTokenId = "kie-ci"
def labelName = "kie-rhel7"
def regexpFilterRegexValue = '([0-9]+)\\.([0-9]+)\\.([0-9]+)-prerelease'

// creation of folder
folder("KIE")
folder ("KIE/kogito")
folder ("KIE/kogito/kogito-tooling")

def folderPath="KIE/kogito/kogito-tooling"

String jobName = "${folderPath}/${repo}-prerelease-branch-UMB-trigger"

pipelineJob(jobName) {
 parameters {
  stringParam('ref', '')
  stringParam('ref_type', '')
  stringParam('x_github_event', '')
 }

 triggers {
  genericTrigger {
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

 environmentVariables{
    groovy('''
        if (ref_type.equals("branch") && ref.endsWith("-prerelease")) {
            def kogitoToolingBranch = ref
            def kogitoToolingVersion = (ref =~ /[0-9]+\\.[0-9]+\\.[0-9]+/)[ 0 ]
            def kogitoToolingUmbVersion = kogitoToolingVersion.replaceAll("\\.", "-")
            
            def result = ["KOGITO_TOOLING_BRANCH":  kogitoToolingBranch, 
                          "KOGITO_TOOLING_VERSION": kogitoToolingVersion, 
                          "KOGITO_TOOLING_UMB_VERSION": kogitoToolingUmbVersion]
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
                    return (\${KOGITO_TOOLING_VERSION} != null)
                }
            }

            ciMessageBuilder {
                providerData {
                    activeMQPublisher {
                        name('Red Hat Umb')
                        messageContent('
                                        {
                                            \"npmRegistry\": \"\${NPM_REGISTRY_PUBLISH_URL}",
                                            \"kogitoToolingVersion\": \"\${KOGITO_TOOLING_VERSION}\",
                                            \"kogitoToolingBranch\": \"\${KOGITO_TOOLING_BRANCH}\"
                                        }
                        ')
                        failOnError(false)
                        messageProperties('CI_TYPE=custom label=rhba-ci')
                        messageType('Custom')
                        overrides {
                            topic('VirtualTopic.qe.ci.ba.kogito-tooling.\${KOGITO_TOOLING_UMB_VERSION}.CR.trigger')
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
