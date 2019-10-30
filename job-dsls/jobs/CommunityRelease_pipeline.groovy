import org.kie.jenkins.jobdsl.Constants

def kieVersion=Constants.KIE_PREFIX
def baseBranch=Constants.BRANCH
def releaseBranch="r7.29.0.Final"
def organization=Constants.GITHUB_ORG_UNIT
def m2Dir="\$HOME/.m2/repository"
def MAVEN_OPTS="-Xms1g -Xmx3g"
def commitMsg="Upgraded version to "
def javadk=Constants.JDK_VERSION
def mvnVersion="kie-maven-3.5.2"
String EAP7_DOWNLOAD_URL = "http://download.devel.redhat.com/released/JBoss-middleware/eap7/7.2.0/jboss-eap-7.2.0.zip"

// creation of folder
folder("Release")

def folderPath="Release"

def comRelease='''
pipeline {
    agent {
        label 'kie-releases'
    }
    tools {
        maven 'kie-maven-3.5.2'
        jdk 'kie-jdk1.8'
    }
    stages {
        stage('CleanWorkspace') {
            steps {
                cleanWs()
            }
        }
        stage('Checkout droolsjbpm-build-bootstrap') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '$baseBranch']], browser: [$class: 'GithubWeb', repoUrl: 'git@github.com:$organization/droolsjbpm-build-bootstrap.git'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'droolsjbpm-build-bootstrap']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'kie-ci-user-key', url: 'git@github.com:$organization/droolsjbpm-build-bootstrap.git']]])
                dir("${WORKSPACE}" + '/droolsjbpm-build-bootstrap') {
                    sh 'pwd \\n' +
                    'git branch \\n' +
                    'git checkout -b $baseBranch\'
                }
            }    
        }    
        stage ('Clone others'){
            steps {
                sshagent(['kie-ci-user-key']) {
                    sh 'sh droolsjbpm-build-bootstrap/script/git-clone-others.sh'
                }    
            }
        }
        stage ('Check if branch exists') {
            steps{
                sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/droolsjbpm-build-bootstrap') {
                        script {
                            myVar = sh(script: 'git ls-remote --heads origin ${releaseBranch} | wc -l', returnStdout: true).trim()
                        } 
                    }
                }
            }
        }
        stage ('log results') {
            steps {
                echo 'myVar: ' + "$myVar"
                script {
                    if ( "$myVar" == "1") {
                        echo "branch exists"
                    } else {
                        echo "branch does not exist"
                    } 
                }
            }
        }
        stage('Create release branches') {
            when{
                expression { myVar == '0'}
            }        
            steps {
                sshagent(['kie-ci-user-key']) {
                    sh 'sh droolsjbpm-build-bootstrap/script/release/02_createReleaseBranches.sh $releaseBranch $baseBranch'
                }    
            }
        }                  
        stage('Update versions') {
            when{
                expression { myVar == '0'}
            }
            steps {
                echo 'kieVersion: ' + "{$kieVersion}"
                sh 'sh droolsjbpm-build-bootstrap/script/release/03_upgradeVersions.sh $kieVersion'
            }
        }
        stage ('Add and commit version upgrades') {
            when{
                expression { myVar == '0'}
            }        
            steps {
                echo 'kieVersion: ' + "{$kieVersion}"
                echo 'commitMsg: ' + "{$commitMsg}"                
                sh 'sh droolsjbpm-build-bootstrap/script/release/addAndCommit.sh "$commitMsg" $kieVersion'
            }
        }
        stage('Push release branches') {
            when{
                expression { myVar == '0'}
            }        
            steps {
                sshagent(['kie-ci-user-key']) {
                    sh 'sh droolsjbpm-build-bootstrap/script/release/04_pushReleaseBranches.sh $baseBranch'
                }    
            }
        }
        stage('Pull from existing release Branches') {
            when{
                expression { myVar == '1'}
            }         
            steps {
                sshagent(['kie-ci-user-key']) {
                    sh 'sh droolsjbpm-build-bootstrap/script/git-all.sh fetch origin'
                    sh 'sh droolsjbpm-build-bootstrap/script/git-all.sh checkout ' + "$releaseBranch"
                }            
            }
        } 
        stage('Build & deploy repositories locally'){
            steps {
                configFileProvider([configFile(fileId: '771ff52a-a8b4-40e6-9b22-d54c7314aa1e', targetLocation: 'jenkins-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                    sh './droolsjbpm-build-bootstrap/script/release/05a_communityDeployLocally.sh $SETTINGS_XML_FILE'
                }
            }
        }    
        stage('Publish JUnit test results reports') {
            steps {
              junit '**/target/*-reports/TEST-*.xml'    
            }
        }         
        stage('Upload binaries to staging repository to Nexus') {
            steps {
                configFileProvider([configFile(fileId: '3f317dd7-4d08-4ee4-b9bb-969c309e782c', targetLocation: 'uploadNexus-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                    dir("${WORKSPACE}" + '/community-deploy-dir') {
                        sh '../droolsjbpm-build-bootstrap/script/release/06_uploadBinariesToNexus.sh $SETTINGS_XML_FILE $kieVersion'
                    }
                }    
            }
        }
        stage('Additional tests') {
            steps {
                parallel (
                    "communityRelease-jbpmTestCoverageMatrix" : {
                        build job: "communityRelease-jbpmTestCoverageMatrix-${baseBranch}", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'baseBranch', value: baseBranch]]
                    },
                    "communityRelease-kieWbTestsMatrix" : {
                            build job: "communityRelease-kieWbTestsMatrix-${baseBranch}", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'baseBranch', value: baseBranch]]
                     },
                    "communityRelease-kieServerMatrix" : {
                            build job: "communityRelease-kieServerMatrix-${baseBranch}", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'baseBranch', value: baseBranch]]
                    }
                )    
            } 
        }               
        stage ('Send email with BUILD result') {
            steps {
                emailext body: 'Build of community ${kieVersion} was:  ' + "${currentBuild.currentResult}" +  '\\n' +
                    ' \\n' +
                    'Failed tests: $BUILD_URL/testReport \\n' +
                    ' \\n' +
                    ' \\n' +
                    'The artifacts are available here \\n' +
                    ' \\n' +
                    'business-central artifacts: (wildfly14.war) \\n' +
                    'https://origin-repository.jboss.org/nexus/content/groups/kie-group/org/kie/business-central/' + "${kieVersion}" + '\\n'+
                    '\\n' +
                    'business-central-webapp: \\n' +
                    'https://origin-repository.jboss.org/nexus/content/groups/kie-group/org/kie/business-central-webapp/' + "${kieVersion}" + '\\n'+
                    '\\n' +
                    'business-monitoring-webapp: \\n' +
                    'https://origin-repository.jboss.org/nexus/content/groups/kie-group/org/kie/business-monitoring-webapp/' + "${kieVersion}" + '\\n'+
                    ' \\n' +
                    'Please download for sanity checks: \\n' +
                    'jbpm-server-distribution.zip: https://origin-repository.jboss.org/nexus/content/groups/kie-group/org/kie/jbpm-server-distribution/' + "${kieVersion}" + '\\n'+
                    ' \\n' +                    
                    ' \\n' +
                    'Please download the needed binaries, fill in your assigned test scenarios and check the failing tests \\n' +
                    'sanity checks: https://docs.google.com/spreadsheets/d/1jPtRilvcOji__qN0QmVoXw6KSi4Nkq8Nz_coKIVfX6A/edit#gid=167259416 \\n' +
                    ' \\n' +
                    'KIE version: ' + "${kieVersion}" + '\\n' +
                    ' \\n' +
                    ' \\n' +                    
                    '${BUILD_LOG, maxLines=750}', subject: 'community ${kieVersion} status and artefacts for sanity checks', to: 'bsig@redhat.com'
            }    
        }
        stage('Approval (Point of NO return)') {
            steps {
                input message: 'Was the build stable enough to do a release', ok: 'Continue with releasing'
            }
        }
        stage('Push community tag') {
            steps {
                sh 'sh droolsjbpm-build-bootstrap/script/release/08a_communityPushTags.sh $kieVersion'
            }
        }
        stage('Create jbpm installers') {
            steps {
                sh 'sh droolsjbpm-build-bootstrap/script/release/09_createjBPM_installers.sh $kieVersion'
            }
        }        
        stage('Push binaries to filemgmgt.jboss.org') {
            steps {
                sshagent(['jenkins-ci-filemgmt']) {
                    sh 'sh droolsjbpm-build-bootstrap/script/release/10_uploadBinariesToFilemgmt.sh $kieVersion'
                }    
            }
        }
        stage ('Send email to BSIG') {
            steps {
                emailext body: 'The community ${kieVersion} was released. \\n' +
                ' \\n' +
                'The tags are pushed and the binaries for the webs were uploaded to filemgmt.jboss.org. \\n' +
                ' \\n' +
                ' \\n' +
                'You can download the artefacts..: \\n' +
                ' \\n' +
                'business-central artifacts: https://repository.jboss.org/nexus/content/groups/public-jboss/org/kie/business-central/$kieVersion/ \\n' +
                'business-central-webapp: https://repository.jboss.org/nexus/content/groups/public-jboss/org/kie/business-central-webapp/$kieVersion/ \\n' +
                'business-monitoring-webapp: https://repository.jboss.org/nexus/content/groups/public-jboss/org/kie/business-monitoring-webapp/kieVersion/ \\n' +
                'jbpm-server-distribution (single zip): https://repository.jboss.org/nexus/content/groups/public-jboss/org/kie/jbpm-server-distribution/$kieVersion/ \\n' +
                 '\\n' +
                 'Component version:\\n' +
                 'kie = $kieVersion', subject: 'community $kieVersion was released', to: 'bsig@redhat.com'
            }
        }                                                                        
    }
}
'''


