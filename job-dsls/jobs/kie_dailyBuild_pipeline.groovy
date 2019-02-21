import org.kie.jenkins.jobdsl.Constants

// definition of parameters (will change with each branch)

def javadk=Constants.JDK_VERSION
def javaToolEnv="KIE_JDK1_8"
def mvnToolEnv="KIE_MAVEN_3_5_4"
def mvnVersion="kie-maven-3.5.4"
def mvnHome="${mvnToolEnv}_HOME"
def javaHome="${javaToolEnv}_HOME"
def mvnOpts="-Xms1g -Xmx3g"
def kieMainBranch=Constants.BRANCH
def kieVersion=Constants.KIE_PREFIX
def kieProdBranch=Constants.KIE_PROD_BRANCH_PREFIX
def appformerVersion=Constants.UBERFIRE_PREFIX
def erraiBranch=Constants.ERRAI_BRANCH
def erraiVersionOld=Constants.ERRAI_SNAPSHOT
def erraiVersionNew=Constants.ERRAI_PREFIX
def organization=Constants.GITHUB_ORG_UNIT
def m2Dir="\$HOME/.m2/repository"
String EAP7_DOWNLOAD_URL = "http://download-ipv4.eng.brq.redhat.com/released/JBoss-middleware/eap7/7.1.4/jboss-eap-7.1.4-full-build.zip"

// creation of folder
folder("KIE")
folder("KIE/${kieMainBranch}")
folder("KIE/${kieMainBranch}/dailyBuild")
folder("KIE/Docker")

def folderPath="KIE/${kieMainBranch}/dailyBuild"
def dockerPath="KIE/Docker"

// definition of pipeline jobs

def kieAllpipeline = ''' 
pipeline {
  agent {label('kie-linux&&kie-mem512m')}
   
  stages {
    stage('parameter') {
      steps {
        script {
          date = new Date().format('yyyyMMdd-hhMMss')
          dateProd = new Date().format('yyyyMMdd')
          kieProdVersion = "${kieVersion}.${dateProd}-prod"
          appformerProdVersion = "${appformerVersion}.${dateProd}-prod"          
          kieVersion = "${kieVersion}.${date}"
          appformerVersion = "${appformerVersion}.${date}"
          erraiVersionNew = "${erraiVersionNew}.${date}"
          kieProdBranch = "bsync-${kieProdBranch}-${dateProd}"
          sourceProductTag = ""
          targetProductBuild = ""

                  
          echo "kieVersion: ${kieVersion}"
          echo "appformerVersion: ${appformerVersion}"
          echo "erraiVersionOld: ${erraiVersionOld}"
          echo "erraiVersionNew: ${erraiVersionNew}"
          echo "kieMainBranch: ${kieMainBranch}"
          echo "erraiBranch: ${erraiBranch}"
          echo "organization: ${organization}"
          echo "sourceProductTag: ${sourceProductTag}"
          echo "targetProductBuild: ${targetProductBuild}"
          echo "kieProdVersion: ${kieProdVersion}"
          echo "appformerProdVersion: ${appformerProdVersion}"
          echo "kieProdBranch: ${kieProdBranch}"
             
        }
      }
    }
        
    stage("start daily build errai") {
      steps {
        build job: "errai-kieAllBuild-${kieMainBranch}", parameters: [[$class: 'StringParameterValue', name: 'erraiVersionOld', value: erraiVersionOld],
        [$class: 'StringParameterValue', name: 'erraiVersionNew', value: erraiVersionNew],[$class: 'StringParameterValue', name: 'erraiBranch', value: erraiBranch, name: 'kieMainBranch', value: kieMainBranch]]                    
      }
    } 
        
    stage('start daily kieAllBuilds for community and product') {
      steps {
        parallel (
          "communityBuild" : {
            build job: "kieAllBuild-${kieMainBranch}", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion],
              [$class: 'StringParameterValue', name: 'erraiVersionNew', value: erraiVersionNew],[$class: 'StringParameterValue', name: 'appformerVersion', value: appformerVersion],
              [$class: 'StringParameterValue', name: 'kieMainBranch', value: kieMainBranch]]                    
          },
          "productBuild" : {
            build job: "prod-kieAllBuild-${kieMainBranch}", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieProdVersion', value: kieProdVersion],
            [$class: 'StringParameterValue', name: 'erraiVersionNew', value: erraiVersionNew], [$class: 'StringParameterValue', name: 'appformerProdVersion', value: appformerProdVersion],
            [$class: 'StringParameterValue', name: 'kieProdBranch',value: kieProdBranch], [$class: 'StringParameterValue', name: 'kieMainBranch', value: kieMainBranch]]                
          }
        )
      }
    }
        
    stage('additional daily tests') {
      steps {
        parallel (
          "jbpmTestCoverageMatrix" : {
              build job: "jbpmTestCoverageMatrix-kieAllBuild-${kieMainBranch}", parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'kieMainBranch', value: kieMainBranch]]
          },
          "jbpmTestContainerMatrix" : {
              build job: "jbpmTestContainerMatrix-kieAllBuild-${kieMainBranch}", parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'kieMainBranch', value: kieMainBranch]]
          },
          "kieWbTestsMatrix" : {
            build job: "kieWbTestsMatrix-kieAllBuild-${kieMainBranch}", parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'kieMainBranch', value: kieMainBranch]]
          },
          "kieServerMatrix" : {
            build job: "kieServerMatrix-kieAllBuild-${kieMainBranch}", parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'kieMainBranch', value: kieMainBranch]]
          },
          "kie-docker-ci-images" : {
            build job: "${dockerPath}/kie-docker-ci-images-${kieMainBranch}", parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion]]
          }
        )    
      } 
    }
  }
}'''

