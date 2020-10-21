/**
 * creates the kie-docker-ui-webapp: http://kieci-02.lab.eng.brq.redhat.com:8080/kie-docker-ui-webapp/
 */

import org.kie.jenkins.jobdsl.Constants

def ghOrgUnit = Constants.GITHUB_ORG_UNIT
def repo = "kie-docker-ci"
def mvnGoals = "-e -B clean install"
def labelName = "kieci-02-docker"
def ghJenkinsfilePwd = "kie-ci"
def repoBranch = Constants.BRANCH
def gitHubJenkinsfileRepUrl = "https://github.com/${ghOrgUnit}/${repo}/"
def mavenVersion = Constants.MAVEN_VERSION
def javaVersion = Constants.JDK_VERSION

// Creation of folders where jobs are stored
folder("docker-ui-webapp")
def folderPath="docker-ui-webapp"
def exeScript ="./scripts/kie-docker-ui-containers-clean.sh"

job("${folderPath}/docker-ui-build") {

    description("Builds and deploys latest version of the kie-docker-ci web application")

    logRotator {
        numToKeep(3)
    }

    scm {
        gitSCM {
            userRemoteConfigs {
                userRemoteConfig {
                    url("${gitHubJenkinsfileRepUrl}")
                    credentialsId("${ghJenkinsfilePwd}")
                    name("")
                    refspec("")
                }
                branches {
                    branchSpec {
                        name("*/${repoBranch}")
                    }
                }
                browser { }
                doGenerateSubmoduleConfigurations(false)
                gitTool("")
            }
        }
    }

    concurrentBuild()

    properties {
        ownership {
            primaryOwnerId("mbiarnes")
            coOwnerIds("mbiarnes")
        }
        githubProjectUrl("https://github.com/${ghOrgUnit}/${repo}")

    }

    jdk(javaVersion)

    label(labelName)


    // executes a shell script
    steps {
        environmentVariables{
            env("JAVA_OPTS","\"-Djsse.enableSNIExtension=false\"")
        }
        shell(exeScript)
        maven {
            mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")
            goals(mvnGoals)
            property("kie.dockerui.privateHost","172.17.0.1")
            property("kie.dockerui.publicHost","kieci-02.lab.eng.brq.redhat.com")
            providedSettings("771ff52a-a8b4-40e6-9b22-d54c7314aa1e")
            rootPOM("${repo}")
            injectBuildVariables(true)
        }
    }

    wrappers {
        timeout {
            elastic(150, 3, 30)
        }
        timestamps()
        colorizeOutput()
        toolenv(javaVersion, mavenVersion)
        preBuildCleanup()
    }

    publishers {
        wsCleanup()
    }
}