pipelineJob("${folderPath}/communityRelease-pipeline-${baseBranch}") {

    description('this is a pipeline job for a community release /tag of all reps')


    parameters{
        stringParam("kieVersion", "${kieVersion}", "Please edit the version of kie i.e 7.28.0.Final ")
        stringParam("baseBranch", "${baseBranch}", "Please edit the name of the kie branch ")
        stringParam("releaseBranch", "${releaseBranch}", "Please edit name of the releaseBranch - i.e. r7.28.0.Final ")
        stringParam("organization", "${organization}", "Please edit the name of organization.")
        wHideParameterDefinition {
            name('MAVEN_OPTS')
            defaultValue("${MAVEN_OPTS}")
            description('Please edit the Maven options')
        }
        wHideParameterDefinition {
            name('commitMsg')
            defaultValue("${commitMsg}")
            description('Please edit the commitMsg')
        }
    }

    logRotator {
        numToKeep(10)
        daysToKeep(10)
    }

    definition {
        cps {
            script("${comRelease}")
            sandbox()
        }
    }

}

// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// Additional tests

// definition of jbpmTestCoverageMatrix test
def jbpmTestCoverage='''#!/bin/bash -e
STAGING_REP=kie-internal-group
echo "KIE version: $kieVersion"
# wget the tar.gz sources
wget -q http://\${LOCAL_NEXUS_IP}:8081/nexus/content/repositories/kieAllBuild-$baseBranch/org/jbpm/jbpm/$kieVersion/jbpm-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
mv jbpm-$kieVersion/* .
rmdir jbpm-$kieVersion
'''

