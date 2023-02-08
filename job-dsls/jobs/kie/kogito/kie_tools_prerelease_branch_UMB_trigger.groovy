/**
 * Creates job that triggers when a new `<MAJOR>-<MINOR>-<PATCH>-prerelease` branch is
 pushed to kie-tools repository and notifies QE of this by sending and UMB message.
 */
import org.kie.jenkins.jobdsl.Constants

def repo = "kie-tools"
def labelName = "kie-rhel7 && kie-mem8g"
def javadk = Constants.JDK_TOOL
def regexpFilterRegexValue = '([0-9]+)\\.([0-9]+)\\.([0-9]+)-prerelease'

// creation of folder
folder ("KIE")
folder ("KIE/kogito")
folder ("KIE/kogito/kie-tools")

def folderPath="KIE/kogito/kie-tools"

String jobName = "${folderPath}/${repo}-prerelease-branch-UMB-trigger"

job(jobName) {

    description("this job sends an Red Hat UMB trigger when a new branch in kie-tools should be created")

    logRotator{
        numToKeep(5)
    }

    jdk(javadk)

    label(labelName)

    parameters {
        stringParam('ref', '')
        stringParam('ref_type', '')
        stringParam('x_github_event', '')
    }

    publishers {
        ciMessageNotifier {
            providerData {
                activeMQPublisher {
                    name('Red Hat Umb')
                    overrides {
                        topic("VirtualTopic.qe.ci.ba.kie-tools.CR.trigger")
                    }
                    // Type of CI message to be sent.
                    messageType('Custom')
                    messageContent("""
                            {
                                \"npmRegistry\": \"\${NPM_REGISTRY_PUBLISH_URL}\",
                                \"kieToolsVersion\": \"\${KIE_TOOLS_VERSION}\",
                                \"kieToolsBranch\": \"\${KIE_TOOLS_BRANCH}\"
                            }
                            """)
                    // Whether you want to fail the build if there is an error sending a message.
                    failOnError(false)
                    // KEY=value pairs, one per line (Java properties file format) to be used as message properties.
                    messageProperties('CI_TYPE=custom\nlabel=rhba-ci')

                }
            }
        }
        cleanWs()
    }

    environmentVariables{
        keepBuildVariables(true)
        keepSystemVariables(true)
        groovy("""
            print 'START'
            if (ref_type.equals("branch") && ref.endsWith("-prerelease")) {
                def kieToolsBranch = ref
                def kieToolsVersion = (ref =~ /[0-9]+\\.[0-9]+\\.[0-9]+/)[ 0 ]
                def kieToolsUmbVersion = kieToolsVersion.replaceAll("\\\\.", "-")

                print kieToolsBranch
                print kieToolsVersion
                print kieToolsUmbVersion
           
                def result = ["KIE_TOOLS_BRANCH":  kieToolsBranch, 
                              "KIE_TOOLS_VERSION": kieToolsVersion,
                              "KIE_TOOLS_UMB_VERSION": kieToolsUmbVersion]
                return result;
                } else {
                return null;
            }
        """)
    }

    triggers {
        GenericTrigger{
            genericVariables {
                genericVariable {
                    key('ref')
                    value("\$.ref")
                    expressionType('JSONPath')
                }
                genericVariable {
                    key('ref_type')
                    value("\$.ref_type")
                    expressionType('JSONPath')
                }
            regexpFilterText('') //Optional, defaults to empty string
            regexpFilterExpression('') //Optional, defaults to empty string
            }
            genericHeaderVariables {
                genericHeaderVariable {
                    key('x-github-event')
                    regexpFilter("")
                }
                printContributedVariables(true)
                printPostContent(true)
                silentResponse(false)
                regexpFilterText("\$ref") //Optional, defaults to empty string
                regexpFilterExpression("$regexpFilterRegexValue") //Optional, defaults to empty string
            }
        }
    }
}