pipelineJob("${folderPath}/kieAllBuildPipeline-${kieMainBranch}") {

    disabled()
    
    description('this is a pipeline job that triggers all other jobs with it\'s parameters needed for the kieAllBuild')
    

    parameters{
        stringParam("kieVersion", "${kieVersion}", "Version of kie. This will be usually set automatically by the parent pipeline job. ")
        stringParam("kieProdBranch", "${kieProdBranch}", "The prod branch will get this value in it's name: bsync-value-date. " )
        stringParam("appformerVersion", "${appformerVersion}", "Version of appformer. This will be usually set automatically by the parent pipeline job. ")
        stringParam("erraiVersionOld", "${erraiVersionOld}", "Old version of errai. This will be usually set automatically by the parent pipeline job. ")
        stringParam("erraiVersionNew", "${erraiVersionNew}", "New version of errai. This will be usually set automatically by the parent pipeline job. ")
        stringParam("kieMainBranch", "${kieMainBranch}", "kie branch. This will be usually set automatically by the parent pipeline job. ")
        stringParam("erraiBranch", "${erraiBranch}", "errai branch. This will be usually set automatically by the parent pipeline job. ")
        stringParam("organization", "${organization}", "Name of organization. This will be usually set automatically by the parent pipeline job. ")
        stringParam("dockerPath", "${dockerPath}", "Path to kie-docker-ci-images-${kieMainBranch}. This will be usually set automatically by the parent pipeline job. ")
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
            script("${kieAllpipeline}")
        }
    }

    publishers {
        buildDescription ("KIE version ([^\\s]*)")
    }

}


// ++++++++++++++++++++++++++++++++++++++++++ Build and deploys errai ++++++++++++++++++++++++++++++++++++++++++++++++++

// definition of errai script
def erraiVersionBuild='''#!/bin/bash -e
# removing UF and errai artifacts from local maven repo (basically all possible SNAPSHOTs)
if [ -d $MAVEN_REPO_LOCAL ]; then
rm -rf $MAVEN_REPO_LOCAL/org/jboss/errai/
       fi
# clone the Errai repository
git clone https://github.com/errai/errai.git -b $erraiBranch --depth 100
# checkout the release branch
cd errai
git checkout -b $erraiVersionNew $erraiBranch
# update versions
sh updateVersions.sh $erraiVersionOld $erraiVersionNew
# build the repos & deploy into local dir (will be later copied into staging repo)
deployDir=$WORKSPACE/deploy-dir
# do a full build, but deploy only into local dir
# we will deploy into remote staging repo only once the whole build passed (to save time and bandwith)
mvn -U -B -e clean deploy -Dfull -Drelease -DaltDeploymentRepository=local::default::file://$deployDir -s $SETTINGS_XML_FILE\\
 -Dmaven.test.failure.ignore=true -Dgwt.compiler.localWorkers=3
# unpack zip to QA Nexus
cd $deployDir
zip -r kiegroup .
curl --silent --upload-file kiegroup.zip -u $kieUnpack -v http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/service/local/repositories/kieAllBuild-$kieMainBranch/content-compressed
'''


job("${folderPath}/errai-kieAllBuild-${kieMainBranch}") {
    description("Upgrades and builds the errai version")
    parameters{
        stringParam("erraiVersionNew", "${erraiVersionNew}", "Version of Errai. This will be usually set automatically by the parent trigger job. ")
        stringParam("erraiVersionOld", "${erraiVersionOld}", "Version of Errai. This will be usually set automatically by the parent trigger job. ")
        stringParam("erraiBranch", "${erraiBranch}", "Branch of errai. This will be usually set automatically by the parent trigger job. ")
        stringParam("kieMainBranch", "${kieMainBranch}", "Branch of kie reps. This will be usually set automatically by the parent trigger job. ")
    }

    label("kie-rhel7&&kie-mem4g")

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
            mavenSettings("771ff52a-a8b4-40e6-9b22-d54c7314aa1e"){
                variable("SETTINGS_XML_FILE")
                targetLocation("jenkins-settings.xml")
            }
        }
        credentialsBinding {
          usernamePassword("kieUnpack" , "unpacks-zip-on-qa-nexus")
        }
    }

    publishers {
        archiveJunit("**/target/*-reports/TEST-*.xml")
        mailer('mbiarnes@redhat.com', false, false)
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
        shell(erraiVersionBuild)
    }

}

