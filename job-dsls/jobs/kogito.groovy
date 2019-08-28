import org.kie.jenkins.jobdsl.Constants

def javadk=Constants.JDK_VERSION
def javaToolEnv="KIE_JDK1_8"
def mvnToolEnv="KIE_MAVEN_3_5_4"
def mvnVersion="kie-maven-3.5.4"
def mvnHome="${mvnToolEnv}_HOME"
def javaHome="${javaToolEnv}_HOME"
def mvnOpts="-Xms1g -Xmx3g"
def m2Dir="\$HOME/.m2/repository"
def mainBranch="master"
def ghOrgUnit=Constants.GITHUB_ORG_UNIT

// creation of folder
folder("kogito-deploy")

def folderPath="kogito-deploy"

def kogitoPipeline = ''' 
pipeline {
   agent {label('kie-rhel7&&kie-mem8g')}  
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
       stage('kogito-bom') {
           steps {
               build job: "kogito-bom-${mainBranch}", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'mainBranch', value: mainBranch], [$class: 'StringParameterValue', name: 'ghOrgUnit', value: ghOrgUnit]] 
           }
       }
       stage('kogito-runtimes') {
           steps {
               build job: "kogito-runtimes-${mainBranch}", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'mainBranch', value: mainBranch], [$class: 'StringParameterValue', name: 'ghOrgUnit', value: ghOrgUnit]] 
           }
       }
       stage('kogito-cloud') {
           steps {
               build job: "kogito-cloud-${mainBranch}", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'mainBranch', value: mainBranch], [$class: 'StringParameterValue', name: 'ghOrgUnit', value: ghOrgUnit]] 
           }
       }       
       stage('kogito-examples') {
           steps {
               build job: "kogito-examples-${mainBranch}", parameters: [[$class: 'StringParameterValue', name: 'mainBranch', value: mainBranch], [$class: 'StringParameterValue', name: 'ghOrgUnit', value: ghOrgUnit]] 
           }
       }              
   }
}'''

pipelineJob("$folderPath/kogito-pipeline-${mainBranch}") {

    description("pipeline for all relevant kogito projects build")

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
                topic 'Consumer.rh-jenkins-ci-plugin.${JENKINS_UMB_ID}-prod-daily-7-26-x-submarine-trigger.VirtualTopic.qe.ci.ba.daily-7-26-x-submarine.trigger'
            }
            selector 'origin = \'rhba\''
        }
    }

    definition {
        cps {
            script("${kogitoPipeline}")
        }
    }

}

// ++++++++++++++++++++++++++++++++++++++++++ Build and deploys kogito-bom ++++++++++++++++++++++++++++++++++++++++++++++++++

// definition of kogito-bom script
def kogitoBom='''#!/bin/bash -e
# removing kogito-bom artifacts from local maven repo (basically all possible SNAPSHOTs)
if [ -d $MAVEN_REPO_LOCAL ]; then
  rm -rf $MAVEN_REPO_LOCAL/org/kie/kogito-bom/
fi
# clone the kogito-bom repository
git clone https://github.com/$ghOrgUnit/kogito-bom.git -b $mainBranch --depth 50
# build the project
cd kogito-bom
mvn -U -B -e clean deploy -s $SETTINGS_XML_FILE -Dkie.maven.settings.custom=$SETTINGS_XML_FILE '''


job("${folderPath}/kogito-bom-${mainBranch}") {
    description("build project kogito-bom")

    parameters {
        stringParam("mainBranch", "${mainBranch}", "Branch to clone. This will be usually set automatically by the parent trigger job. ")
        stringParam("ghOrgUnit", "${ghOrgUnit}", "Name of organization. This will be usually set automatically by the parent trigger job. ")
    }

    label('submarine-static')

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            elastic(250, 3, 90)
        }
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
        preBuildCleanup()
        configFiles {
            mavenSettings("9239af2e-46e3-4ba3-8dd6-1a814fc8a56d"){
                variable("SETTINGS_XML_FILE")
                targetLocation("jenkins-settings.xml")
            }
        }
    }

    publishers {
        mailer('mbiarnes@redhat.com', false, false)
        mailer('mswiders@redhat.com', false, false)
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
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", JAVA_HOME : "\$${javaHome}", MAVEN_REPO_LOCAL : "${m2Dir}", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(kogitoBom)
    }

}

// ++++++++++++++++++++++++++++++++++++++++++ Build and deploys kogito-runtimes ++++++++++++++++++++++++++++++++++++++++++++++++++

// definition of kogito-runtimes script
def kogitoRuntimes='''#!/bin/bash -e
# removing kogito-runtimes artifacts from local maven repo (basically all possible SNAPSHOTs)
if [ -d $MAVEN_REPO_LOCAL ]; then
  rm -rf $MAVEN_REPO_LOCAL/org/kie/kogito-runtimes/
fi
# clone the kogito-bom repository
git clone https://github.com/$ghOrgUnit/kogito-runtimes.git -b $mainBranch --depth 50
# build the project
cd kogito-runtimes
mvn -U -B -e clean deploy -s $SETTINGS_XML_FILE -Dkie.maven.settings.custom=$SETTINGS_XML_FILE -Dmaven.test.redirectTestOutputToFile=true -Dmaven.test.failure.ignore=true'''


