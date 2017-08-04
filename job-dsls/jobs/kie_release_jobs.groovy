//Define Variables

def kieVersion="8.0.x"
def uberfireVersion="2.0.x"
def dashbuilderVersion="1.0.x"
def javadk="jdk1.8"
def jaydekay="JDK1_8"
def mvn="APACHE_MAVEN_3_3_9"
def mvnHome="${mvn}_HOME"
def mvnOpts="-Xms2g -Xmx3g"
def kieMainBranch="master"
def uberfireBranch="master"
def dashbuilderBranch="master"
def organization="kiegroup"
def uberfireOrganization="AppFormer"
def dashbuilderOrganization="dashbuilder"


//def pushReleaseBranches ="""
//sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-createAndPushReleaseBranches.sh
//"""

def deployLocally="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-deployLocally.sh
"""

def copyToNexus="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-copyBinariesToNexus.sh
"""

def jbpmTestCoverageMatrix="""
git clone https://github.com/kiegroup/droolsjbpm-build-bootstrap.git -b ${kieMainBranch}
sh \$WORKSPACE/droolsjbpm-build-bootstrap/script/release/kie-jbpmTestCoverMartix.sh
"""

def kieAllServerMatrix="""
git clone https://github.com/kiegroup/droolsjbpm-build-bootstrap.git -b ${kieMainBranch}
sh \$WORKSPACE/droolsjbpm-build-bootstrap/script/release/kie-allServerMatrix.sh
"""

def kieWbSmokeTestsMatrix="""
git clone https://github.com/kiegroup/droolsjbpm-build-bootstrap.git -b ${kieMainBranch}
sh \$WORKSPACE/droolsjbpm-build-bootstrap/script/release/kie-wbSmokeTestsMatrix.sh
"""

def pushTags="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-pushTag.sh
"""

def removeBranches="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-removeReleaseBranches.sh
"""

def updateVersions="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-updateToNextDevelopmentVersion.sh
"""

def copyBinariesToFilemgmt="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-copyBinariesToFilemgmt.sh
"""

def ufDeploy="""
sh \$WORKSPACE/scripts/uberfire/scripts/release/uberfire-createAndDeploy.sh
"""

def ufPushTag="""
sh \$WORKSPACE/scripts/uberfire/scripts/release/uberfire-pushTag.sh
"""

def ufUpdateVersion="""
sh \$WORKSPACE/scripts/uberfire/scripts/release/uberfire-updateVersion.sh
"""

def dashDeploy="""
sh \$WORKSPACE/scripts/dashbuilder/scripts/release/dashbuilder-createAndDeploy.sh
"""

def dashPushTag="""
sh \$WORKSPACE/scripts/dashbuilder/scripts/release/dashbuilder-pushTag.sh
"""

def dashUpdateVersion="""
sh \$WORKSPACE/scripts/dashbuilder/scripts/release/dashbuilder-updateVersion.sh
"""