// +++++++++++++++++++++++++++++++++++++++++++ Build and deploy kie ++++++++++++++++++++++++++++++++++++++++++++++++++++

// definition of kie build  script
def kieVersionBuild='''#!/bin/bash -e
# removing KIE artifacts from local maven repo (basically all possible SNAPSHOTs)
if [ -d $MAVEN_REPO_LOCAL ]; then
    rm -rf $MAVEN_REPO_LOCAL/org/jboss/tools/
    rm -rf $MAVEN_REPO_LOCAL/org/kie/
    rm -rf $MAVEN_REPO_LOCAL/org/drools/
    rm -rf $MAVEN_REPO_LOCAL/org/jbpm/
    rm -rf $MAVEN_REPO_LOCAL/org/optaplanner/
    rm -rf $MAVEN_REPO_LOCAL/org/uberfire/
    rm -rf $MAVEN_REPO_LOCAL/org/dashbuilder/
fi
# clone the build-bootstrap that contains the other build scripts
git clone https://github.com/kiegroup/droolsjbpm-build-bootstrap.git --branch $kieMainBranch --depth 100
# clone rest of the repos
./droolsjbpm-build-bootstrap/script/git-clone-others.sh --quiet --branch $kieMainBranch --depth 100
# checkout to release branches
./droolsjbpm-build-bootstrap/script/git-all.sh checkout -b $kieVersion $kieMainBranch

# upgrade version kiegroup 
./droolsjbpm-build-bootstrap/script/release/update-version-all.sh $kieVersion $appformerVersion custom
echo "errai version:" $erraiVersionNew
echo "appformer version:" $appformerVersion
echo "kie version" $kieVersion
# change properties via sed as they don't update automatically
# appformer
cd appformer
sed -i "$!N;s/<version.org.kie>.*.<\\/version.org.kie>/<version.org.kie>$kieVersion<\\/version.org.kie>/;P;D" pom.xml
sed -i "$!N;s/<version.org.jboss.errai>.*.<\\/version.org.jboss.errai>/<version.org.jboss.errai>$erraiVersionNew<\\/version.org.jboss.errai>/;P;D" pom.xml
cd ..
#droolsjbpm-build-bootstrap
cd droolsjbpm-build-bootstrap
sed -i "$!N;s/<version.org.kie>.*.<\\/version.org.kie>/<version.org.kie>$kieVersion<\\/version.org.kie>/;P;D" pom.xml
sed -i "$!N;s/<version.org.uberfire>.*.<\\/version.org.uberfire>/<version.org.uberfire>$appformerVersion<\\/version.org.uberfire>/;P;D" pom.xml
sed -i "$!N;s/<version.org.jboss.errai>.*.<\\/version.org.jboss.errai>/<version.org.jboss.errai>$erraiVersionNew<\\/version.org.jboss.errai>/;P;D" pom.xml
sed -i "$!N;s/<latestReleasedVersionFromThisBranch>.*.<\\/latestReleasedVersionFromThisBranch>/<latestReleasedVersionFromThisBranch>$kieVersion<\\/latestReleasedVersionFromThisBranch>/;P;D" pom.xml
cd ..

# build the repos & deploy into local dir (will be later copied into staging repo)
deployDir=$WORKSPACE/deploy-dir

cat > "$WORKSPACE/clean-up.sh" << EOT
cd \\$1
# Add test reports to the index to prevent their removal in the following step
git add --force **target/*-reports/TEST-*.xml
# Remove all build artifacts to save space
git clean -ffdx
EOT

# do a full build, but deploy only into local dir
# we will deploy into remote staging repo only once the whole build passed (to save time and bandwith)
./droolsjbpm-build-bootstrap/script/mvn-all.sh -B -e -U clean deploy -Dfull -Drelease -DaltDeploymentRepository=local::default::file://$deployDir -s $SETTINGS_XML_FILE\\
 -Dkie.maven.settings.custom=$SETTINGS_XML_FILE -Dmaven.test.redirectTestOutputToFile=true -Dmaven.test.failure.ignore=true\\
 -Dgwt.memory.settings="-Xmx10g" --clean-up-script="$WORKSPACE/clean-up.sh"

# unpack zip to QA Nexus
cd $deployDir
zip -r kiegroup .
curl --silent --upload-file kiegroup.zip -u $kieUnpack -v http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/service/local/repositories/kieAllBuild-$kieMainBranch/content-compressed
cd ..

# creates a file (list) of the last commit hash of each repository as handover for production
./droolsjbpm-build-bootstrap/script/git-all.sh log -1 --pretty=oneline >> git-commit-hashes.txt
echo $kieVersion > $WORKSPACE/version.txt
# creates JSON file for prod
# resultant sed extraction files
./droolsjbpm-build-bootstrap/script/git-all.sh log -1 --format=%H  >> sedExtraction_1.txt
sed -e '1d;2d' -e '/Total/d' -e '/====/d' -e 's/Repository: //g' -e 's/^/"/; s/$/"/;' -e '/""/d' sedExtraction_1.txt >> sedExtraction_2.txt
sed -e '0~2 a\\' sedExtraction_2.txt >> sedExtraction_3.txt
sed -e '1~2 s/$/,/g' sedExtraction_3.txt >> sedExtraction_4.txt
sed -e '1~2 s/^/"repo": /' sedExtraction_4.txt >> sedExtraction_5.txt
sed -e '2~2 s/^/"commit": /' sedExtraction_5.txt >> sedExtraction_6.txt
sed -e '0~2 s/$/\\n },{/g' sedExtraction_6.txt >> sedExtraction_7.txt
sed -e '$d' sedExtraction_7.txt >> sedExtraction_8.txt
cat sedExtraction_8.txt
cutOffDate=$(date +"%m-%d-%Y %H:%M")
reportDate=$(date +"%m-%d-%Y")
fileToWrite=$reportDate.json
commitHash=$(cat sedExtraction_8.txt)
cat <<EOF > int.json
{
   "handover" : {
   "cut_off_date" : "$cutOffDate",
   "report_date": "$reportDate",
   "repos" : [
      {
         $commitHash
      }   
    ],
   "source_product_tag":"$sourceProductTag",
   "target_product_build":"$targetProductBuild" 
   }
}
EOF
# indent json
python -m json.tool int.json >> $fileToWrite
# remove sed extraction and int files
rm sedExtraction*
rm int.json
'''

