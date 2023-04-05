import org.kie.jenkins.jobdsl.Constants

def folderPath = 'OSBS'
folder('OSBS')
// Job Description
String jobDescription = 'Job responsible for seed jobs to building nightly openshift image build pipelines'

//Define Variables
String bamoe = "bamoe"
String rhpam = "rhpam"
def prodComponent = [bamoe, rhpam]

def buildDate = Constants.BUILD_DATE
Map<String, String> prodVersion = [
    bamoe : Constants.BAMOE_NEXT_PROD_VERSION,
    rhpam : Constants.NEXT_PROD_VERSION
]
def verbose = Constants.VERBOSE

prodComponent.each { component ->

    pipelineJob("${folderPath}/${component}-kogito-nightly-build-pipeline") {

        parameters {
            stringParam('PROD_VERSION', "${prodVersion.get(component)}")
            stringParam('PROD_COMPONENT', "${component}")
            stringParam('VERBOSE', "${verbose}")
            stringParam("PROPERTY_FILE_URL", "") // in case we would like this job manually (not triggered by UMB message)
        }

        logRotator {
            numToKeep(5)
        }

        definition {
            cpsScm {
                scm {
                    git {
                        remote {
                            url("https://github.com/kiegroup/kie-ci.git")
                        }
                        branch("main")

                    }
                }
                scriptPath("job-dsls/jobs/osbs/kogito_osbs_build_pipeline.jenkinsfile")
                lightweight(false)
            }
        }

        properties {
            pipelineTriggers {
                triggers {
                    ciBuildTrigger {
                        noSquash(true)
                        providers {
                            providerDataEnvelope {
                                providerData {
                                    activeMQSubscriber {
                                        name('Red Hat UMB')
                                        overrides {
                                            topic("Consumer.ba-eng-jenkins.${UUID.randomUUID()}.VirtualTopic.qe.ci.ba.kogito.${getUMBFromVersion(prodVersion.get(component))}.nightly.trigger")
                                        }

                                        selector("CI_TYPE='custom' and label='rhba-ci'")
                                        timeout(60)
                                        variable("CI_MESSAGE")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

String getUMBFromVersion(def version) {
    // if empty return main branch too
    if (isMainBranchVersion(version) || version == '') {
        return Constants.MAIN_BRANCH
    }
    def matcher = version =~ /(\d*)\.(\d*)\.?/
    return "${matcher[0][1]}${matcher[0][2]}${getBlueSuffix(version, '')}"
}

String getBlueSuffix(String version, String separator) {
    return version.endsWith('blue') ? separator + 'blue' : ''
}

boolean isMainBranchVersion(String version) {
    return [Constants.MAIN_BRANCH_PROD_VERSION, Constants.KOGITO_MAIN_BRANCH_PROD_VERSION, Constants.RHBOP_MAIN_BRANCH_PROD_VERSION].contains(version)
}