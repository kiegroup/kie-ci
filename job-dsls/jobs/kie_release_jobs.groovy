//Define Variables

def KIE_VERSION="7.2.x"
def UF_VERSION="1.2.x"
def DASH_VERSION="0.8.x"
def JAVADK="jdk1.8"
def JDK="JDK1_8"
def MAVEN="APACHE_MAVEN_3_3_9"
def MVNHOME="${MAVEN}_HOME"
def MVNOPTS="-Xms2g -Xmx3g"
def KIE_MAIN_BRANCH="master"
def UF_MAIN_BRANCH="master"
def DASH_MAIN_BRANCH="master"
def ORGANIZATION="kiegroup"
def UF_ORGANIZATION="AppFormer"
def DASH_ORGANIZATION="dashbuilder"


def pushReleaseBranches ="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie_createAndPushReleaseBranches.sh
"""

def deployLocally="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie_deployLocally.sh
"""

def copyToNexus="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie_copyBinariesToNexus.sh
"""

def jbpmTestCoverageMatrix="""
git clone https://github.com/kiegroup/droolsjbpm-build-bootstrap.git -b master
sh \$WORKSPACE/droolsjbpm-build-bootstrap/script/release/kie_jbpmTestCoverMartix.sh
"""

def kieAllServerMatrix="""
git clone https://github.com/kiegroup/droolsjbpm-build-bootstrap.git -b master
sh \$WORKSPACE/droolsjbpm-build-bootstrap/script/release/kie_allServerMatrix.sh
"""

def kieWbSmokeTestsMatrix="""
git clone https://github.com/kiegroup/droolsjbpm-build-bootstrap.git -b master
sh \$WORKSPACE/droolsjbpm-build-bootstrap/script/release/kie_wbSmokeTestsMatrix.sh
"""

def pushTags="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie_pushTags.sh
"""

def removeBranches="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie_removeReleaseBranches.sh
"""

def updateVersions="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie_updateToNextDevelopmentVersion.sh
"""

def copyBinariesToFilemgmt="""
sh \$WORKSPACE/scripts/droolsjbpm-build-bootstrap/script/release/kie_copyBinariesToFilemgmt.sh
"""

def ufDeploy="""
sh \$WORKSPACE/scripts/uberfire/scripts/release/uf_createAndDeploy.sh
"""

def ufPushTag="""
sh \$WORKSPACE/scripts/uberfire/scripts/release/uf_pushTag.sh
"""

def ufUpdateVersion="""
sh \$WORKSPACE/scripts/uberfire/scripts/release/uf_updateVersion.sh
"""

def dashDeploy="""
sh \$WORKSPACE/scripts/dashbuilder/scripts/release/dash_createAndDeploy.sh
"""

def dashPushTag="""
sh \$WORKSPACE/scripts/dashbuilder/scripts/release/dash_pushTag.sh
"""

def dashUpdateVersion="""
sh \$WORKSPACE/scripts/dashbuilder/scripts/release/dash_updateVersion.sh
"""


// **************************************************************************