job("${folderPath}/kieAllBuild-${kieMainBranch}") {
    description("Upgrades and builds the kie version")

    parameters{
        stringParam("erraiVersionNew", "${erraiVersionNew}", "Version of errai. This will be usually set automatically by the parent trigger job. ")
        stringParam("appformerVersion", "${appformerVersion}", "Version of appformer. This will be usually set automatically by the parent trigger job. ")
        stringParam("kieVersion", "${kieVersion}", "Version of kie. This will be usually set automatically by the parent trigger job. ")
        stringParam("kieMainBranch", "${kieMainBranch}", "branch of kie. This will be usually set automatically by the parent trigger job. ")
    }

    label("kie-linux&&kie-rhel7&&kie-mem24g")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            elastic(250, 3, 900)
        }
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
        preBuildCleanup()
        configFiles {
            mavenSettings("771ff52a-a8b4-40e6-9b22-d54c7314aa1e"){
                variable("SETTINGS_XML_FILE")
                targetLocation("jenkins-settings.xml")
            }
        }
        credentialsBinding {
            usernamePassword("kieUnpack" , "unpacks-zip-on-qa-nexus")
        }
    }

    publishers {
        archiveJunit("**/target/*-reports/TEST-*.xml")
        archiveArtifacts{
            onlyIfSuccessful(false)
            allowEmpty(true)
            pattern("**/git-commit-hashes.txt, version.txt, **/hs_err_pid*.log, **/target/*.log, **/*.json")
        }
        mailer('bsig@redhat.com', false, false)
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
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", JAVA_HOME : "\$${javaHome}", MAVEN_REPO_LOCAL : "${m2Dir}", JENKINS_SETTINGS_XML_FILE : "\$SETTINGS_XML_FILE" , PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(kieVersionBuild)
    }
}

// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// definition of prod-build