// **************************************************************************
def pushReleaseBranches='''#!/bin/bash -e

# clone rest of the repos
./droolsjbpm-build-bootstrap/script/git-clone-others.sh --branch $baseBranch --depth 70

if [ "$source" == "community-branch" ]; then

   # checkout to local release names
   ./droolsjbpm-build-bootstrap/script/git-all.sh checkout -b $releaseBranch $baseBranch

   # add new remote pointing to jboss-integration
   ./droolsjbpm-build-bootstrap/script/git-add-remote-jboss-integration.sh

fi

if [ "$source" == "community-tag" ]; then

   # add new remote pointing to jboss-integration
   ./droolsjbpm-build-bootstrap/script/git-add-remote-jboss-integration.sh

   # get the tags of community
   ./droolsjbpm-build-bootstrap/script/git-all.sh fetch --tags origin

   # checkout to local release names
   ./droolsjbpm-build-bootstrap/script/git-all.sh checkout -b $releaseBranch $tag

fi

if [ "$source" == "production-tag" ]; then

   # add new remote pointing to jboss-integration
   ./droolsjbpm-build-bootstrap/script/git-add-remote-jboss-integration.sh

   # get the tags of jboss-integration
   ./droolsjbpm-build-bootstrap/script/git-all.sh fetch --tags jboss-integration

   # checkout to local release names
   ./droolsjbpm-build-bootstrap/script/git-all.sh checkout -b $releaseBranch $tag

fi

# upgrades the version to the release/tag version
./droolsjbpm-build-bootstrap/script/release/update-version-all.sh $releaseVersion $target

# update kie-parent-metadata
cd droolsjbpm-build-bootstrap/

# change properties via sed as they don't update automatically
sed -i \\
-e "$!N;s/<version.org.uberfire>.*.<\\/version.org.uberfire>/<version.org.uberfire>$uberfireVersion<\\/version.org.uberfire>/;" \\
-e "s/<version.org.dashbuilder>.*.<\\/version.org.dashbuilder>/<version.org.dashbuilder>$dashbuilderVersion<\\/version.org.dashbuilder>/;" \\
-e "s/<version.org.jboss.errai>.*.<\\/version.org.jboss.errai>/<version.org.jboss.errai>$erraiVersion<\\/version.org.jboss.errai>/;" \\
-e "s/<latestReleasedVersionFromThisBranch>.*.<\\/latestReleasedVersionFromThisBranch>/<latestReleasedVersionFromThisBranch>$releaseVersion<\\/latestReleasedVersionFromThisBranch>/;P;D" \\
pom.xml

cd $WORKSPACE

# git add and commit the version update changes
./droolsjbpm-build-bootstrap/script/git-all.sh add .
commitMsg="Upgraded versions for release $releaseVersion"
./droolsjbpm-build-bootstrap/script/git-all.sh commit -m "$commitMsg"

# pushes the local release branches to kiegroup or to jboss-integration [IMPORTANT: "push -n" (--dryrun) should be replaced by "push" when script will be in production]
if [ "$target" == "community" ]; then
  ./droolsjbpm-build-bootstrap/script/git-all.sh push -n origin $releaseBranch
else
  ./droolsjbpm-build-bootstrap/script/git-all.sh push -n jboss-integration $releaseBranch
  ./droolsjbpm-build-bootstrap/script/git-all.sh push -n jboss-integration $baseBranch
fi '''

