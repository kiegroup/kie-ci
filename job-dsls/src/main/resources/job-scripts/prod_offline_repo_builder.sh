#!/usr/bin/env bash +x

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

RELEASE_REPO=$(echo $RELEASE_REPO_URL | sed 's/\//\\\//g')
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
REPOS=$(sed 's/\(https\?:\/\/[^/]*\/\).*/\1/g' $SUCCESS | sort -u)
for repo in $REPOS;
do
  downloads=$(grep $repo $SUCCESS | wc -l)
  echo "$repo: $downloads"
done