job("${folderPath}/kogito-runtimes-${mainBranch}") {
    description("build project kogito-runtimes")
    parameters {
        stringParam("mainBranch", "${mainBranch}", "Branch to clone. This will be usually set automatically by the parent trigger job. ")
        stringParam("ghOrgUnit", "${ghOrgUnit}", "Name of organization. This will be usually set automatically by the parent trigger job. ")
    }

    label('submarine-static')

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            elastic(250, 3, 90)
        }
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
        preBuildCleanup()
        configFiles {
            mavenSettings("9239af2e-46e3-4ba3-8dd6-1a814fc8a56d"){
                variable("SETTINGS_XML_FILE")
                targetLocation("jenkins-settings.xml")
            }
        }
    }

    publishers {
        archiveJunit("**/target/*-reports/TEST-*.xml")
        mailer('mbiarnes@redhat.com', false, false)
        mailer('mswiders@redhat.com', false, false)
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
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", JAVA_HOME : "\$${javaHome}", MAVEN_REPO_LOCAL : "${m2Dir}", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(kogitoRuntimes)
    }

}

// ++++++++++++++++++++++++++++++++++++++++++ Build and deploys kogito-cloud ++++++++++++++++++++++++++++++++++++++++++++++++++

// definition of kogito-cloud script
def kogitoCloud='''#!/bin/bash -e
# removing kogito-runtimes artifacts from local maven repo (basically all possible SNAPSHOTs)
if [ -d $MAVEN_REPO_LOCAL ]; then
  rm -rf $MAVEN_REPO_LOCAL/org/kie/kogito-cloud/
fi
# clone the kogito-bom repository
git clone https://github.com/$ghOrgUnit/kogito-cloud.git -b $mainBranch --depth 50
# build the project
cd kogito-cloud
mvn -U -B -e clean deploy -s $SETTINGS_XML_FILE -Dkie.maven.settings.custom=$SETTINGS_XML_FILE -Dmaven.test.redirectTestOutputToFile=true -Dmaven.test.failure.ignore=true'''


job("${folderPath}/kogito-cloud-${mainBranch}") {
    description("build project kogito-cloud")
    parameters {
        stringParam("mainBranch", "${mainBranch}", "Branch to clone. This will be usually set automatically by the parent trigger job. ")
        stringParam("ghOrgUnit", "${ghOrgUnit}", "Name of organization. This will be usually set automatically by the parent trigger job. ")
    }

    label('submarine-static')

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            elastic(250, 3, 90)
        }
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
        preBuildCleanup()
        configFiles {
            mavenSettings("9239af2e-46e3-4ba3-8dd6-1a814fc8a56d"){
                variable("SETTINGS_XML_FILE")
                targetLocation("jenkins-settings.xml")
            }
        }
    }

    publishers {
        archiveJunit("**/target/*-reports/TEST-*.xml")
        mailer('mbiarnes@redhat.com', false, false)
        mailer('mswiders@redhat.com', false, false)
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
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", JAVA_HOME : "\$${javaHome}", MAVEN_REPO_LOCAL : "${m2Dir}", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(kogitoCloud)
    }

}

// ++++++++++++++++++++++++++++++++++++++++++ Build and deploys kogito-examples ++++++++++++++++++++++++++++++++++++++++++++++++++

// definition of kogito-examples script
def kogitoExamples='''#!/bin/bash -e
# removing kogito-examples artifacts from local maven repo (basically all possible SNAPSHOTs)
if [ -d $MAVEN_REPO_LOCAL ]; then
  rm -rf $MAVEN_REPO_LOCAL/org/kie/kogito-examples
fi
# clone the kogito-bom repository
git clone https://github.com/$ghOrgUnit/kogito-examples.git -b $mainBranch --depth 50
# build the project
cd kogito-examples
mvn -U -B -e clean deploy -s $SETTINGS_XML_FILE -Dkie.maven.settings.custom=$SETTINGS_XML_FILE '''


job("${folderPath}/kogito-examples-${mainBranch}") {
    description("build project kogito-examples")

    parameters {
        stringParam("mainBranch", "${mainBranch}", "Branch to clone. This will be usually set automatically by the parent trigger job. ")
        stringParam("ghOrgUnit", "${ghOrgUnit}", "Name of organization. This will be usually set automatically by the parent trigger job. ")
    }

    label('submarine-static')

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            elastic(250, 3, 90)
        }
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
        preBuildCleanup()
        configFiles {
            mavenSettings("9239af2e-46e3-4ba3-8dd6-1a814fc8a56d"){
                variable("SETTINGS_XML_FILE")
                targetLocation("jenkins-settings.xml")
            }
        }
    }

    publishers {
        mailer('mbiarnes@redhat.com', false, false)
        mailer('mswiders@redhat.com', false, false)
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
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", JAVA_HOME : "\$${javaHome}", MAVEN_REPO_LOCAL : "${m2Dir}", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(kogitoExamples)
    }

}
