import org.kie.jenkins.jobdsl.Constants

//Define Variables

def javadk=Constants.JDK_VERSION
def javaToolEnv="KIE_JDK1_8"
def mvnToolEnv="KIE_MAVEN_3_5_2"
def mvnVersion="kie-maven-3.5.2"
def mvnHome="${mvnToolEnv}_HOME"
def mvnOpts="-Xms2g -Xmx3g"
def kieMainBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def m2Dir="\$HOME/.m2/repository"
String EAP7_DOWNLOAD_URL = "http://download-ipv4.eng.brq.redhat.com/released/JBoss-middleware/eap7/7.1.4/jboss-eap-7.1.4-full-build.zip"

// creation of folder
folder("KIE")
folder("KIE/${kieMainBranch}")
folder("KIE/${kieMainBranch}/release")

def folderPath="KIE/${kieMainBranch}/release"


def createReleaseBranches="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-createReleaseBranches.sh
"""

def pushReleaseBranches="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-pushReleaseBranches.sh
"""

def deployLocally="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-deployLocally.sh
"""

def copyToNexus="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-copyBinariesToNexus.sh
"""

def auxiliaryReps="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-upgrade-branch-auxiliary-reps.sh
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

def updateVersions="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-updateToNextDevelopmentVersion.sh
"""

def createJbpmInstaller ="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-createJbpmInstaller.sh
"""

def copyBinariesToFilemgmt="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-copyBinariesToFilemgmt.sh
"""

def prodErrai="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/errai-productizedVersion.sh
"""

// **************************************************************************

job("${folderPath}/createProdErraiVersion-kieReleases-${kieMainBranch}") {

    description("This job: <br> checksout the right source- upgrades the version in poms <br> - pushes the generated release branche to gerrit <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        stringParam("erraiBranch", "base branch", "please select the base branch for errai<br> ******************************************************** <br> ")
        stringParam("erraiVersionNew", "new errai version", "please edit the name of the new errai version <br> i.e. typically <b> 4.2.0.yyymmdd-prod </b> <br> ******************************************************** <br> ")
        stringParam("erraiVersionOld", "old errai version", "please edit the name of the old errai version <br> i.e. typically <b> 4.2.0-SNAPSHOT </b> <br> ******************************************************** <br> ")
        stringParam("erraiTag", "tag for errai prod version", " please edit the tag version here for related kie release <br>  i.e. typically <b> sync-<branch>.yyyy.mm.dd </b> ******************************************************** <br> ")
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
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
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
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "${m2Dir}", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(prodErrai)
    }
}

// **************************************************************************

job("${folderPath}/createAndPushReleaseBranches-kieReleases-${kieMainBranch}") {

    description("This job: <br> checksout the right source- upgrades the version in poms <br> - modifies the kie-parent-metadata pom <br> - pushes the generated release branches to kiegroup <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community: <b> community </b><br>or<br> if it is for building a productization tag: <b>productized <br> ******************************************************** <br> ")
        choiceParam("source", ["community-branch", "community-tag", "production-tag"], " please select the source of this release <br> or it is the master branch ( <b> community-branch </b> ) <br> or a community tag ( <b> community-tag </b> ) <br> or a productization tag ( <b> production-tag </b> ) <br> ******************************************************** <br> ")
        stringParam("tag", "tag", "if you selected as <b> source=community-tag </b> or <b> source=production-tag </b> please edit the name of the tag <br> if selected as <b> source=community-branch </b> the parameter <b> tag </b> will be ignored <br> The tag should typically look like <b> major.minor.micro.<extension></b> for <b> community </b> or <b> sync-major.minor.x-<yyyy.mm.dd> </b> for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("releaseVersion", "release version", "please edit the version for this release <br> The <b> releaseVersion </b> should typically look like <b> major.minor.micro.<extension></b> for <b> community </b> or <b> major.minor.micro.<yyymmdd>-productization</b> for <b> productization </b> <br>******************************************************** <br> ")
        stringParam("baseBranch", "base branch", "please select the base branch <br> ******************************************************** <br> ")
        stringParam("releaseBranch", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b>for <b> community </b>or <b> bsync-major.minor.x-<yyyy.mm.dd>  </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("uberfireVersion", "uberfire version", "please edit the right version to use of uberfire<br> The tag should typically look like <b> major.minor.micro.<extension> </b> for <b> community </b> or <b> related kie major.minor.micro.<yyymmdd>-productized </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("erraiVersion", "errai version", " please edit the related errai version<br> ******************************************************** <br> ")
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
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
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
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "${m2Dir}", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(createReleaseBranches)
        shell(pushReleaseBranches)
    }
}

// **************************************************************************************

job("${folderPath}/buildAndDeployLocally-kieReleases-${kieMainBranch}") {

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
        archiveArtifacts{
            onlyIfSuccessful(false)
            allowEmpty(true)
            pattern("**/target/*.log")
        }
    }

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
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
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "${m2Dir}", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(deployLocally)
    }
}

