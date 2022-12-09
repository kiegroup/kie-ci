/**
 * job that publishes automatically the ${repo}-website
 */

import org.kie.jenkins.jobdsl.Constants

def baseBranch=Constants.BRANCH
// creation of folder
folder("KIE")
folder("KIE/${baseBranch}")
folder("KIE/${baseBranch}/webs")
def folderPath="KIE/${baseBranch}/webs"


def final DEFAULTS = [
        repository : "kiegroup-website",
        jenkinsFilePath : ".ci/jenkins/Jenkinsfile.publish"
]

def final REPO_CONFIGS = [
        "kiegroup"  : [],
        "jbpm"   : [
                repository : "jbpm-website"
        ]
]

for (reps in REPO_CONFIGS) {
    Closure<Object> get = { String key -> reps.value[key] ?: DEFAULTS[key] }

    String REPO = get("repository")
    String GH_ORG_UNIT = "kiegroup"
    String JENKINSFILE_PWD = 'kie-ci'
    String JENKINSFILE_URL = "https://github.com/${GH_ORG_UNIT}/${REPO}"
    String JENKINSFILE_BRANCH = 'main'
    String JENKINSFILE_PATH = get("jenkinsFilePath")

    pipelineJob("${folderPath}/${REPO}-automatic-publishing") {
        
        description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |
                    |this is a pipeline job for publishing automatically the ${REPO}.
                    |""".stripMargin())

        logRotator {
            numToKeep(5)
        }

        properties {
            githubProjectUrl("https://github.com/kiegroup/${REPO}")
            pipelineTriggers {
                triggers {
                    githubPush()
                }
            }
        }

        definition {
            cpsScm {
                scm {
                    gitSCM {
                        userRemoteConfigs {
                            userRemoteConfig {
                                url(JENKINSFILE_URL)
                                credentialsId(JENKINSFILE_PWD)
                                name('')
                                refspec('')
                            }
                        }
                        branches {
                            branchSpec {
                                name("*/${JENKINSFILE_BRANCH}")
                            }
                        }
                        browser { }
                        doGenerateSubmoduleConfigurations(false)
                        gitTool('')
                    }
                }
                scriptPath(JENKINSFILE_PATH)
            }
        }

    }
}