matrixJob("${folderPath}/communityRelease-jbpmTestCoverageMatrix-${baseBranch}") {
    description("This job: <br> - Test coverage Matrix for jbpm <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")
    parameters {
        stringParam("kieVersion", "${kieVersion}", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>(7.1.0.20170514-productized) for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
        stringParam("baseBranch", "${baseBranch}", "please edit the branch of the KIE release <br> Will be supplied by the parent job. <br> Normally the baseBranch will be supplied by parent job <br> ******************************************************** <br> ")
        wHideParameterDefinition {
            name('target')
            defaultValue("community")
            description("community")
        }
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

// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

//  run additional test: kieWbTestsMatrix
def kieWbTest='''#!/bin/bash -e
echo "KIE version $kieVersion"
# wget the tar.gz sources
wget -q http://\${LOCAL_NEXUS_IP}:8081/nexus/content/repositories/kieAllBuild-$baseBranch/org/kie/kie-wb-distributions/$kieVersion/kie-wb-distributions-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
mv kie-wb-distributions-$kieVersion/* .
rmdir kie-wb-distributions-$kieVersion'''

matrixJob("${folderPath}/communityRelease-kieWbTestsMatrix-${baseBranch}") {
    description("This job: <br> - Runs the KIE Server integration tests on mutiple supported containers and JDKs <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

    parameters {
        stringParam("kieVersion", "${kieVersion}", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>(7.1.0.20170514-productized) for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
        stringParam("baseBranch", "${baseBranch}", "please edit the branch of the KIE release <br> Will be supplied by the parent job. <br> Normally the baseBranch will be supplied by parent job <br> ******************************************************** <br> ")
        wHideParameterDefinition {
            name('target')
            defaultValue("community")
            description("community")
        }
    }

    axes {
        labelExpression("label_exp", "kie-rhel7&&kie-mem8g&&gui-testing")
        text("container", "wildfly")
        text("war","business-central")
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
        mailer('bsig@redhat.com', false, false)
        wsCleanup()
    }

    steps {
        shell(kieWbTest)
        maven{
            mavenInstallation("${mvnVersion}")
            goals("-nsu -B -e -fae clean verify -P\$container,\$war")
            rootPOM("business-central-tests/pom.xml")
            properties("maven.test.failure.ignore": true)
            properties("deployment.timeout.millis":"240000")
            properties("container.startstop.timeout.millis":"240000")
            properties("webdriver.firefox.bin":"/opt/tools/firefox-60esr/firefox-bin")
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
wget -q http://\${LOCAL_NEXUS_IP}:8081/nexus/content/repositories/kieAllBuild-$baseBranch/org/drools/droolsjbpm-integration/$kieVersion/droolsjbpm-integration-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
mv droolsjbpm-integration-$kieVersion/* .
rmdir droolsjbpm-integration-$kieVersion'''

matrixJob("${folderPath}/communityRelease-kieServerMatrix-${baseBranch}") {
    description("This job: <br> - Runs the KIE Server integration tests on mutiple supported containers and JDKs <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

    // Label which specifies which nodes this job can run on.
    label("master")

    parameters {
        stringParam("kieVersion", "${kieVersion}", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>(7.1.0.20170514-productized) for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
        stringParam("baseBranch", "${baseBranch}", "please edit the branch of the KIE release <br> Will be supplied by the parent job. <br> Normally the baseBranch will be supplied by parent job <br> ******************************************************** <br> ")
        wHideParameterDefinition {
            name('target')
            defaultValue("community")
            description("community")
        }
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
        mailer('bsig@redhat.com', false, false)
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