job("createAndPushReleaseBranches-kieReleases-${kieVersion}") {

    description("This job: <br> checksout the right source- upgrades the version in poms <br> - modifies the kie-parent-metadata pom <br> - pushes the generated release branches to kiegroup <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community: <b> community </b><br>or<br> if it is for building a productization tag: <b>productized <br> ******************************************************** <br> ")
        choiceParam("source", ["community-branch", "community-tag", "production-tag"], " please select the source of this release <br> or it is the master branch ( <b> community-branch </b> ) <br> or a community tag ( <b> community-tag </b> ) <br> or a productization tag ( <b> production-tag </b> ) <br> ******************************************************** <br> ")
        stringParam("tag", "tag", "if you selected as <b> source=community-tag </b> or <b> source=production-tag </b> please edit the name of the tag <br> if selected as <b> source=community-branch </b> the parameter <b> tag </b> will be ignored <br> The tag should typically look like <b> major.minor.micro.<extension></b> for <b> community </b> or <b> sync-major.minor.x-<yyyy.mm.dd> </b> for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("releaseVersion", "release version", "please edit the version for this release <br> The <b> releaseVersion </b> should typically look like <b> major.minor.micro.<extension></b> for <b> community </b> or <b> major.minor.micro.<yyymmdd>-productization</b> for <b> productization </b> <br>******************************************************** <br> ")
        stringParam("baseBranch", "base branch", "please select the base branch <br> ******************************************************** <br> ")
        stringParam("releaseBranch", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b>for <b> community </b>or <b> bsync-major.minor.x-<yyyy.mm.dd>  </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("uberfireVersion", "uberfire version", "please edit the right version to use of uberfire<br> The tag should typically look like <b> major.minor.micro.<extension> </b> for <b> community </b> or <b> related kie major.minor.micro.<yyymmdd>-productized </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("dashbuilderVersion", "dashbuilder version", "please edit the right version to use of dashbuilder <br> The tag should typically look like <b> major.minor.micro.<extension>  </b>for <b> community </b> or <b> related kie major.minor.micro.<yyymmdd>-productized </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("erraiVersion", "errai version", " please edit the related errai version<br> ******************************************************** <br> ")
    };

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

    // label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${mvn}", "${jaydekay}")
        preBuildCleanup()
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
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(pushReleaseBranches)
    }
}

// **************************************************************************************

job("buildAndDeployLocally-kieReleases-${kieVersion}") {

    description("This job: <br> - builds all repositories and deploys them locally <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community <b> community </b> or <br> if it is for building a productization tag <b>productized <br> ******************************************************** <br> ")
        stringParam("releaseBranch", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension></b> for <b> community </b>or <b> bsync-major.minor.x-<yyyy.mm.dd>  </b>for <b> productization </b> <br> ******************************************************** <br> ")
    };

    scm {
        git {
            remote {
                github("${organization}/droolsjbpm-build-bootstrap")
            }
            branch ("${kieMainBranch}")
            extensions {
                relativeTargetDirectory("scripts/droolsjbpm-build-bootstrap")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    publishers {
        archiveJunit("**/TEST-*.xml")
    }

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${mvn}", "${jaydekay}")
        preBuildCleanup()
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
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(deployLocally)
    }
}

// ********************************************************************************

job("copyBinariesToNexus-kieReleases-${kieVersion}") {

    description("This job: <br> - copies binaries from local dir to Nexus <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community: <b> community </b> or <br> if it is for building a productization tag: <b>productized <br> ******************************************************** <br> ")
    };

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    customWorkspace("\$HOME/workspace/buildAndDeployLocally-kieReleases-${kieVersion}")

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${mvn}", "${jaydekay}")
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

    publishers{
        downstreamParameterized {
            trigger("jbpmTestCoverageMatrix-kieReleases-${kieVersion}, serverMatrix-kieReleases-${kieVersion}, wbSmokeTestsMatrix-kieReleases-${kieVersion}") {
                condition("SUCCESS")
                parameters {
                    propertiesFile("kie.properties", true)
                }
            }
        }
    }

    steps {
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(copyToNexus)
    }


}

// **************************************************************************************

matrixJob("jbpmTestCoverageMatrix-kieReleases-${kieVersion}") {

    description("This job: <br> - Test coverage Matrix for jbpm <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")
    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community <b> community: </b> or <br> if it is for building a productization tag: <b>productized <br> Version to test. Will be supplied by the parent job. <br> ******************************************************** <br> ")
        stringParam("releaseVersion", "KIE release version", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
    };

    axes {
        labelExpression("label-exp","linux&&mem4g")
        jdk("jdk1.8")
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
    }

    publishers {
        archiveJunit("**/TEST-*.xml")
        mailer('mbiarnes@redhat.com', false, false)
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
        shell(jbpmTestCoverageMatrix)
        maven{
            mavenInstallation("apache-maven-3.2.5")
            goals("clean verify -e -B -Dmaven.test.failure.ignore=true -Dintegration-tests")
            rootPOM("jbpm-test-coverage/pom.xml")
            mavenOpts("-Xmx3g")
            providedSettings("org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1438340407905")
        }
    }
}

// **********************************************************************************

matrixJob("serverMatrix-kieReleases-${kieVersion}") {
    description("This job: <br> - Runs the KIE Server integration tests on mutiple supported containers and JDKs <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

    parameters {
        choiceParam("target", ["community", "productized"], "<br> ******************************************************** <br> ")
        stringParam("releaseVersion", "KIE release version", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
    };

    axes {
        jdk("jdk1.8")
        text("container", "tomcat8", "wildfly10")
        labelExpression("label_exp", "linux&&mem4g")
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
            mavenSettings("org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1438340407905"){
                variable("SETTINGS_XML_FILE")
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
        archiveJunit("**/target/failsafe-reports/TEST-*.xml")
        mailer('mbiarnes@redhat.com', false, false)
    }

    steps {
        shell(kieAllServerMatrix)
        maven{
            mavenInstallation("apache-maven-3.2.5")
            goals("-B -U -e -fae clean verify -P\$container")
            rootPOM("kie-server-parent/kie-server-tests/pom.xml")
            properties("kie.server.testing.kjars.build.settings.xml":"\$SETTINGS_XML_FILE")
            properties("maven.test.failure.ignore": true)
            properties("deployment.timeout.millis":"240000")
            properties("container.startstop.timeout.millis":"240000")
            properties("eap64x.download.url":"http://download.devel.redhat.com/released/JBEAP-6/6.4.4/jboss-eap-6.4.4-full-build.zip")
            mavenOpts("-Xms1024m -Xmx1536m")
            providedSettings("org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1438340407905")
        }
    }
}

// ****************************************************************************************************

matrixJob("wbSmokeTestsMatrix-kieReleases-${kieVersion}") {
    description("This job: <br> - Runs the smoke tests on KIE <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

    parameters {
        choiceParam("target", ["community", "productized"], "<br> ******************************************************** <br> ")
        stringParam("releaseVesion", "KIE release version", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
    };

    axes {
        jdk("jdk1.8")
        text("container", "wildfly10", "tomcat8", "eap7")
        text("war", "kie-wb", "kie-drools-wb")
        labelExpression("label_exp", "linux&&mem8g&&gui-testing")
    }

    childCustomWorkspace("\${SHORT_COMBINATION}")

    properties {
        rebuild {
            autoRebuild()
        }
    }

    logRotator {
        numToKeep(10)
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

    wrappers {
        timeout {
            absolute(120)
        }
        timestamps()
        colorizeOutput()
        preBuildCleanup()
        xvnc {
            useXauthority(true)
        }
    }

    publishers {
        archiveJunit("**/target/failsafe-reports/TEST-*.xml")
        mailer('mbiarnes@redhat.com', false, false)
    }

    steps {
        shell(kieWbSmokeTestsMatrix)
        maven{
            mavenInstallation("apache-maven-3.2.5")
            goals("-B -e -fae clean verify -P\$container,\$war,selenium -D\$TARGET")
            rootPOM("kie-wb-tests/pom.xml")
            properties("maven.test.failure.ignore":true)
            properties("deployment.timeout.millis":"240000")
            properties("container.startstop.timeout.millis":"240000")
            properties("webdriver.firefox.bin":"/opt/tools/firefox-38esr/firefox-bin")
            properties("eap7.download.url":"http://download.eng.brq.redhat.com/released/JBEAP-7/7.0.2/jboss-eap-7.0.2-full-build.zip")
            mavenOpts("-Xms1024m -Xmx1536m")
            providedSettings("org.jenkinsci.plugins.configfiles.maven.MavenSettingsConfig1438340407905")
        }
    }
}

// ************************************************************************************************

job("pushTags-kieReleases-${kieVersion}") {

    description("This job: <br> creates and pushes the tags for <br> community (kiegroup) or product (jboss-integration) <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("tatget", ["community", "productized"], "please select if this release is for community: <b> community </b> or <br> if it is for building a productization tag: <b>productized <br> ******************************************************** <br> ")
        stringParam("releaseBranch", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b>for <b> community </b>or <b> bsync-major.minor.x-<yyy.mm.dd>  </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("tag", "tag", "Please enter the tag. The tag should typically look like <b> major.minor.micro.<extension> </b>for <b> community </b> or <b> sync-major.minor.x-<yyy.mm.dd> </b>for <b> productization </b> <br> ******************************************************** <br> ")
    };

    scm {
        git {
            remote {
                github("${organization}/droolsjbpm-build-bootstrap")
            }
            branch ("${kieMainBranch}")
            extensions {
                relativeTargetDirectory("scripts/droolsjbpm-build-bootstrap")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            absolute(30)
        }
        timestamps()
        preBuildCleanup()
        colorizeOutput()
        toolenv("${mvn}", "${jaydekay}")
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
        mailer('mbiarnes@redhat.com', false, false)
    }

    steps {
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(pushTags)
    }
}

// ***********************************************************************************

job("removeReleaseBranches-kieReleases-${kieVersion}") {

    description("This job: <br> creates and pushes the tags for <br> community (kiegroup) or product (jboss-integration) <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community: <b> community </b> or <br> if it is for building a productization tag: <b>productized <br> ******************************************************** <br> ")
        stringParam("baseBranch", "base branch", "please select the base branch <br> ******************************************************** <br> ")
        stringParam("releaseBranch", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b>for <b> community </b>or <b> bsync-major.minor.x-<yyy.mm.dd> </b>for <b> productization </b> <br> ******************************************************** <br> ")
    };

    scm {
        git {
            remote {
                github("${organization}/droolsjbpm-build-bootstrap")
            }
            branch ("${kieMainBranch}")
            extensions {
                relativeTargetDirectory("scripts/droolsjbpm-build-bootstrap")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            absolute(30)
        }
        timestamps()
        preBuildCleanup()
        colorizeOutput()
        toolenv("${mvn}", "${jaydekay}")
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
        mailer('mbiarnes@redhat.com', false, false)
    }

    steps {
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(removeBranches)
    }
}

// ****************************************************************************************

job("updateToNextDevelopmentVersion-kieReleases-${kieVersion}") {

    description("This job: <br> updates the KIE repositories to a new developmenmt version<br>IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        stringParam("baseBranch","master","Branch you want to upgrade")
        stringParam("newVersion", "new KIE version", "Edit the KIE development version")
        stringParam("uberfireDevelVersion", "uberfire version", "Edit the uberfire development version")
        stringParam("dashbuilderDevelVersion", "dashbuilder version", "Edit the dashbuilder development version")
        stringParam("erraiDevelVersion", "errai version", "Edit the errai development version")
    }

    scm {
        git {
            remote {
                github("${organization}/droolsjbpm-build-bootstrap")
            }
            branch ("${kieMainBranch}")
            extensions {
                relativeTargetDirectory("scripts/droolsjbpm-build-bootstrap")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            absolute(30)
        }
        timestamps()
        preBuildCleanup()
        colorizeOutput()
        toolenv("${mvn}", "${jaydekay}")
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
        mailer('mbiarnes@redhat.com', false, false)
    }

    steps {
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(updateVersions)
    }
}

// ****************************************************************************************

job("copyBinariesToFilemgmt-kieReleases-${kieVersion}") {

    description("This job: <br> copies kiegroup binaries to filemgmt.jbosss.org  <br> IMPORTANT: makes only sense for community releases <br><b> Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.<b>")

    parameters{
        stringParam("version", "release version", "Edit the version of release, i.e. <b>major.minor.micro.<extension></b> ")
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    customWorkspace("\$HOME/workspace/buildAndDeployLocally-kieReleases-${kieVersion}")

    wrappers {
        timeout {
            absolute(120)
        }
        timestamps()
        colorizeOutput()
        toolenv("${mvn}", "${jaydekay}")
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
        mailer('mbiarnes@redhat.com', false, false)
    }

    steps {
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(copyBinariesToFilemgmt)
    }
}

// *************** Uberfire Release scripts *********

job("release-uberfire-${uberfireVersion}") {

    description("This job: <br> releases uberfire, upgrades the version, builds and deploys, copies artifacts to Nexus, closes the release on Nexus  <br> <b>IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.<b>")

    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community <b> community </b> or <br> if it is for building a productization tag <b>productized <br> ******************************************************** <br> ")
        stringParam("baseBranch", "base branch", "please edit the name of the base branch <br> i.e. typically <b> ${uberfireBranch} </b> for <b> community </b><br> ******************************************************** <br> ")
        stringParam("releaseBranch", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b>for <b> community </b>or <b> related kie prod release branch bsync-major.minor.x-<yyyy.mm.dd> </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("newVersion", "new version", "please edit the new version that should be used in the poms <br> The version should typically look like <b> major.minor.micro.<extension> </b>for<b> community </b> or <b> major.minor.micro.<yyyymmdd>-productized </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("erraiVersion", "errai version", " please edit the related errai version<br> ******************************************************** <br> ")
    }

    scm {
        git {
            remote {
                github("${uberfireOrganization}/uberfire")
            }
            branch ("${uberfireBranch}")
            extensions {
                relativeTargetDirectory("scripts/uberfire")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            absolute(60)
        }
        timestamps()
        preBuildCleanup()
        colorizeOutput()
        toolenv("${mvn}", "${jaydekay}")
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
        mailer('mbiarnes@redhat.com', false, false)
    }

    steps {
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(ufDeploy)
    }
}

// ******************************************************

job("pushTag-uberfire-${uberfireVersion}") {

    description("This job: <br> creates and pushes the tags for <br> community (droolsjbpm) or product (jboss-integration) <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community <b> community </b> or <br> if it is for building a productization tag <b>productized <br> ******************************************************** <br> ")
        stringParam("releaseBranch", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b> for <b> community </b>or <b>  related kie prod release branch bsync-major.minor.x->yyy.mm.dd> </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("tag", "tag", "The tag should typically look like <b> major.minor.micro.<extension> </b>for <b> community </b> or <b>  related kie prod tag sync-major.minor.x-<yyyy.mm.dd> </b>for <b> productization </b> <br> ******************************************************** <br> ")
    };

    scm {
        git {
            remote {
                github("${uberfireOrganization}/uberfire")
            }
            branch ("${uberfireBranch}")
            extensions {
                relativeTargetDirectory("scripts/uberfire")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            absolute(30)
        }
        timestamps()
        colorizeOutput()
        preBuildCleanup()
        toolenv("${mvn}", "${jaydekay}")
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
        mailer('mbiarnes@redhat.com', false, false)
    }

    steps {
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(ufPushTag)
    }
}

// ******************************************************

job("updateVersion-uberfire-${uberfireVersion}") {

    description("This job: <br> updates the uberfire repository to a new development version <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        stringParam("newVersion", "uberfire development version", "Edit uberfire development version")
        stringParam("baseBranch", "base branch", "please edit the name of the base branch <br> ******************************************************** <br> ")
        stringParam("erraiDevelVersion","errai development version","Edit errai development version")
    }

    scm {
        git {
            remote {
                github("${uberfireOrganization}/uberfire")
            }
            branch ("${uberfireBranch}")
            extensions {
                relativeTargetDirectory("scripts/uberfire")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            absolute(30)
        }
        timestamps()
        colorizeOutput()
        preBuildCleanup()
        toolenv("${mvn}", "${jaydekay}")
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
        mailer('mbiarnes@redhat.com', false, false)
    }

    steps {
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(ufUpdateVersion)
    }
}
// *************** Dashbuilder Release scripts ***********************

job("release-dashbuilder-${dashbuilderVersion}") {

    description("This job: <br> releases dashbuilder, upgrades the version, builds and deploys, copies artifacts to Nexus, closes the release on Nexus  <br> <b>IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.<b>")

    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community <b> community </b> or <br> if it is for building a productization tag <b>productized <br> ******************************************************** <br> ")
        stringParam("baseBranch", "base branch", "please edit the name of the base branch <br> i.e. typically <b> major.minor.x </b>for <b> community </b><br> ******************************************************** <br> ")
        stringParam("releaseBranch", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b>for <b> community </b>or <b> related kie prod release branch bsync-major.minor.x-<yyyy.mm.dd> </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("newVersion", "new version", "please edit the new version that should be used in the poms <br> The version should typically look like <b> major.minor.micro.<extension> </b>for<b> community </b> or <b> major.minor.micro.<yyyymmdd>-productized </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("uberfireVersion", "uberfire version", "please edit the version of uberfire <br> The version should typically look like <b> major.minor.micro.<extension> </b>for <b> community </b> or <b> major.minor.micro.<yyyymmdd>-productized </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("erraiVersion", "errai version", "please select the needed errai version <br> ******************************************************** <br> ")
    }

    scm {
        git {
            remote {
                github("${dashbuilderOrganization}/dashbuilder")
            }
            branch ("${dashbuilderBranch}")
            extensions {
                relativeTargetDirectory("scripts/dashbuilder")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            absolute(60)
        }
        timestamps()
        preBuildCleanup()
        colorizeOutput()
        toolenv("${mvn}", "${jaydekay}")
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
        mailer('mbiarnes@redhat.com', false, false)
    }

    steps {
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(dashDeploy)
    }
}

// ******************************************************

job("pushTag-dashbuilder-${dashbuilderVersion}") {

    description("This job: <br> creates and pushes the tags for <br> community (dashbuilder) or product (jboss-integration) <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community <b> community </b> or <br> if it is for building a productization tag <b>productized <br> ******************************************************** <br> ")
        stringParam("releaseBranch", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b>for <b> community </b>or <b> related kie prod release branch bsync-major.minor.x->yyy.mm.dd> </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("tag", "tag", "The tag should typically look like <b> major.minor.micro.<extension> </b>for <b> community </b> or <b> related kie prod tag sync-major.minor.x-<yyyy.mm.dd> </b>for <b> productization </b> <br> ******************************************************** <br> ")
    };

    scm {
        git {
            remote {
                github("${dashbuilderOrganization}/dashbuilder")
            }
            branch ("${dashbuilderBranch}")
            extensions {
                relativeTargetDirectory("scripts/dashbuilder")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            absolute(30)
        }
        timestamps()
        colorizeOutput()
        preBuildCleanup()
        toolenv("${mvn}", "${jaydekay}")
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
        mailer('mbiarnes@redhat.com', false, false)
    }

    steps {
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(dashPushTag)
    }
}

// ******************************************************

job("updateVersion-dashbuilder-${dashbuilderVersion}") {

    description("This job: <br> updates dashbuilder repository to a new developmenmt version <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        stringParam("newVersion", "new dashbuilder version", "Edit the new dashbuilder version")
        stringParam("baseBranch", "base branch", "please select the base branch <br> ******************************************************** <br> ")
        stringParam("uberfireDevelVersion", "uberfire development version", "Edit the uberfire development version")
        stringParam("erraiDevelVersion", "errai development version", "Edit the errai development version")
    }

    scm {
        git {
            remote {
                github("${dashbuilderOrganization}/dashbuilder")
            }
            branch ("${dashbuilderBranch}")
            extensions {
                relativeTargetDirectory("scripts/dashbuilder")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    wrappers {
        timeout {
            absolute(30)
        }
        timestamps()
        colorizeOutput()
        preBuildCleanup()
        toolenv("${mvn}", "${jaydekay}")
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
        mailer('mbiarnes@redhat.com', false, false)
    }

    steps {
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(dashUpdateVersion)
    }
}


// **************************** VIEW to create on JENKINS CI *******************************************

nestedView("kieReleases-${kieMainBranch}"){
    views{
        listView("kieRelease-${kieVersion}"){
            description("all scripts needed for building a ${kieVersion} KIE Release")
            jobs {
                name("createAndPushReleaseBranches-kieReleases-${kieVersion}")
                name("buildAndDeployLocally-kieReleases-${kieVersion}")
                name("copyBinariesToNexus-kieReleases-${kieVersion}")
                name("jbpmTestCoverageMatrix-kieReleases-${kieVersion}")
                name("serverMatrix-kieReleases-${kieVersion}")
                name("wbSmokeTestsMatrix-kieReleases-${kieVersion}")
                name("pushTags-kieReleases-${kieVersion}")
                name("removeReleaseBranches-kieReleases-${kieVersion}")
                name("updateToNextDevelopmentVersion-kieReleases-${kieVersion}")
                name("copyBinariesToFilemgmt-kieReleases-${kieVersion}")
            }
            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
            }
        }
        listView("uberfireRelease-${uberfireVersion}"){
            description("all scripts needed for building a ${uberfireVersion} uberfire release")
            jobs {
                name("release-uberfire-${uberfireVersion}")
                name("pushTag-uberfire-${uberfireVersion}")
                name("updateVersion-uberfire-${uberfireVersion}")
            }
            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
            }
        }
        listView("dashbuilderRelease-${dashbuilderVersion}"){
            description("all scripts needed for building a ${dashbuilderVersion} dashbuilder release")
            jobs {
                name("release-dashbuilder-${dashbuilderVersion}")
                name("pushTag-dashbuilder-${dashbuilderVersion}")
                name("updateVersion-dashbuilder-${dashbuilderVersion}")
            }
            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
            }
        }
    }
}