job("kie_${KIE_VERSION}_createAndPushReleaseBranches") {

    description("This job: <br> checksout the right source- upgrades the version in poms <br> - modifies the kie-parent-metadata pom <br> - pushes the generated release branches to kiegroup <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("TARGET", ["community", "productized"], "please select if this release is for community: <b> community </b><br>or<br> if it is for building a productization tag: <b>productized <br> ******************************************************** <br> ")
        choiceParam("SOURCE", ["community-branch", "community-tag", "production-tag"], " please select the source of this release <br> or it is the master branch ( <b> community-branch </b> ) <br> or a community tag ( <b> community-tag </b> ) <br> or a productization tag ( <b> production-tag </b> ) <br> ******************************************************** <br> ")
        stringParam("TAG", "tag", "if you selected as <b> SOURCE=community-tag </b> or <b> SOURCE=production-tag </b> please edit the name of the tag <br> if selected as <b> SOURCE=community-branch </b> the parameter <b> TAG </b> will be ignored <br> The tag should typically look like <b> major.minor.micro.<extension> </b>(7.1.0.Beta1) for <b> community </b> or <b> sync-major.minor.x-<yyyy.mm.dd> (sync-7.1.x-2017.05.14)  </b> for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("RELEASE_VERSION", "release version", "please edit the version for this release <br> The <b> RELEASE_VERSION </b> should typically look like <b> major.minor.micro.<extension></b> (7.1.0.Beta1) for <b> community </b> or <b> major.minor.micro.<yyymmdd>-productization</b> (7.1.0.20170514-productized) for <b> productization </b> <br>******************************************************** <br> ")
        stringParam("BASE_BRANCH", "base branch", "please select the base branch <br> ******************************************************** <br> ")
        stringParam("RELEASE_BRANCH", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b>(r7.1.0.Beta1) for <b> community </b>or <b> bsync-major.minor.x-<yyyy.mm.dd>  </b>(bsync-7.1.x-2017.05.14) for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("UBERFIRE_VERSION", "uberfire version", "please edit the right version to use of uberfire/uberfire-extensions <br> The tag should typically look like <b> major.minor.micro.<extension> </b>(1.1.0.Beta1) for <b> community </b> or <b> related kie major.minor.micro.<yyymmdd>-productized </b>(7.1.0.20170514-productized) for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("DASHBUILDER_VERSION", "dashbuilder version", "please edit the right version to use of dashbuilder <br> The tag should typically look like <b> major.minor.micro.<extension>  </b>(0.7.0.Beta1) for <b> community </b> or <b> related kie major.minor.micro.<yyymmdd>-productized </b>(7.1.0.20170514-productized) for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("ERRAI_VERSION", "errai version", " please edit the related errai version<br> ******************************************************** <br> ")
    };

    scm {
        git {
            remote {
                github("${ORGANIZATION}/droolsjbpm-build-bootstrap")
            }
            branch ("${KIE_MAIN_BRANCH}")
            extensions {
                relativeTargetDirectory("scripts/droolsjbpm-build-bootstrap")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${JAVADK}")

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${MAVEN}", "${JDK}")
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
            envs(MAVEN_OPTS : "${MVNOPTS}", MAVEN_HOME : "\$${MVNHOME}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${MVNHOME}/bin:\$PATH")
        }
        shell(pushReleaseBranches)
    }
}

// **************************************************************************************

job("kie_${KIE_VERSION}_buildAndDeployLocally") {

    description("This job: <br> - builds all repositories and deploys them locally <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("TARGET", ["community", "productized"], "please select if this release is for community <b> community </b> or <br> if it is for building a productization tag <b>productized <br> ******************************************************** <br> ")
        stringParam("RELEASE_BRANCH", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b>(7.1.0.Beta1) </b> for <b> community </b>or <b> bsync-major.minor.x-<yyyy.mm.dd>  </b>(bsync-7.1.x-2017.05.15) for <b> productization </b> <br> ******************************************************** <br> ")
    };

    scm {
        git {
            remote {
                github("${ORGANIZATION}/droolsjbpm-build-bootstrap")
            }
            branch ("${KIE_MAIN_BRANCH}")
            extensions {
                relativeTargetDirectory("scripts/droolsjbpm-build-bootstrap")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${JAVADK}")

    publishers {
        archiveJunit("**/TEST-*.xml")
    }

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${MAVEN}", "${JDK}")
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
            envs(MAVEN_OPTS : "${MVNOPTS}", MAVEN_HOME : "\$${MVNHOME}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${MVNHOME}/bin:\$PATH")
        }
        shell(deployLocally)
    }
}

// ********************************************************************************

job("kie_${KIE_VERSION}_copyBinariesToNexus") {

    description("This job: <br> - copies binaries from local dir to Nexus <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("TARGET", ["community", "productized"], "please select if this release is for community: <b> community </b> or <br> if it is for building a productization tag: <b>productized <br> ******************************************************** <br> ")
    };

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${JAVADK}")

    customWorkspace("\$HOME/workspace/kie_${KIE_VERSION}_buildAndDeployLocally")

    wrappers {
        timestamps()
        colorizeOutput()
        toolenv("${MAVEN}", "${JDK}")
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
            trigger("kie_${KIE_VERSION}_allJbpmTestCoverageMatrix, kie_${KIE_VERSION}_allServerMatrix, kie_${KIE_VERSION}_wbSmokeTestsMatrix") {
                condition("SUCCESS")
                parameters {
                    propertiesFile("kie.properties", true)
                }
            }
        }
    }

    steps {
        environmentVariables {
            envs(MAVEN_OPTS : "${MVNOPTS}", MAVEN_HOME : "\$${MVNHOME}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${MVNHOME}/bin:\$PATH")
        }
        shell(copyToNexus)
    }


}

// **************************************************************************************

matrixJob("kie_${KIE_VERSION}_allJbpmTestCoverageMatrix") {

    description("This job: <br> - Test coverage Matrix for jbpm <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")
    parameters {
        choiceParam("TARGET", ["community", "productized"], "please select if this release is for community <b> community: </b> or <br> if it is for building a productization tag: <b>productized <br> Version to test. Will be supplied by the parent job. <br> ******************************************************** <br> ")
        stringParam("KIE_VERSION", "KIE version", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>(7.1.0.20170514-productized) for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
    };

    axes {
        labelExpression("label-exp","linux && mem4g")
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

matrixJob("kie_${KIE_VERSION}_allServerMatrix") {
    description("This job: <br> - Runs the KIE Server integration tests on mutiple supported containers and JDKs <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

    parameters {
        choiceParam("TARGET", ["community", "productized"], "<br> ******************************************************** <br> ")
        stringParam("KIE_VERSION", "KIE version", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>(7.1.0.20170514-productized) for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
    };

    axes {
        jdk("jdk1.8")
        text("container", "tomcat8", "wildfly10")
        labelExpression("label_exp", "linux && mem4g")
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

matrixJob("kie_${KIE_VERSION}_wbSmokeTestsMatrix") {
    description("This job: <br> - Runs the smoke tests on KIE <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

    parameters {
        choiceParam("TARGET", ["community", "productized"], "<br> ******************************************************** <br> ")
        stringParam("KIE_VERSION", "KIE version", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>(7.1.0.20170514-productized) for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
    };

    axes {
        jdk("jdk1.8")
        text("container", "wildfly10", "tomcat8", "eap7")
        text("war", "kie-wb", "kie-drools-wb")
        labelExpression("label_exp", "linux && mem4g && gui-testing")
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

job("kie_${KIE_VERSION}_pushTags") {

    description("This job: <br> creates and pushes the tags for <br> community (kiegroup) or product (jboss-integration) <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("TARGET", ["community", "productized"], "please select if this release is for community: <b> community </b> or <br> if it is for building a productization tag: <b>productized <br> ******************************************************** <br> ")
        stringParam("RELEASE_BRANCH", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b>(r7.1.0.Beta1) for <b> community </b>or <b> bsync-major.minor.x-<yyy.mm.dd>  </b>()bsync-7.1.x-2017.05.14 for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("TAG_NAME", "tag", "Please enter the tag. The tag should typically look like <b> major.minor.micro.<extension> </b>(7.1.0.Beta1) for <b> community </b> or <b> sync-major.minor.x-<yyy.mm.dd> </b>(sync-7.1.x-2017.05.10) for <b> productization </b> <br> ******************************************************** <br> ")
    };

    scm {
        git {
            remote {
                github("${ORGANIZATION}/droolsjbpm-build-bootstrap")
            }
            branch ("${KIE_MAIN_BRANCH}")
            extensions {
                relativeTargetDirectory("scripts/droolsjbpm-build-bootstrap")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${JAVADK}")

    wrappers {
        timeout {
            absolute(30)
        }
        timestamps()
        preBuildCleanup()
        colorizeOutput()
        toolenv("${MAVEN}", "${JDK}")
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
            envs(MAVEN_OPTS : "${MVNOPTS}", MAVEN_HOME : "\$${MVNHOME}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${MVNHOME}/bin:\$PATH")
        }
        shell(pushTags)
    }
}

// ***********************************************************************************

job("kie_${KIE_VERSION}_removeReleaseBranches") {

    description("This job: <br> creates and pushes the tags for <br> community (kiegroup) or product (jboss-integration) <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("TARGET", ["community", "productized"], "please select if this release is for community: <b> community </b> or <br> if it is for building a productization tag: <b>productized <br> ******************************************************** <br> ")
        stringParam("BASE_BRANCH", "base branch", "please select the base branch <br> ******************************************************** <br> ")
        stringParam("RELEASE_BRANCH", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b>(r7.0.0.CR1) for <b> community </b>or <b> bsync-major.minor.x-<yyy.mm.dd> </b>bsync-7.1.x-2017.05.14 for <b> productization </b> <br> ******************************************************** <br> ")
    };

    scm {
        git {
            remote {
                github("${ORGANIZATION}/droolsjbpm-build-bootstrap")
            }
            branch ("${KIE_MAIN_BRANCH}")
            extensions {
                relativeTargetDirectory("scripts/droolsjbpm-build-bootstrap")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${JAVADK}")

    wrappers {
        timeout {
            absolute(30)
        }
        timestamps()
        preBuildCleanup()
        colorizeOutput()
        toolenv("${MAVEN}", "${JDK}")
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
            envs(MAVEN_OPTS : "${MVNOPTS}", MAVEN_HOME : "\$${MVNHOME}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${MVNHOME}/bin:\$PATH")
        }
        shell(removeBranches)
    }
}

// ****************************************************************************************

job("kie_${KIE_VERSION}_updateToNextDevelopmentVersion") {

    description("This job: <br> updates the KIE repositories to a new developmenmt version <br> for 7.1.x </br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        stringParam("BASE_BRANCH","master","Branch you want to upgrade")
        stringParam("newVersion", "new KIE version", "Edit the KIE development version")
        stringParam("UF_DEVEL_VERSION", "uberfire version", "Edit the uberfire development version")
        stringParam("DASHB_DEVEL_VERSION", "dashbuilder version", "Edit the dashbuilder development version")
        stringParam("ERRAI_DEVEL_VERSION", "errai version", "Edit the errai development version")
    }

    scm {
        git {
            remote {
                github("${ORGANIZATION}/droolsjbpm-build-bootstrap")
            }
            branch ("${KIE_MAIN_BRANCH}")
            extensions {
                relativeTargetDirectory("scripts/droolsjbpm-build-bootstrap")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${JAVADK}")

    wrappers {
        timeout {
            absolute(30)
        }
        timestamps()
        preBuildCleanup()
        colorizeOutput()
        toolenv("${MAVEN}", "${JDK}")
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
            envs(MAVEN_OPTS : "${MVNOPTS}", MAVEN_HOME : "\$${MVNHOME}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${MVNHOME}/bin:\$PATH")
        }
        shell(updateVersions)
    }
}

// ****************************************************************************************

job("kie_${KIE_VERSION}_copyBinariesToFilemgmt") {

    description("This job: <br> copies kiegroup binaries to filemgmt.jbosss.org  <br> IMPORTANT: makes only sense for community releases <br><b> Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.<b>")

    parameters{
        stringParam("VERSION", "release version", "Edit the version of release, i.e. <b>major.minor.micro.<extension></b>(7.0.0.Final) ")
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${JAVADK}")

    customWorkspace("\$HOME/workspace/kie_${KIE_VERSION}_buildAndDeployLocally")

    wrappers {
        timeout {
            absolute(120)
        }
        timestamps()
        colorizeOutput()
        toolenv("${MAVEN}", "${JDK}")
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
            envs(MAVEN_OPTS : "${MVNOPTS}", MAVEN_HOME : "\$${MVNHOME}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${MVNHOME}/bin:\$PATH")
        }
        shell(copyBinariesToFilemgmt)
    }
}

// *************** Uberfire Release scripts *********

job("uf_${UF_VERSION}_release") {

    description("This job: <br> releases uberfire, upgrades the version, builds and deploys, copies artifacts to Nexus, closes the release on Nexus  <br> <b>IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.<b>")

    parameters {
        choiceParam("TARGET", ["community", "productized"], "please select if this release is for community <b> community </b> or <br> if it is for building a productization tag <b>productized <br> ******************************************************** <br> ")
        stringParam("BASE_BRANCH", "base branch", "please edit the name of the base branch <br> i.e. typically <b> 1.0.x </b> for <b> community </b>or <b> bsync-6.5.x-2016.08.05  </b> for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("RELEASE_BRANCH", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extenbsion> </b>(r1.1.0.Beta1) for <b> community </b>or <b> related kie prod release branch bsync-major.minor.x-<yyyy.mm.dd> </b> bsync-7.1.x-2017.05.14  for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("newVersion", "new version", "please edit the new version that should be used in the poms <br> The version should typically look like <b> major.minor.micro.<extension> </b>(1.1.0.Beta1) for<b> community </b> or <b> major.minor.micro.<yyyymmdd>-productized </b>(1.1.0.20170515-productized) for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("ERRAI_VERSION", "errai version", " please edit the related errai version<br> ******************************************************** <br> ")
    }

    scm {
        git {
            remote {
                github("${UF_ORGANIZATION}/uberfire")
            }
            branch ("${UF_MAIN_BRANCH}")
            extensions {
                relativeTargetDirectory("scripts/uberfire")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${JAVADK}")

    wrappers {
        timeout {
            absolute(60)
        }
        timestamps()
        preBuildCleanup()
        colorizeOutput()
        toolenv("${MAVEN}", "${JDK}")
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
            envs(MAVEN_OPTS : "${MVNOPTS}", MAVEN_HOME : "\$${MVNHOME}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${MVNHOME}/bin:\$PATH")
        }
        shell(ufDeploy)
    }
}

// ******************************************************

job("uf_${UF_VERSION}_pushTag") {

    description("This job: <br> creates and pushes the tags for <br> community (droolsjbpm) or product (jboss-integration) <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("TARGET", ["community", "productized"], "please select if this release is for community <b> community </b> or <br> if it is for building a productization tag <b>productized <br> ******************************************************** <br> ")
        stringParam("RELEASE_BRANCH", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b> (r1.1.0.Beta1) for <b> community </b>or <b>  related kie prod release branch bsync-major.minor.x->yyy.mm.dd> </b>(bsync-7.1.x-2017.05.14) for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("TAG", "tag", "The tag should typically look like <b> major.minor.micro.<extension> </b>(1.1.0.Beta1) </b> for <b> community </b> or <b>  related kie prod tag sync-major.minor.x-<yyyy.mm.dd> </b>(sync-7.1.0.2017.05.14) </b> for <b> productization </b> <br> ******************************************************** <br> ")
    };

    scm {
        git {
            remote {
                github("${UF_ORGANIZATION}/uberfire")
            }
            branch ("${UF_MAIN_BRANCH}")
            extensions {
                relativeTargetDirectory("scripts/uberfire")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${JAVADK}")

    wrappers {
        timeout {
            absolute(30)
        }
        timestamps()
        colorizeOutput()
        preBuildCleanup()
        toolenv("${MAVEN}", "${JDK}")
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
            envs(MAVEN_OPTS : "${MVNOPTS}", MAVEN_HOME : "\$${MVNHOME}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${MVNHOME}/bin:\$PATH")
        }
        shell(ufPushTag)
    }
}

// ******************************************************

job("uf_${UF_VERSION}_updateVersion") {

    description("This job: <br> updates the uberfire repository to a new development version <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        stringParam("newVersion", "uberfire development version", "Edit uberfire development version")
        stringParam("BASE_BRANCH", "base branch", "please edit the name of the base branch <br> ******************************************************** <br> ")
        stringParam("ERRAI_DEVEL_VERSION","errai development version","Edit errai development version")
    }

    scm {
        git {
            remote {
                github("${UF_ORGANIZATION}/uberfire")
            }
            branch ("${UF_MAIN_BRANCH}")
            extensions {
                relativeTargetDirectory("scripts/uberfire")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${JAVADK}")

    wrappers {
        timeout {
            absolute(30)
        }
        timestamps()
        colorizeOutput()
        preBuildCleanup()
        toolenv("${MAVEN}", "${JDK}")
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
            envs(MAVEN_OPTS : "${MVNOPTS}", MAVEN_HOME : "\$${MVNHOME}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${MVNHOME}/bin:\$PATH")
        }
        shell(ufUpdateVersion)
    }
}
// *************** Dashbuilder Release scripts ***********************

job("dash_${DASH_VERSION}_release") {

    description("This job: <br> releases dashbuilder, upgrades the version, builds and deploys, copies artifacts to Nexus, closes the release on Nexus  <br> <b>IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.<b>")

    parameters {
        choiceParam("TARGET", ["community", "productized"], "please select if this release is for community <b> community </b> or <br> if it is for building a productization tag <b>productized <br> ******************************************************** <br> ")
        stringParam("BASE_BRANCH", "base branch", "please edit the name of the base branch <br> i.e. typically <b> major.minor.x </b>(0.7.x) for <b> community </b>or <b> bsync-major.minor.x-<yyyy.mm.dd> </b>(bsync-7.1.x-2017.05.14)  for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("RELEASE_BRANCH", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b>(r0.7.0.Beta1) for <b> community </b>or <b> related kie prod release branch bsync-major.minor.x-<yyyy.mm.dd> </b> bsync-7.1.x-2017.05.14  for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("newVersion", "new version", "please edit the new version that should be used in the poms <br> The version should typically look like <b> major.minor.micro.<extension> </b>(0.7.0.Beta1) for<b> community </b> or <b> major.minor.micro.<yyyymmdd>-productized </b>(0.7.0.20170514-productized) for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("UBERFIRE_VERSION", "uberfire version", "please edit the version of uberfire <br> The version should typically look like <b> major.minor.micro.<extension> </b>(1.1.0.Beta1) for <b> community </b> or <b> major.minor.micro.<yyyymmdd>-productized </b>(7.1.0.20170524-productized) for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("ERRAI_VERSION", "errai version", "please select the needed errai version <br> ******************************************************** <br> ")
    }

    scm {
        git {
            remote {
                github("${DASH_ORGANIZATION}/dashbuilder")
            }
            branch ("${DASH_MAIN_BRANCH}")
            extensions {
                relativeTargetDirectory("scripts/dashbuilder")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${JAVADK}")

    wrappers {
        timeout {
            absolute(60)
        }
        timestamps()
        preBuildCleanup()
        colorizeOutput()
        toolenv("${MAVEN}", "${JDK}")
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
            envs(MAVEN_OPTS : "${MVNOPTS}", MAVEN_HOME : "\$${MVNHOME}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${MVNHOME}/bin:\$PATH")
        }
        shell(dashDeploy)
    }
}

// ******************************************************

job("dash_${DASH_VERSION}_pushTag") {

    description("This job: <br> creates and pushes the tags for <br> community (dashbuilder) or product (jboss-integration) <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        choiceParam("TARGET", ["community", "productized"], "please select if this release is for community <b> community </b> or <br> if it is for building a productization tag <b>productized <br> ******************************************************** <br> ")
        stringParam("RELEASE_BRANCH", "release branch", "please edit the name of the release branch <br> i.e. typically <b> r+major.minor.micro.<extension> </b> (r0.7.0.Beta1) for <b> community </b>or <b> related kie prod release branch bsync-major.minor.x->yyy.mm.dd> </b>(bsync-7.1.x-2017.05.14) for <b> productization </b> <br> ******************************************************** <br> ")
        stringParam("TAG", "tag", "The tag should typically look like <b> major.minor.micro.<extension> </b>(0.7.0.Beta1) </b> for <b> community </b> or <b> related kie prod tag sync-major.minor.x-<yyyy.mm.dd> </b>(sync-7.1.0.2017.05.14) </b> for <b> productization </b> <br> ******************************************************** <br> ")
    };

    scm {
        git {
            remote {
                github("${DASH_ORGANIZATION}/dashbuilder")
            }
            branch ("${DASH_MAIN_BRANCH}")
            extensions {
                relativeTargetDirectory("scripts/dashbuilder")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${JAVADK}")

    wrappers {
        timeout {
            absolute(30)
        }
        timestamps()
        colorizeOutput()
        preBuildCleanup()
        toolenv("${MAVEN}", "${JDK}")
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
            envs(MAVEN_OPTS : "${MVNOPTS}", MAVEN_HOME : "\$${MVNHOME}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${MVNHOME}/bin:\$PATH")
        }
        shell(dashPushTag)
    }
}

// ******************************************************

job("dash_${DASH_VERSION}_updateVersion") {

    description("This job: <br> updates dashbuilder repository to a new developmenmt version <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")

    parameters {
        stringParam("newVersion", "new dashbuilder version", "Edit the new dashbuilder version")
        stringParam("BASE_BRANCH", "base branch", "please select the base branch <br> ******************************************************** <br> ")
        stringParam("UF_DEVEL_VERSION", "uberfire development version", "Edit the uberfire development version")
        stringParam("ERRAI_DEVEL_VERSION", "errai development version", "Edit the errai development version")
    }

    scm {
        git {
            remote {
                github("${DASH_ORGANIZATION}/dashbuilder")
            }
            branch ("${DASH_MAIN_BRANCH}")
            extensions {
                relativeTargetDirectory("scripts/dashbuilder")
            }

        }
    }

    label("kie-releases")

    logRotator {
        numToKeep(10)
    }

    jdk("${JAVADK}")

    wrappers {
        timeout {
            absolute(30)
        }
        timestamps()
        colorizeOutput()
        preBuildCleanup()
        toolenv("${MAVEN}", "${JDK}")
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
            envs(MAVEN_OPTS : "${MVNOPTS}", MAVEN_HOME : "\$${MVNHOME}", MAVEN_REPO_LOCAL : "/home/jenkins/.m2/repository", PATH : "\$${MVNHOME}/bin:\$PATH")
        }
        shell(dashUpdateVersion)
    }
}


// **************************** VIEW to create on JENKINS CI *******************************************

nestedView("kie release for ${KIE_MAIN_BRANCH} branch"){
    views{
        listView("${KIE_VERSION} kie release"){
            description("all scripts needed for building a ${KIE_VERSION} KIE Release}")
            jobs {
                name("kie_${KIE_VERSION}_createAndPushReleaseBranches")
                name("kie_${KIE_VERSION}_buildAndDeployLocally")
                name("kie_${KIE_VERSION}_copyBinariesToNexus")
                name("kie_${KIE_VERSION}_allJbpmTestCoverageMatrix")
                name("kie_${KIE_VERSION}_allServerMatrix")
                name("kie_${KIE_VERSION}_wbSmokeTestsMatrix")
                name("kie_${KIE_VERSION}_pushTags")
                name("kie_${KIE_VERSION}_removeReleaseBranches")
                name("kie_${KIE_VERSION}_updateToNextDevelopmentVersion")
                name("kie_${KIE_VERSION}_copyBinariesToFilemgmt")
            }
            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
            }
        }
        listView("${UF_VERSION} uberfire Release"){
            description("all scripts needed for building a ${UF_VERSION} uberfire release}")
            jobs {
                name("uf_${UF_VERSION}_release")
                name("uf_${UF_VERSION}_pushTag")
                name("uf_${UF_VERSION}_updateVersion")
            }
            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
            }
        }
        listView("${DASH_VERSION} dashbuilder release"){
            description("all scripts needed for building a ${DASH_VERSION} dashbuilder release}")
            jobs {
                name("dash_${DASH_VERSION}_release")
                name("dash_${DASH_VERSION}_pushTag")
                name("dash_${DASH_VERSION}_updateVersion")
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
