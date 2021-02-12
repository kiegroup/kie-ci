/**
 * Check offliner manifest against offline repository
 */
import org.kie.jenkins.jobdsl.Constants

String commands = this.getClass().getResource("job-scripts/prod_offline_repo_builder.sh").text

folder("PROD")

job("PROD/offline-repo-builder") {
    description("Check offliner manifest against offline repository")

    logRotator {
        numToKeep(5)
        artifactNumToKeep(1)
    }

    parameters {
        stringParam("MANIFEST_URL", "", "Offliner manifest URL")
        stringParam("RELEASE_REPO_URL", "http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/offline-repo-7.10/", "Scratch repository for the release")
    }

    scm {
        git {
            remote {
                url("ssh://jb-ip-tooling-jenkins@code.engineering.redhat.com/integration-platform-tooling")
                branch("master")
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