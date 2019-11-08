 /**
 * Executes PiG tool
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        repoFolder             : "/home/jenkins/git-repos/build-configuration.git",
        productFileGenerator   : " /opt/tools/pig/product-files-generator.jar"        
]
Closure<Object> get = { String key -> DEFAULTS[key] }
String repoFolder = get("repoFolder")
def javadk=Constants.JDK_VERSION

// Creation of folders where jobs are stored
folder(Constants.DEPLOY_FOLDER)

// jobs for master branch don't use the branch in the name
String jobName = "pigExecution"

job(jobName) {

    description("PiG execution")

    logRotator {
        numToKeep(10)
    }

    parameters {
        stringParam("repo", "https://gitlab.cee.redhat.com/middleware/build-configurations.git", "the build configuration repo")
        stringParam("repoBranch", "master", "the build configuration branch to be checked out")
        stringParam("buildConfiguration", "rhba/7.5.1", "the build configuration folder")
        stringParam("scmRevision", "", "the revision `sync-7.26.x-2019.10.29` for instance")
        stringParam("additionalParameters", "--skipBuilds --skipPncUpdate", "")
    }

    scm {
        git {
            remote {
                url("\${repo}")
                branch("\${repoBranch}")
            }
            extensions {
                cloneOptions {
                    reference("git-repos/build-configurations.git")
                }
            }
        }
    }

    properties {
        ownership {
            primaryOwnerId("mbiarnes")
            coOwnerIds("almorale", "anstephe", "emingora")
        }
    }

    label("kie-linux&&kie-mem4g")

    jdk("kie-jdk1.8")

    steps {
        shell("java -DskipBranchCheck -jar /opt/tools/pig/product-files-generator.jar -c $WORKSPACE/git-repos/build-configurations.git/\${buildConfiguration} -v scmRevision=\${scmRevision} \${additionalParameters}")
    }
}