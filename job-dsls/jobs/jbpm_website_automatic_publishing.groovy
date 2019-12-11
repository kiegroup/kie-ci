import org.kie.jenkins.jobdsl.Constants

def javadk=Constants.JDK_VERSION
def organization=Constants.GITHUB_ORG_UNIT

// creation of folder
folder("KIE")
folder("KIE/websites")

def folderPath="KIE/websites"

// definition of website publishing script
def websitePublish='''#!/bin/bash -e
cd $WORKSPACE

isImage=$(docker images -q kiegroup/jbpm-website)
echo "Image: $isImage"

if [ "$isImage" != "" ]; then
  docker rmi kiegroup/jbpm-website
fi
docker images
docker build -t kiegroup/jbpm-website:latest _dockerPublisher/
docker run --cap-add net_raw --cap-add net_admin -i --rm --volume /home/jenkins/.ssh/jbpm:/home/jenkins/.ssh:Z --name JBPM_CONTAINER kiegroup/jbpm-website:latest bash -l -c 'cd jbpm-website-master && sudo rake setup && rake clean build && rake publish'
'''

job("${folderPath}/jbpm_website_automatic_publishing") {

    description("Builds <b>optaplanner-website</b> inside a Docker container (which has all the needed deps like ruby and awestruct).\n" +
            "<br>The end goal is to automatically deploy the site once the build succeeds.<br><br> <b>IMPORTANT</b>: because of keys this job only can run on \"kie-releases\".<br>\n" +
            "<b>TODO</b>: this job is not yet prepared to run on an arbitral slave.")

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    scm {
        git {
            remote {
                github("${organization}/jbpm-website")
            }
            branch ("master")
        }
    }

    triggers {
        scm('H/30 * * * *')
    }

    wrappers {
        timeout {
            elastic(250, 3, 900)
        }
        timestamps()
        colorizeOutput()
        preBuildCleanup()
        configFiles {
            mavenSettings("771ff52a-a8b4-40e6-9b22-d54c7314aa1e"){
                variable("SETTINGS_XML_FILE")
                targetLocation("jenkins-settings.xml")
            }
        }
    }

    publishers {
        mailer('mbiarnes@redhat.com', false, false)
        //wsCleanup()
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
        shell(websitePublish)
    }
}