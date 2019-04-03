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
folder("submarine")

def folderPath="submarine"

def submarines = ''' 
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
       stage('submarine-bom') {
           steps {
               build job: "submarine-bom-${mainBranch}", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'mainBranch', value: mainBranch], [$class: 'StringParameterValue', name: 'ghOrgUnit', value: ghOrgUnit]] 
           }
       }
       stage('submarine-runtimes') {
           steps {
               build job: "submarine-runtimes-${mainBranch}", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'mainBranch', value: mainBranch], [$class: 'StringParameterValue', name: 'ghOrgUnit', value: ghOrgUnit]] 
           }
       }
       stage('submarine-examples') {
           steps {
               build job: "submarine-examples-${mainBranch}", parameters: [[$class: 'StringParameterValue', name: 'mainBranch', value: mainBranch], [$class: 'StringParameterValue', name: 'ghOrgUnit', value: ghOrgUnit]] 
           }
       }              
   }
}'''

pipelineJob("$folderPath/submarine-pipeline-${mainBranch}") {

    description("pipeline for all relevant submarine projects build")

    parameters {
        stringParam("mainBranch", "${mainBranch}", "edit the branch here. ")
        stringParam("ghOrgUnit", "${ghOrgUnit}", "edit the organization. ")
    }

    logRotator {
        numToKeep(10)
        daysToKeep(10)
    }

    triggers {
        cron("H 20 * * *")
    }

    definition {
        cps {
            script("${submarines}")
        }
    }

}

// ++++++++++++++++++++++++++++++++++++++++++ Build and deploys submarine-bom ++++++++++++++++++++++++++++++++++++++++++++++++++

// definition of submarine-bom script
def submarineBom='''#!/bin/bash -e
# removing submarine-bom artifacts from local maven repo (basically all possible SNAPSHOTs)
if [ -d $MAVEN_REPO_LOCAL ]; then
  rm -rf $MAVEN_REPO_LOCAL/org/kie/submarine-bom/
fi
# clone the submarine-bom repository
git clone https://github.com/$ghOrgUnit/submarine-bom.git -b $mainBranch --depth 50
# build the project
cd submarine-bom
mvn -U -B -e clean deploy -s $SETTINGS_XML_FILE -Dkie.maven.settings.custom=$SETTINGS_XML_FILE '''


job("${folderPath}/submarine-bom-${mainBranch}") {
    description("build project submarine-bom")

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
        shell(submarineBom)
    }

}

// ++++++++++++++++++++++++++++++++++++++++++ Build and deploys submarine-runtimes ++++++++++++++++++++++++++++++++++++++++++++++++++

// definition of submarine-runtimes script
def submarineRuntimes='''#!/bin/bash -e
# removing submarine-runtimes artifacts from local maven repo (basically all possible SNAPSHOTs)
if [ -d $MAVEN_REPO_LOCAL ]; then
  rm -rf $MAVEN_REPO_LOCAL/org/kie/submarine-runtimes/
fi
# clone the submarine-bom repository
git clone https://github.com/$ghOrgUnit/submarine-runtimes.git -b $mainBranch --depth 50
# build the project
cd submarine-runtimes
mvn -U -B -e clean deploy -s $SETTINGS_XML_FILE -Dkie.maven.settings.custom=$SETTINGS_XML_FILE -Dmaven.test.redirectTestOutputToFile=true -Dmaven.test.failure.ignore=true'''


job("${folderPath}/submarine-runtimes-${mainBranch}") {
    description("build project submarine-runtimes")
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
        shell(submarineRuntimes)
    }

}

// ++++++++++++++++++++++++++++++++++++++++++ Build and deploys submarine-examples ++++++++++++++++++++++++++++++++++++++++++++++++++

// definition of submarine-examples script
def submarineExamples='''#!/bin/bash -e
# removing submarine-examples artifacts from local maven repo (basically all possible SNAPSHOTs)
if [ -d $MAVEN_REPO_LOCAL ]; then
  rm -rf $MAVEN_REPO_LOCAL/org/kie/submarine-examples
fi
# clone the submarine-bom repository
git clone https://github.com/$ghOrgUnit/submarine-examples.git -b $mainBranch --depth 50
# build the project
cd submarine-examples
mvn -U -B -e clean deploy -s $SETTINGS_XML_FILE -Dkie.maven.settings.custom=$SETTINGS_XML_FILE '''


job("${folderPath}/submarine-examples-${mainBranch}") {
    description("build project submarine-examples")

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
        shell(submarineExamples)
    }

}