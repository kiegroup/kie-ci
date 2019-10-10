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
        stringParam("repo")
        stringParam("repoBranch")
        stringParam("buildConfiguration")
        stringParam("scmRevision")
        stringParam("additionalParameters")
    }

    scm {
        git {
            remote {
                url("\${repo}")
                branch("\${repoBranch}")
            }
            extensions {
                cloneOptions {
                    // git repo cache which is present on the slaves
                    // it significantly reduces the clone time and also saves a lot of bandwidth
                    reference("${repoFolder}")
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

    jdk("kie-jdk1.8")

    steps {
        shell("sh java -DskipBranchCheck -jar core/target/product-files-generator.jar -c ${repoFolder}/\${buildConfiguration} -v scmRevision=\${scmRevision} \${additionalParameters}")
    }
}