def kieProdBuild='''#!/bin/bash -e
echo "kieProdVersion:" $kieProdVersion
echo "kieProdBranch:" $kieProdBranch
echo "appformerProdVersion:" $appformerProdVersion
echo "erraiVersionNew:" $erraiVersionNew
echo "kieMainBranch:" $kieMainBranch

# removing KIE artifacts from local maven repo (basically all possible SNAPSHOTs)
if [ -d $MAVEN_REPO_LOCAL ]; then
    rm -rf $MAVEN_REPO_LOCAL/org/jboss/tools/
    rm -rf $MAVEN_REPO_LOCAL/org/kie/
    rm -rf $MAVEN_REPO_LOCAL/org/drools/
    rm -rf $MAVEN_REPO_LOCAL/org/jbpm/
    rm -rf $MAVEN_REPO_LOCAL/org/optaplanner/
    rm -rf $MAVEN_REPO_LOCAL/org/uberfire/
    rm -rf $MAVEN_REPO_LOCAL/org/dashbuilder/
fi

#switch to the right droolsjbpm-build-bootstrap master branch
cd $WORKSPACE/droolsjbpm-build-bootstrap
git checkout $kieMainBranch

# cd into the workspace where droolsjbpm-build-bootstrap is
cd $WORKSPACE

# clone rest of the repos
./droolsjbpm-build-bootstrap/script/git-clone-others.sh --quiet --branch $kieMainBranch --depth 100
# checkout to release branches
./droolsjbpm-build-bootstrap/script/git-all.sh checkout -b $kieProdBranch $kieMainBranch

# upgrade version kiegroup 
./droolsjbpm-build-bootstrap/script/release/update-version-all.sh $kieProdVersion $appformerProdVersion custom

# change properties via sed as they don't update automatically
# appformer
cd appformer
sed -i "$!N;s/<version.org.kie>.*.<\\/version.org.kie>/<version.org.kie>$kieProdVersion<\\/version.org.kie>/;P;D" pom.xml
sed -i "$!N;s/<version.org.jboss.errai>.*.<\\/version.org.jboss.errai>/<version.org.jboss.errai>$erraiVersionNew<\\/version.org.jboss.errai>/;P;D" pom.xml
cd ..
#droolsjbpm-build-bootstrap
cd droolsjbpm-build-bootstrap
sed -i "$!N;s/<version.org.kie>.*.<\\/version.org.kie>/<version.org.kie>$kieProdVersion<\\/version.org.kie>/;P;D" pom.xml
sed -i "$!N;s/<version.org.uberfire>.*.<\\/version.org.uberfire>/<version.org.uberfire>$appformerProdVersion<\\/version.org.uberfire>/;P;D" pom.xml
sed -i "$!N;s/<version.org.jboss.errai>.*.<\\/version.org.jboss.errai>/<version.org.jboss.errai>$erraiVersionNew<\\/version.org.jboss.errai>/;P;D" pom.xml
sed -i "$!N;s/<latestReleasedVersionFromThisBranch>.*.<\\/latestReleasedVersionFromThisBranch>/<latestReleasedVersionFromThisBranch>$kieProdVersion<\\/latestReleasedVersionFromThisBranch>/;P;D" pom.xml
cd ..

# git add and commit changes
./droolsjbpm-build-bootstrap/script/git-all.sh add .
./droolsjbpm-build-bootstrap/script/git-all.sh commit -m "upgraded version"

cat > "$WORKSPACE/clean-up.sh" << EOT
cd \\$1
# Add test reports to the index to prevent their removal in the following step
git add --force **target/*-reports/TEST-*.xml
# Remove all build artifacts to save space
git clean -ffdx
EOT

# do a full build
./droolsjbpm-build-bootstrap/script/mvn-all.sh -B -e -U clean install -Dfull -Drelease -Dproductized -s $SETTINGS_XML_FILE\\
 -Dkie.maven.settings.custom=$SETTINGS_XML_FILE -Dmaven.test.redirectTestOutputToFile=true -Dmaven.test.failure.ignore=true\\
 -Dgwt.memory.settings="-Xmx10g" --clean-up-script="$WORKSPACE/clean-up.sh"
 
# creates a tarball with all repositories and saves it on Jenkins master
tar czf prodBranches.tgz *
'''

job("${folderPath}/prod-kieAllBuild-${kieMainBranch}") {

    description("Upgrades and builds the prod kie version")

    parameters{
        stringParam("kieProdVersion", "${kieVersion}+<date>+suffix", "Prod kie version. This will be usually set automatically by the parent trigger job. ")
        stringParam("appformerProdVersion", "${appformerVersion}+<date>+suffix", "Prod appformer version (former uberfire version). This will be usually set automatically by the parent trigger job. ")
        stringParam("erraiVersionNew", "${erraiVersionNew}+<date>+suffix", "Errai version. This will be usually set automatically by the parent trigger job. ")
        stringParam("kieMainBranch", "${kieMainBranch}", "Name of kie branch. This will be usually set automatically by the parent trigger job. ")
        stringParam("kieProdBranch", "${kieProdBranch}", "Name of product branch. This will be usually set automatically by the parent trigger job. ")
    }

    scm {
        git {
            remote {
                github("${organization}/droolsjbpm-build-bootstrap")
            }
            branch ("${kieMainBranch}")
            extensions {
                relativeTargetDirectory("droolsjbpm-build-bootstrap")
            }

        }
    }

    label("kie-linux&&kie-rhel7&&kie-mem24g")

    logRotator {
        numToKeep(5)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            elastic(250, 3, 900)
        }
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
        preBuildCleanup()
        configFiles {
            mavenSettings("771ff52a-a8b4-40e6-9b22-d54c7314aa1e"){
                variable("SETTINGS_XML_FILE")
                targetLocation("jenkins-settings.xml")
            }
        }
    }

    publishers {
        archiveJunit("**/target/*-reports/TEST-*.xml")
        archiveArtifacts{
            onlyIfSuccessful(false)
            allowEmpty(true)
            pattern("prodBranches.tgz")
        }
        mailer('mbiarnes@redhat.com pszubiak@redhat.com anstephe@redhat.com', false, false)
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
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", JAVA_HOME : "\$${javaHome}", MAVEN_REPO_LOCAL : "${m2Dir}", JENKINS_SETTINGS_XML_FILE : "\$SETTINGS_XML_FILE" ,PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(kieProdBuild)
    }
}
// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

