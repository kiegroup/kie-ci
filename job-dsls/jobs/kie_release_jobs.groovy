//Define Variables

def kieVersion="7.5.x"
def appformerVersion="2.0.x"
def javadk="jdk1.8"
def jaydekay="JDK1_8"
def mvnToolEnv="APACHE_MAVEN_3_3_9"
def mvnVersion="apache-maven-3.3.9"
def mvnHome="${mvnToolEnv}_HOME"
def mvnOpts="-Xms2g -Xmx3g"
def kieMainBranch="master"
def appformerBranch="master"
def organization="kiegroup"


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

def copyBinariesToFilemgmt="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-copyBinariesToFilemgmt.sh
"""

def createJbpmInstaller ="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie-createJbpmInstaller.sh
"""

def kiesoupCreateReleaseBranch="""
sh \$WORKSPACE/scripts/kie-soup/scripts/release/kiesoup-createReleaseBranch.sh
"""

def kiesoupDeployLocally="""
sh \$WORKSPACE/scripts/kie-soup/scripts/release/kiesoup-deployLocally.sh
"""

def kiesoupPushReleaseBranch="""
sh \$WORKSPACE/scripts/kie-soup/scripts/release/kiesoup-pushReleaseBranch.sh
"""

def kiesoupPushArtifacts="""
sh \$WORKSPACE/scripts/kie-soup/scripts/release/kiesoup-pushArtifactsToNexus.sh
"""

def kiesoupPushTag="""
sh \$WORKSPACE/scripts/kie-soup/scripts/release/kiesoup-pushTag.sh
"""

def kiesoupUpdateVersion="""
sh \$WORKSPACE/scripts/kie-soup/scripts/release/kiesoup-updateVersionAndRaisePR.sh
"""

def appformerCreateReleaseBranch="""
sh \$WORKSPACE/scripts/kie-soup/scripts/release/appformer-createReleaseBranch.sh
"""

def appformerDeployLocally="""
sh \$WORKSPACE/scripts/kie-soup/scripts/release/appformer-deployLocally.sh
"""

def appformerPushReleaseBranch="""
sh \$WORKSPACE/scripts/kie-soup/scripts/release/appformer-pushReleaseBranch.sh
"""

def appformerPushArtifacts="""
sh \$WORKSPACE/scripts/kie-soup/scripts/release/appformer-pushArtifactsToNexus.sh
"""

def appformerPushTag="""
sh \$WORKSPACE/scripts/kie-soup/scripts/release/appformer-pushTag.sh
"""

def appformerUpdateVersion="""
sh \$WORKSPACE/scripts/kie-soup/scripts/release/appformer-updateVersionAndRaisePR.sh
"""


// **************************************************************************