// ********************************************************************************

job("${folderPath}/copyBinariesToNexus-kieReleases-${kieMainBranch}") {

    description("This job: <br> - copies binaries from local dir to Nexus <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community: <b> community </b> or <br> if it is for building a productization tag: <b>productized <br> ******************************************************** <br> ")
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

    customWorkspace("\$HOME/workspace/${folderPath}/buildAndDeployLocally-kieReleases-${kieMainBranch}")

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
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
            trigger("jbpmTestCoverageMatrix-kieReleases-${kieMainBranch}, serverMatrix-kieReleases-${kieMainBranch}, wbSmokeTestsMatrix-kieReleases-${kieMainBranch}") {
                condition("SUCCESS")
                parameters {
                    propertiesFile("kie.properties", true)
                }
            }
        }
    }

    steps {
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "${m2Dir}", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(copyToNexus)
    }


}

// **************************************************************************************

job("${folderPath}/kie-auxiliary-reps-kieReleases-${kieMainBranch}") {

    description("This job: <br> - copies binaries from local dir to Nexus <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        stringParam("newBranch", "name of branch you're going to create", "branch that needs to be branched from master <br> ******************************************************** <br> ")
        stringParam("oldSnapshot", "old SNAPSHOT version", "please edit the old SNAPSHOT version of the branch <br> ******************************************************** <br> ")
        stringParam("newSnapshot", "new SNAPSHOT version", "please edit the new SNAPSHOT version you are going to upgrade to <br> ******************************************************** <br> ")
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
        archiveArtifacts{
            onlyIfSuccessful(false)
            allowEmpty(true)
            pattern("**/target/*.log")
        }
    }

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
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
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "${m2Dir}", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(auxiliaryReps)
    }


}

// **************************************************************************************

matrixJob("${folderPath}/jbpmTestCoverageMatrix-kieReleases-${kieMainBranch}") {

    description("This job: <br> - Test coverage Matrix for jbpm <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")
    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community <b> community: </b> or <br> if it is for building a productization tag: <b>productized <br> Version to test. Will be supplied by the parent job. <br> ******************************************************** <br> ")
        stringParam("kieVersion", "KIE release version", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
    };

    axes {
        labelExpression("label-exp","kie-linux&&kie-mem4g")
        jdk("$javadk")
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
        shell(jbpmTestCoverageMatrix)
        maven{
            mavenInstallation("${mvnVersion}")
            goals("clean verify -e -B -Dmaven.test.failure.ignore=true -Dintegration-tests")
            rootPOM("jbpm-test-coverage/pom.xml")
            mavenOpts("-Xmx3g")
            providedSettings("1461de41-7511-4269-ae02-8eeb01fd059d")
        }
    }
}

// **********************************************************************************

