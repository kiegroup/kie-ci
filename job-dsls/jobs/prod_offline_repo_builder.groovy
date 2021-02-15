/**
 * Check offliner manifest against offline repository
 */
import org.kie.jenkins.jobdsl.Constants
def offline = '''
      set +x
      
      if [ ! MANIFEST_URL ]
      then
          echo "ERROR: Manifest URL not supplied."
          exit 1
      fi
      
      MANIFEST="offliner-manifest.txt"
      if ! curl -s -k -f -o $MANIFEST $MANIFEST_URL
      then
          echo "ERROR: Failed to download the offliner manifest"
          exit 1
      fi   
      
      RELEASE_REPO=$(echo $RELEASE_REPO_URL | sed 's/\\//\\\\\\//g')
      WRAPPER="ip-tooling/scripts/rhba/build-offline-repo.sh"
      sed -i "s/MRRC=.*/MRRC=$RELEASE_REPO/g" $WRAPPER
      
      chmod 755 $WRAPPER
      OUTPUT="output.log"
      "./$WRAPPER" $MANIFEST | tee $OUTPUT
      
      
      ### Stats
      STATS="download-stats"
      mkdir $STATS
      
      ATTEMPTS="$STATS/attempts.log"
      grep -P '^>>>Downloading: .*$' $OUTPUT | sed 's/>>>Downloading: //g' > $ATTEMPTS
      
      MISSING="$STATS/missing.log"
      grep -P '^<<<Not Found: .*$' $OUTPUT | sed 's/<<<Not Found: //g' > $MISSING
      
      FAILURES="$STATS/failures.log"
      grep -P '^<<<FAIL: .*$' $OUTPUT | sed 's/<<<FAIL: //g' > $FAILURES
      
      SUCCESS="$STATS/success.log"
      grep -vF -f $MISSING $ATTEMPTS > $SUCCESS
      
      
      echo "Downloads by repo:"
      REPOS=$(sed 's/\\(https\\?:\\/\\/[^/]*\\/\\).*/\\1/g' $SUCCESS | sort -u)
      for repo in $REPOS;
      do
          downloads=$(grep $repo $SUCCESS | wc -l)
          echo "$repo: $downloads"
      done
'''

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
        shell(offline)
    }

    publishers {
        archiveArtifacts("errors.log,download-stats/*.log")
    }

}