// definition of jbpmTestCoverageMatrix test
def jbpmTestCoverage='''#!/bin/bash -e
STAGING_REP=kie-internal-group
echo "KIE version: $kieVersion"
# wget the tar.gz sources
wget -q http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/kieAllBuild-$kieMainBranch/org/jbpm/jbpm/$kieVersion/jbpm-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
mv jbpm-$kieVersion/* .
rmdir jbpm-$kieVersion
'''

matrixJob("${folderPath}/jbpmTestCoverageMatrix-kieAllBuild-${kieMainBranch}") {
    description("This job: <br> - Test coverage Matrix for jbpm <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")
    parameters {
        stringParam("kieVersion", "${kieVersion}", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>(7.1.0.20170514-productized) for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
        stringParam("kieMainBranch", "${kieMainBranch}", "please edit the branch of the KIE release <br> Will be supplied by the parent job. <br> Normally the kieMainBranch will be supplied by parent job <br> ******************************************************** <br> ")
    }

    axes {
        labelExpression("label-exp","kie-linux&&kie-mem8g")
        jdk("${javadk}")
    }

    logRotator {
        numToKeep(10)
    }

    wrappers {
        timeout {
            absolute(120)
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
        archiveJunit("**/target/*-reports/TEST-*.xml")
        mailer('mbiarnes@redhat.com', false, false)
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
        shell(jbpmTestCoverage)
        maven{
            mavenInstallation("${mvnVersion}")
            goals("clean verify -e -B -Dmaven.test.failure.ignore=true -Dintegration-tests")
            rootPOM("jbpm-test-coverage/pom.xml")
            mavenOpts("-Xmx3g")
            providedSettings("771ff52a-a8b4-40e6-9b22-d54c7314aa1e")
        }
    }
}

// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// run additional test: jbpmContainerTestMatrix test
def jbpmContainerTest='''#!/bin/bash -e
echo "KIE version $kieVersion"
# wget the tar.gz sources
wget -q http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/kieAllBuild-$kieMainBranch/org/jbpm/jbpm/$kieVersion/jbpm-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
mv jbpm-$kieVersion/* .
rmdir jbpm-$kieVersion
'''

matrixJob("${folderPath}/jbpmTestContainerMatrix-kieAllBuild-${kieMainBranch}") {
    description("Version to test. Will be supplied by the parent job. Also used to donwload proper sources. <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")
    parameters {
        stringParam("kieVersion", "${kieVersion}", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>(7.1.0.20170514-productized) for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
        stringParam("kieMainBranch", "${kieMainBranch}", "please edit the branch of the KIE release <br> Will be supplied by the parent job. <br> Normally the kieMainBranch will be supplied by parent job <br> ******************************************************** <br> ")

    }

    axes {
        labelExpression("label-exp","kie-rhel7&&kie-mem8g")
        jdk("${javadk}")
        text("container", "tomcat9", "wildfly")
    }

    logRotator {
        numToKeep(10)
    }

    childCustomWorkspace("\${SHORT_COMBINATION}")

    wrappers {
        timeout {
            absolute(120)
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
        archiveJunit("**/target/*-reports/TEST-*.xml")
        mailer('mbiarnes@redhat.com', false, false)
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
        shell(jbpmContainerTest)
        maven{
            mavenInstallation("${mvnVersion}")
            goals("-e -B clean install")
            rootPOM("jbpm-container-test/pom.xml")
            mavenOpts("-Xmx3g")
            providedSettings("771ff52a-a8b4-40e6-9b22-d54c7314aa1e")
            properties("maven.test.failure.ignore": true)
            properties("container.profile":"\$container")
            properties("org.apache.maven.user-settings":"\$SETTINGS_XML_FILE")
        }
    }
}

// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  run additional test: kieWbTestsMatrix
def kieWbTest='''#!/bin/bash -e
echo "KIE version $kieVersion"
# wget the tar.gz sources
wget -q http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/kieAllBuild-$kieMainBranch/org/kie/kie-wb-distributions/$kieVersion/kie-wb-distributions-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
mv kie-wb-distributions-$kieVersion/* .
rmdir kie-wb-distributions-$kieVersion'''

matrixJob("${folderPath}/kieWbTestsMatrix-kieAllBuild-${kieMainBranch}") {
    description("This job: <br> - Runs the KIE Server integration tests on mutiple supported containers and JDKs <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

    parameters {
        stringParam("kieVersion", "${kieVersion}", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>(7.1.0.20170514-productized) for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
        stringParam("kieMainBranch", "${kieMainBranch}", "please edit the branch of the KIE release <br> Will be supplied by the parent job. <br> Normally the kieMainBranch will be supplied by parent job <br> ******************************************************** <br> ")

    }

    axes {
        labelExpression("label_exp", "kie-linux&&kie-mem8g&&gui-testing")
        text("container", "wildfly", "eap7")
        text("war","kie-wb","kie-drools-wb")
        jdk("${javadk}")
        text("browser","firefox")
    }

    childCustomWorkspace("\${SHORT_COMBINATION}")

    logRotator {
        numToKeep(10)
    }

    properties{
        rebuild{
            autoRebuild()
        }
    }

    throttleConcurrentBuilds {
        maxPerNode(1)
        maxTotal(5)
        throttleMatrixConfigurations()
    }

    wrappers {
        timeout {
            absolute(120)
        }
        timestamps()
        colorizeOutput()
        preBuildCleanup()
        configFiles {
            mavenSettings("771ff52a-a8b4-40e6-9b22-d54c7314aa1e") {
                variable("SETTINGS_XML_FILE")
                targetLocation("jenkins-settings.xml")
            }
        }
        xvnc{
            useXauthority()
        }
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

    publishers {
        archiveJunit("**/target/*-reports/TEST-*.xml, **/target/screenshots/*")
        mailer('mbiarnes@redhat.com', false, false)
        wsCleanup()
    }

    steps {
        shell(kieWbTest)
        maven{
            mavenInstallation("${mvnVersion}")
            goals("-nsu -B -e -fae clean verify -P\$container,\$war")
            rootPOM("kie-wb-tests/pom.xml")
            properties("maven.test.failure.ignore": true)
            properties("deployment.timeout.millis":"240000")
            properties("container.startstop.timeout.millis":"240000")
            properties("webdriver.firefox.bin":"/opt/tools/firefox-60esr/firefox-bin")
            properties("eap7.download.url":EAP7_DOWNLOAD_URL)
            mavenOpts("-Xms1024m -Xmx1536m")
            providedSettings("771ff52a-a8b4-40e6-9b22-d54c7314aa1e")
        }
    }
}

// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  run additional test: kieServerMatrix
def kieServerTest='''#!/bin/bash -e
echo "KIE version $kieVersion"
# wget the tar.gz sources
wget -q http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/kieAllBuild-$kieMainBranch/org/drools/droolsjbpm-integration/$kieVersion/droolsjbpm-integration-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
mv droolsjbpm-integration-$kieVersion/* .
rmdir droolsjbpm-integration-$kieVersion'''

matrixJob("${folderPath}/kieServerMatrix-kieAllBuild-${kieMainBranch}") {
    description("This job: <br> - Runs the KIE Server integration tests on mutiple supported containers and JDKs <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

    // Label which specifies which nodes this job can run on.
    label("master")

    parameters {
        stringParam("kieVersion", "${kieVersion}", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>(7.1.0.20170514-productized) for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
        stringParam("kieMainBranch", "${kieMainBranch}", "please edit the branch of the KIE release <br> Will be supplied by the parent job. <br> Normally the kieMainBranch will be supplied by parent job <br> ******************************************************** <br> ")

    }

    axes {
        jdk("${javadk}")
        text("container", "wildfly", "eap7", "tomcat9")
        labelExpression("label_exp", "kie-linux&&kie-mem8g")
    }

    childCustomWorkspace("\${SHORT_COMBINATION}")

    logRotator {
        numToKeep(10)
    }

    wrappers {
        timeout {
            absolute(120)
        }
        timestamps()
        colorizeOutput()
        preBuildCleanup()
        configFiles {
            mavenSettings("771ff52a-a8b4-40e6-9b22-d54c7314aa1e") {
                variable("SETTINGS_XML_FILE")
                targetLocation("jenkins-settings.xml")
            }
        }
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

    publishers {
        archiveJunit("**/target/*-reports/TEST-*.xml")
        mailer('mbiarnes@redhat.com', false, false)
        wsCleanup()
    }

    steps {
        shell(kieServerTest)
        maven{
            mavenInstallation("${mvnVersion}")
            goals("-B -e -fae -nsu clean verify -P\$container -Pjenkins-pr-builder")
            rootPOM("kie-server-parent/kie-server-tests/pom.xml")
            properties("kie.server.testing.kjars.build.settings.xml":"\$SETTINGS_XML_FILE")
            properties("maven.test.failure.ignore": true)
            properties("deployment.timeout.millis":"240000")
            properties("container.startstop.timeout.millis":"240000")
            properties("eap7.download.url":EAP7_DOWNLOAD_URL)
            mavenOpts("-Xms1024m -Xmx1536m")
            providedSettings("771ff52a-a8b4-40e6-9b22-d54c7314aa1e")
        }
    }
}

// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  run additional test: kieAlBuild-windows

def windowsTests='''set repo_list=droolsjbpm-build-bootstrap droolsjbpm-knowledge drools optaplanner jbpm droolsjbpm-integration droolsjbpm-tools kie-appformer-extensions guvnor kie-wb-common jbpm-form-modeler drools-wb jbpm-designer jbpm-console-ng optaplanner-wb kie-wb-distributions
for %%x in (%repo_list%) do (
    if "%%x" == "kie-wb-common" (
        rem clone the kie-wb-common into directory with shortest name possible to avoid long path issues
        git clone --depth 10 https://github.com/kiegroup/%%x.git k
    ) else (
        git clone --depth 10 https://github.com/kiegroup/%%x.git
    )
)
for %%x in (%repo_list%) do (
    if "%%x" == "kie-wb-common" (
        c:\\tools\\apache-maven-3.2.5\\bin\\mvn.bat -U -e -B -f k\\pom.xml clean install -Dfull -Dmaven.test.failure.ignore=true -Dgwt.memory.settings="-Xmx2g -Xms1g -XX:MaxPermSize=256m -Xss1M" -Dgwt.compiler.localWorkers=1 || exit \\b
    ) else (
        c:\\tools\\apache-maven-3.2.5\\bin\\mvn.bat -U -e -B -f %%x\\pom.xml clean install -Dfull -Dmaven.test.failure.ignore=true -Dgwt.memory.settings="-Xmx2g -Xms1g -XX:MaxPermSize=256m -Xss1M" -Dgwt.compiler.localWorkers=1 -Dgwt.compiler.skip=true -Dgwt.skipCompilation=true || exit \\b
    )
)'''

job("${folderPath}/windows-kieAllBuild-${kieMainBranch}") {
    disabled ()
    description("Builds all repos specified in\n" +
            "<a href=\"https://github.com/droolsjbpm/droolsjbpm-build-bootstrap/blob/master/script/repository-list.txt\">repository-list.txt</a> (master branch) on Windows machine.\n" +
            "It does not deploy the artifacts to staging repo (or any other remote). It just checks our repositories can be build and tested on Windows, so that \n" +
            "contributors do not hit issues when using Windows machines for development.<br/>\n" +
            "<br/>\n" +
            "<b>Important:</b> the workspace is under c:\\x, instead of c:\\jenkins\\workspace\\kie-all-build-windows-master. This is to decrease the path prefix as much as possible\n" +
            "to avoid long path issues on Windows (limit there is 260 chars).")

    label("windows")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            absolute(300)
        }
        timestamps()
        colorizeOutput()
        preBuildCleanup()
    }

    triggers {
        cron("H 22 * * *")
    }

    publishers {
        archiveJunit("**/target/*-reports/TEST-*.xml")
        mailer('mbiarnes@redhat.com', false, false)
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
            envs(MAVEN_OPTS : "-Xms2g -Xmx3g")
        }
        shell(windowsTests)
    }


}
// *****************************************************************************************************
// definition of kieDockerCi  script

def kieDockerCi='''
sh scripts/docker-clean.sh $kieVersion
sh scripts/update-versions.sh $kieVersion -s "$SETTINGS_XML"'''

job("${dockerPath}/kie-docker-ci-images-${kieMainBranch}") {
    description("Builds CI Docker images for master branch. <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

    parameters {
        stringParam("kieVersion", "${kieVersion}-SNAPSHOT", "Please edit the version of the kie release <br> i.e. typically <b> major.minor.micro.EXT </b>i.e. 8.0.0.Beta1<br> Normally the kie version will be supplied by parent job <br> ******************************************************** <br> ")
    }

    scm {
        git {
            remote {
                github("${organization}/kie-docker-ci-images")
            }
            branch ("${kieMainBranch}")
        }
    }

    label("kieci-02-docker")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            absolute(120)
        }
        timestamps()
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
        colorizeOutput()
        preBuildCleanup()
        configFiles {
            mavenSettings("3ebb89ff-985c-43a2-965d-1cde56f31e1a"){
                targetLocation("\$WORKSPACE/settings.xml")
                variable("SETTINGS_XML")
            }
        }
    }

    publishers {
        mailer('mbiarnes@redhat.com', false, false)
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
            envs(MAVEN_HOME : "\$${mvnHome}", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(kieDockerCi)
        maven{
            mavenInstallation("${mvnVersion}")
            goals("-e -B -U clean install")
            providedSettings("3ebb89ff-985c-43a2-965d-1cde56f31e1a")
            properties("kie.artifacts.deploy.path":"/home/docker/kie-artifacts/\$kieVersion")
        }
    }
}


