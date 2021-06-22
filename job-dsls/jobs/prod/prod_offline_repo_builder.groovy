/**
 * Check offliner manifest against offline repository
 */
import org.kie.jenkins.jobdsl.Constants

String commands = this.getClass().getResource("job-scripts/prod_offline_repo_builder.sh").text

def folderPath = "PROD"
folder(folderPath)

job("${folderPath}/offline-repo-builder") {
    description("Check offliner manifest against offline repository")

    logRotator {
        numToKeep(5)
        artifactNumToKeep(1)
    }

    parameters {
        stringParam("MANIFEST_URL", "", "Offliner manifest URL")
        stringParam("RELEASE_REPO_GROUP_URL", "http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/groups/offline-repo-group-7-11/", "Scratch repositories group for the release")
    }

    scm {
        git {
            remote {
                url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com/integration-platform-tooling")
                branch("main")
                credentials("code.engineering.redhat.com")
            }
            extensions {
                relativeTargetDirectory("ip-tooling")
            }
        }
    }

    wrappers {
        preBuildCleanup()
    }

    steps {
        shell(commands)
    }

    publishers {
        archiveArtifacts("errors.log,download-stats/*.log")
    }

}