job("createAndPushReleaseBranches-kieReleases-${kieVersion}") {

    description("This job: <br> checksout the right source- upgrades the version in poms <br> - modifies the kie-parent-metadata pom <br> - pushes the generated release branches to kiegroup <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community: <b> community </b><br>or<br> if it is for building a productization tag: <b>productized <br> ******************************************************** <br> ")
        choiceParam("source", ["community-branch", "community-tag", "production-tag"], " please select the source of this release <br> or it is the master branch ( <b> community-branch </b> ) <br> or a community tag ( <b> community-tag </b> ) <br> or a productization tag ( <b> production-tag </b> ) <br> ******************************************************** <br> ")
        stringParam("tag", "tag", "if you selected as <b> source=community-tag </b> or <b> source=production-tag </b> please edit the name of the tag <br> if selected as <b> source=community-branch </b> the parameter <b> tag </b> will be ignored <br> The tag should typically look like <b> major.minor.micro.<extension></b> for <b> community </b> or <b> sync-major.minor.x-<yyyy.mm.dd> </b> for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("releaseVersion", "release version", "please edit the version for this release <br> The <b> releaseVersion </b> should typically look like <b> major.minor.micro.<extension></b> for <b> community </b> or <b> major.minor.micro.<yyymmdd>-productization</b> for <b> productization </b> <br>******************************************************** <br> ")
        stringParam("baseBranch", "base branch", "please select the base branch <br> ******************************************************** <br> ")
        stringParam("releaseBranch", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b>for <b> community </b>or <b> bsync-major.minor.x-<yyyy.mm.dd>  </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("appformerVersion", "appformer version", "please edit the right version to use of appformer<br> The tag should typically look like <b> major.minor.micro.<extension> </b> for <b> community </b> or <b> related kie major.minor.micro.<yyymmdd>-productized </b>for <b> productization </b> <br> ******************************************************** <br> ")
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
        toolenv("${mvnToolEnv}", "${jaydekay}")
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
        shell(createReleaseBranches)
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
        archiveArtifacts{
            onlyIfSuccessful(false)
            allowEmpty(true)
            pattern("**/target/*.log")
        }
    }

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${jaydekay}")
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

    customWorkspace("\$HOME/workspace/buildAndDeployLocally-kieReleases-${kieVersion}")

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${jaydekay}")
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
            mavenInstallation("${mvnVersion}")
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
        labelExpression("label_exp", "linux&&mem8g")
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
            mavenInstallation("${mvnVersion}")
            goals("-B -U -e -fae clean verify -P\$container")
            rootPOM("kie-server-parent/kie-server-tests/pom.xml")
            properties("kie.server.testing.kjars.build.settings.xml":"\$SETTINGS_XML_FILE")
            properties("maven.test.failure.ignore": true)
            properties("deployment.timeout.millis":"240000")
            properties("container.startstop.timeout.millis":"240000")
            properties("eap7.download.url":"http://download.eng.brq.redhat.com/released/JBEAP-7/7.0.2/jboss-eap-7.0.2-full-build.zip")
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
            mavenInstallation("${mvnVersion}")
            goals("-B -e -fae clean verify -P\$container,\$war,selenium -D\$target")
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
        toolenv("${mvnToolEnv}", "${jaydekay}")
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

// ****************************************************************************************

job("updateToNextDevelopmentVersion-kieReleases-${kieVersion}") {

    description("This job: <br> updates the KIE repositories to a new developmenmt version<br>IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        stringParam("baseBranch","master","Branch you want to upgrade")
        stringParam("newVersion", "new KIE version", "Edit the KIE development version")
        stringParam("appformerDevelVersion", "appformer version", "Edit the appformer development version")
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
        toolenv("${mvnToolEnv}", "${jaydekay}")
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
job("create-jbpm-installers-kieReleases-${kieVersion}") {

    description("This job: <br> creates the jbpm-installers  <br> IMPORTANT: makes only sense for community releases <br><b> Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.<b>")

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

    customWorkspace("\$HOME/workspace/buildAndDeployLocally-kieReleases-${kieVersion}")

    wrappers {
        timeout {
            absolute(180)
        }
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${jaydekay}")
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
            envs(MAVEN_OPTS : "${mvnOpts}", MAVEN_HOME : "\$${mvnHome}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${mvnHome}/bin:\$PATH")
        }
        shell(createJbpmInstaller)
    }
}

// ****************************************************************************************

job("copyBinariesToFilemgmt-kieReleases-${kieVersion}") {

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

    customWorkspace("\$HOME/workspace/buildAndDeployLocally-kieReleases-${kieVersion}")

    wrappers {
        timeout {
            absolute(180)
        }
        timestamps()
        colorizeOutput()
        toolenv("${mvnToolEnv}", "${jaydekay}")
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

// *************** kie-soup Release scripts *********

job("release-kiesoup-${kieVersion}") {

    description("This job: <br> releases kie-soup, upgrades the version, builds and deploys, copies artifacts to Nexus, closes the release on Nexus  <br> <b>IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.<b>")

    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community <b> community </b> or <br> if it is for building a productization tag <b>productized <br> ******************************************************** <br> ")
        stringParam("baseBranch", "base branch", "please edit the name of the base branch <br> i.e. typically <b> ${appformerBranch} </b> for <b> community </b><br> ******************************************************** <br> ")
        stringParam("releaseBranch", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b>for <b> community </b>or <b> related kie prod release branch bsync-major.minor.x-<yyyy.mm.dd> </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("newVersion", "new version", "please edit the new version that should be used in the poms <br> The version should typically look like <b> major.minor.micro.<extension> </b>for<b> community </b> or <b> major.minor.micro.<yyyymmdd>-productized </b>for <b> productization </b> <br> ******************************************************** <br> ")

    }

    scm {
        git {
            remote {
                github("${organization}/kie-soup")
            }
            branch ("${kieMainBranch}")
            extensions {
                relativeTargetDirectory("scripts/kie-soup")
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
        toolenv("${mvnToolEnv}", "${jaydekay}")
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
        shell(kiesoupCreateReleaseBranch)
        shell(kiesoupPushReleaseBranch)
        shell(kiesoupDeployLocally)
        shell(kiesoupPushArtifacts)
    }
}

// ******************************************************

job("pushTag-kiesoup-${kieVersion}") {

    description("This job: <br> creates and pushes the tags for <br> community (droolsjbpm) or product (jboss-integration) <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community <b> community </b> or <br> if it is for building a productization tag <b>productized <br> ******************************************************** <br> ")
        stringParam("releaseBranch", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b> for <b> community </b>or <b>  related kie prod release branch bsync-major.minor.x->yyy.mm.dd> </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("tag", "tag", "The tag should typically look like <b> major.minor.micro.<extension> </b>for <b> community </b> or <b>  related kie prod tag sync-major.minor.x-<yyyy.mm.dd> </b>for <b> productization </b> <br> ******************************************************** <br> ")
    };

    scm {
        git {
            remote {
                github("${organization}/kie-soup")
            }
            branch ("${kieMainBranch}")
            extensions {
                relativeTargetDirectory("scripts/kie-soup")
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
        toolenv("${mvnToolEnv}", "${jaydekay}")
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
        shell(kiesoupPushTag)
    }
}

// ******************************************************

job("updateVersion-kiesoup-${kieVersion}") {

    description("This job: <br> updates the kie-soup repository to a new development version <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        stringParam("newVersion", "appformer development version", "Edit appformer development version")
        stringParam("baseBranch", "base branch", "please edit the name of the base branch <br> ******************************************************** <br> ")
    }

    scm {
        git {
            remote {
                github("${organization}/kie-soup")
            }
            branch ("${kieMainBranch}")
            extensions {
                relativeTargetDirectory("scripts/kie-soup")
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
        toolenv("${mvnToolEnv}", "${jaydekay}")
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
        shell(kiesoupUpdateVersion)
    }
}

// *************** appformer Release scripts *********

job("release-appformer-${appformerVersion}") {

    description("This job: <br> releases appformer, upgrades the version, builds and deploys, copies artifacts to Nexus, closes the release on Nexus  <br> <b>IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.<b>")

    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community <b> community </b> or <br> if it is for building a productization tag <b>productized <br> ******************************************************** <br> ")
        stringParam("baseBranch", "base branch", "please edit the name of the base branch <br> i.e. typically <b> ${appformerBranch} </b> for <b> community </b><br> ******************************************************** <br> ")
        stringParam("releaseBranch", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b>for <b> community </b>or <b> related kie prod release branch bsync-major.minor.x-<yyyy.mm.dd> </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("newVersion", "new version", "please edit the new version that should be used in the poms <br> The version should typically look like <b> major.minor.micro.<extension> </b>for<b> community </b> or <b> major.minor.micro.<yyyymmdd>-productized </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("kieVersion", "kie version", "please edit the version of kie <br> The version should typically look like <b> major.minor.micro.<extension> </b>for <b> community </b> or <b> major.minor.micro.<yyyymmdd>-prod </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("erraiVersion", "errai version", " please edit the related errai version<br> ******************************************************** <br> ")

    }

    scm {
        git {
            remote {
                github("${organization}/appformer")
            }
            branch ("${appformerBranch}")
            extensions {
                relativeTargetDirectory("scripts/appformer")
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
        toolenv("${mvnToolEnv}", "${jaydekay}")
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
        shell(appformerCreateReleaseBranch)
        shell(appformerPushReleaseBranch)
        shell(appformerDeployLocally)
        shell(appformerPushArtifacts)
    }
}

// ******************************************************

job("pushTag-appformer-${appformerVersion}") {

    description("This job: <br> creates and pushes the tags for <br> community (droolsjbpm) or product (jboss-integration) <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("target", ["community", "productized"], "please select if this release is for community <b> community </b> or <br> if it is for building a productization tag <b>productized <br> ******************************************************** <br> ")
        stringParam("releaseBranch", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b> for <b> community </b>or <b>  related kie prod release branch bsync-major.minor.x->yyy.mm.dd> </b>for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("tag", "tag", "The tag should typically look like <b> major.minor.micro.<extension> </b>for <b> community </b> or <b>  related kie prod tag sync-major.minor.x-<yyyy.mm.dd> </b>for <b> productization </b> <br> ******************************************************** <br> ")
    };

    scm {
        git {
            remote {
                github("${organization}/appformer")
            }
            branch ("${appformerBranch}")
            extensions {
                relativeTargetDirectory("scripts/appformer")
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
        toolenv("${mvnToolEnv}", "${jaydekay}")
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
        shell(appformerPushTag)
    }
}

// ******************************************************

job("updateVersion-appformer-${appformerVersion}") {

    description("This job: <br> updates the appformer repository to a new development version <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        stringParam("newVersion", "appformer development version", "Edit appformer development version")
        stringParam("baseBranch", "base branch", "please edit the name of the base branch <br> ******************************************************** <br> ")
        stringParam("kieVersion","kie version","Edit kie version")
        stringParam("erraiVersion","errai version","Edit errai version")
    }

    scm {
        git {
            remote {
                github("${organization}/appformer")
            }
            branch ("${appformerBranch}")
            extensions {
                relativeTargetDirectory("scripts/appformer")
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
        toolenv("${mvnToolEnv}", "${jaydekay}")
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
        shell(appformerUpdateVersion)
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
                name("create-jbpm-installers-kieReleases-${kieVersion}")
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
        listView("appformerRelease-${appformerVersion}"){
            description("all scripts needed for building a ${appformerVersion} appformer release")
            jobs {
                name("release-appformer-${appformerVersion}")
                name("pushTag-appformer-${appformerVersion}")
                name("updateVersion-appformer-${appformerVersion}")
            }
            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
            }
        }
        listView("kiesoupRelease-${kieVersion}"){
            description("all scripts needed for building a ${kieVersion} kiesoup release")
            jobs {
                name("release-kiesoup-${kieVersion}")
                name("pushTag-kiesoup-${kieVersion}")
                name("updateVersion-kiesoup-${kieVersion}")
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
