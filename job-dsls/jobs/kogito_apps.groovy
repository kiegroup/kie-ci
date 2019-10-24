import org.kie.jenkins.jobdsl.Constants

def mainBranch = "master"
def ghOrgUnit = Constants.GITHUB_ORG_UNIT

// creation of folder
folder("kogito-apps")

def folderPath = "kogito-apps"

def kogitoPipeline = ''' 
pipeline {
   agent { 
        label('kie-rhel7 && kie-mem8g')
   }
   stages {
        stage ('parameter') {
           steps {
               script {
                   mainBranch="${mainBranch}"
                   ghOrgUnit="${ghOrgUnit}"
                   echo "main branch :${mainBranch}"
                   echo "git hub organization : ${ghOrgUnit}"
               }
           }     
        }
        stage('kogito-apps') {
           steps {
               build job: "kogito-apps-${mainBranch}", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'mainBranch', value: mainBranch], [$class: 'StringParameterValue', name: 'ghOrgUnit', value: ghOrgUnit]] 
           }
        }
   }
}'''

pipelineJob("$folderPath/kogito-apps-pipeline-${mainBranch}") {

    description("pipeline for all relevant Kogito Apps projects build")

    parameters {
        stringParam("mainBranch", "${mainBranch}", "edit the branch here. ")
        stringParam("ghOrgUnit", "${ghOrgUnit}", "edit the organization. ")
    }

    logRotator {
        numToKeep(10)
        daysToKeep(10)
    }

    configure { project ->
        project / triggers << 'com.redhat.jenkins.plugins.ci.CIBuildTrigger' {
            spec ''
            providerName 'Red Hat UMB'
            overrides {
                topic 'Consumer.rh-jenkins-ci-plugin.${JENKINS_UMB_ID}-prod-daily-master-submarine-trigger.VirtualTopic.qe.ci.ba.daily-master-submarine.trigger'
            }
            selector 'label = \'rhba-ci\''
        }
    }

    definition {
        cps {
            script("${kogitoPipeline}")
        }
    }

}

// ++++++++++++++++++++++++++++++++++++++++++ Build and deploys kogito-apps ++++++++++++++++++++++++++++++++++++++++++++++++++

// definition of kogito-apps script
def kogitoApps = '''#!/bin/bash -e
# clone the kogito-apps repository
git clone https://github.com/$ghOrgUnit/kogito-apps.git -b $mainBranch --depth 50
# build the project
cd kogito-apps
yarn config set registry ${NPM_REGISTRY_URL}
yarn install
yarn run build:prod
yarn run test'''

job("${folderPath}/kogito-apps-${mainBranch}") {
    description("build project kogito-apps")
    parameters {
        stringParam("mainBranch", "${mainBranch}", "Branch to clone. This will be usually set automatically by the parent trigger job. ")
        stringParam("ghOrgUnit", "${ghOrgUnit}", "Name of organization. This will be usually set automatically by the parent trigger job. ")
    }

    label('submarine-static')

    logRotator {
        numToKeep(10)
    }

    wrappers {
        timeout {
            elastic(250, 3, 90)
        }
        timestamps()
        colorizeOutput()
        preBuildCleanup()
    }

    publishers {
        archiveJunit("**/**/junit.xml")
        mailer('mbiarnes@redhat.com', false, false)
        mailer('cnicolai@redhat.com', false, false)
        wsCleanup()
    }

    configure { project ->
        project / 'buildWrappers' << 'org.jenkinsci.plugins.proccleaner.PreBuildCleanup' {
            cleaner(class: 'org.jenkinsci.plugins.proccleaner.PsCleaner') {
                killerType 'org.jenkinsci.plugins.proccleaner.PsAllKiller'
                killer(class: 'org.jenkinsci.plugins.proccleaner.PsAllKiller')
                username 'jenkins'
            }
        }
    }

    steps {
        shell(kogitoApps)
    }

}