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
def osbsBuildTarget = Constants.OSBS_BUILD_TARGET
def cekitBuildOptions = Constants.CEKIT_BUILD_OPTIONS
def osbsBuildUser = Constants.OSBS_BUILD_USER
def kerberosPrincipal = Constants.KERBEROS_PRINCIPAL
def kerberosKeytab = Constants.KERBEROS_KEYTAB
def kerberosCred = Constants.KERBEROS_CRED
def imageRepo = Constants.IMAGE_REPO
Map<String, String> imageBranch = [
    bamoe : Constants.BAMOE_IMAGE_BRANCH,
    rhpam : Constants.IMAGE_BRANCH
]
def imageSubdir = Constants.IMAGE_SUBDIR
def gitUser = Constants.GIT_USER
def gitEmail = Constants.GIT_EMAIL
def cekitCacheLocal = Constants.CEKIT_CACHE_LOCAL
def verbose = Constants.VERBOSE

prodComponent.each { component ->

    pipelineJob("${folderPath}/${component}-nightly-build-pipeline") {

        parameters {
            // stringParam('BUILD_DATE', "${buildDate}") // this shouldn't be needed as jobs should recieved url properties form the CI_MESSAGE
            stringParam('PROD_VERSION', "${prodVersion.get(component)}")
            stringParam('PROD_COMPONENT', "${component}")
            stringParam('OSBS_BUILD_TARGET', "${osbsBuildTarget}")
            stringParam('CEKIT_BUILD_OPTIONS', "${cekitBuildOptions}")
            stringParam('KERBEROS_PRINCIPAL', "${kerberosPrincipal}")
            stringParam('OSBS_BUILD_USER', "${osbsBuildUser}")
            stringParam('KERBEROS_KEYTAB', "${kerberosKeytab}")
            stringParam('KERBEROS_CRED', "${kerberosCred}")
            stringParam('IMAGE_REPO', "${imageRepo}")
            stringParam('IMAGE_BRANCH', "${imageBranch.get(component)}")
            stringParam('IMAGE_SUBDIR', "${imageSubdir}")
            stringParam('GIT_USER', "${gitUser}")
            stringParam('GIT_EMAIL', "${gitEmail}")
            stringParam('CEKIT_CACHE_LOCAL', "${cekitCacheLocal}")
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
                            url("git@github.com:kiegroup/kie-ci.git")
                        }
                        branch("main")

                    }
                }
                scriptPath("job-dsls/jobs/osbs/osbs_build_pipeline.jenkinsfile")
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
                                            topic("Consumer.ba-qe-jenkins.${UUID.randomUUID()}.VirtualTopic.qe.ci.ba.${component}.${prodVersion.get(component)}.${stream}.trigger")
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