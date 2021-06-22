 /**
 * Executes PiG tool
 */
// create needed folder(s) for where the jobs are created
 folder("Tools")

 def folderPath = ("Tools")

// jobs for main branch don't use the branch in the name
String jobName = "${folderPath}/pigExecution"

job(jobName) {

    description("PiG execution")

    logRotator {
        numToKeep(10)
    }

    parameters {
        stringParam("repo", "https://gitlab.cee.redhat.com/middleware/build-configurations.git", "the build configuration repo")
        stringParam("repoBranch", "main", "the build configuration branch to be checked out")
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
                relativeTargetDirectory('git-repos/build-configurations.git')
            }
        }
    }
    wrappers {
            configFiles{
            file('rhba-pnc-cli.conf') {
                targetLocation('~/.config/pnc-cli/pnc-cli.conf')
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
        shell("mkdir ~/.config/pnc-cli -p")
        shell("cp \$WORKSPACE/~/.config/pnc-cli/pnc-cli.conf ~/.config/pnc-cli/pnc-cli.conf")
        shell("java -DskipBranchCheck -jar /opt/tools/pig/product-files-generator.jar -c \$WORKSPACE/git-repos/build-configurations.git/\${buildConfiguration} -v scmRevision=\${scmRevision} \${additionalParameters}")
    }
}