matrixJob("${folderPath}/serverMatrix-kieReleases-${kieMainBranch}") {

    description("This job: <br> - Runs the KIE Server integration tests on mutiple supported containers and JDKs <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

    parameters {
        choiceParam("target", ["community", "productized"], "<br> ******************************************************** <br> ")
        stringParam("kieVersion", "KIE release version", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
    };

    axes {
        jdk("$javadk")
        text("container", "tomcat9", "wildfly")
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
            mavenSettings("1461de41-7511-4269-ae02-8eeb01fd059d"){
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
        archiveJunit("**/target/*-reports/TEST-*.xml")
        mailer('mbiarnes@redhat.com', false, false)
        wsCleanup()
    }

    steps {
        shell(kieAllServerMatrix)
        maven{
            mavenInstallation("${mvnVersion}")
            goals("-B -U -e -fae clean verify -P\$container")
            rootPOM("kie-server-parent/kie-server-tests/pom.xml")
            properties("kie.server.testing.kjars.build.settings.xml":"\$SETTINGS_XML_FILE")
            properties("maven.test.failure.ignore": true)
            properties("deployment.timeout.millis":"240000")
            properties("container.startstop.timeout.millis":"240000")
            properties("eap7.download.url":EAP7_DOWNLOAD_URL)
            mavenOpts("-Xms1024m -Xmx1536m")
            providedSettings("1461de41-7511-4269-ae02-8eeb01fd059d")
        }
    }
}

// ****************************************************************************************************


matrixJob("${folderPath}/wbSmokeTestsMatrix-kieReleases-${kieMainBranch}") {

    description("This job: <br> - Runs the smoke tests on KIE <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

    parameters {
        choiceParam("target", ["community", "productized"], "<br> ******************************************************** <br> ")
        stringParam("kieVersion", "KIE release version", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
    };

    axes {
        jdk("$javadk")
        text("container", "wildfly", "eap7")
        text("war", "business-central")
        labelExpression("label_exp", "kie-linux&&kie-mem8g&&gui-testing")
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
        archiveJunit("**/target/*-reports/TEST-*.xml")
        mailer('mbiarnes@redhat.com', false, false)
        wsCleanup()
    }

    steps {
        shell(kieWbSmokeTestsMatrix)
        maven{
            mavenInstallation("${mvnVersion}")
            goals("-B -e -fae clean verify -P\$container,\$war,selenium -D\$target")
            rootPOM("business-central-tests/pom.xml")
            properties("maven.test.failure.ignore":true)
            properties("deployment.timeout.millis":"240000")
            properties("container.startstop.timeout.millis":"240000")
            properties("webdriver.firefox.bin":"/opt/tools/firefox-60esr/firefox-bin")
            properties("eap7.download.url":EAP7_DOWNLOAD_URL)
            mavenOpts("-Xms1024m -Xmx1536m")
            providedSettings("1461de41-7511-4269-ae02-8eeb01fd059d")
        }
    }
}

// ************************************************************************************************

job("${folderPath}/pushTags-kieReleases-${kieMainBranch}") {


    description("This job: <br> creates and pushes the tags for <br> community (kiegroup) or product (jboss-integration) <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community: <b> community </b> or <br> if it is for building a productization tag: <b>productized <br> ******************************************************** <br> ")
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
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
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
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "${m2Dir}", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(pushTags)
    }
}

// ***********************************************************************************

job("${folderPath}/updateVersion-kieReleases-${kieMainBranch}") {



    description("This job: <br> updates the KIE repositories to a new developmenmt version<br>IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        stringParam("baseBranch","master","Branch you want to upgrade")
        stringParam("kieVersion", "new KIE version", "Edit the KIE development version")
        stringParam("uberfireVersion", "uberfire version", "Edit the uberfire development version")
        stringParam("erraiVersion", "errai version", "Edit the errai development version")
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
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
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
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "${m2Dir}", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(updateVersions)
    }
}

// ****************************************************************************************

job("${folderPath}/create-jbpm-installers-kieReleases-${kieMainBranch}") {

    description("This job: <br> creates the jbpm-installers  <br> IMPORTANT: makes only sense for community releases <br><b> Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.<b>")

    parameters{
        choiceParam("target", ["public", "internal"], "Select the target where the jbpm installer should be available</br> ")
        stringParam("version", "release version", "Edit the version of release, i.e. <b>major.minor.micro.<extension></b> ")
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

    customWorkspace("\$HOME/workspace/${folderPath}/buildAndDeployLocally-kieReleases-${kieMainBranch}")

    wrappers {
        timeout {
            absolute(180)
        }
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
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
        archiveArtifacts{
            onlyIfSuccessful(false)
            allowEmpty(true)
            pattern("**/jbpm-installer*.zip")
        }
        mailer('mbiarnes@redhat.com', false, false)
    }

    steps {
        environmentVariables {
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "${m2Dir}", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(createJbpmInstaller)
    }
}

// ****************************************************************************************

job("${folderPath}/copyBinariesToFilemgmt-kieReleases-${kieMainBranch}") {

    description("This job: <br> copies kiegroup binaries to filemgmt.jbosss.org  <br> IMPORTANT: makes only sense for community releases <br><b> Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.<b>")

    parameters{
        stringParam("version", "release version", "Edit the version of release, i.e. <b>major.minor.micro.<extension></b> ")
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

    customWorkspace("\$HOME/workspace/${folderPath}/buildAndDeployLocally-kieReleases-${kieMainBranch}")

    wrappers {
        timeout {
            absolute(180)
        }
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${javaToolEnv}")
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
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "${m2Dir}", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(copyBinariesToFilemgmt)
    }
}
