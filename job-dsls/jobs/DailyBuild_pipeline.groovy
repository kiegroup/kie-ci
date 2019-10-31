import org.kie.jenkins.jobdsl.Constants

def javadk=Constants.JDK_VERSION
def mvnVersion="kie-maven-3.5.2"
def javaToolEnv="KIE_JDK1_8"
def mvnToolEnv="KIE_MAVEN_3_5_2"
def mvnHome="${mvnToolEnv}_HOME"
def kieVersion=Constants.KIE_PREFIX
def baseBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def deployDir="deploy-dir"
def m2Dir="\$HOME/.m2/repository"

String EAP7_DOWNLOAD_URL = "http://download.devel.redhat.com/released/JBoss-middleware/eap7/7.2.0/jboss-eap-7.2.0.zip"

// creation of folder
folder("DailyBuild")
folder("Docker")

def folderPath="DailyBuild"
def dockerPath="Docker"

def dailyBuild='''
pipeline {
    agent {
        label 'kie-linux&&kie-rhel7&&kie-mem24g'
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
        stage('Calculate versions') {
            steps {
                script {
                    data = new Date().format('yyyyMMdd-hhMMss')
                    kieVersion = "${kieVersion}.${data}"
                    dockerAbsPath = "KIE/${baseBranch}/Docker"

                    echo "data: ${data}"
                    echo "kieVersion: ${kieVersion}"
                    echo "baseBranch: ${baseBranch}"
                    echo "organization: ${organization}"
                }
            }
        }
        stage ('Checkout droolsjbpm-build-boostrap') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '$baseBranch']], browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/$organization/droolsjbpm-build-bootstrap'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'droolsjbpm-build-bootstrap']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/$organization/droolsjbpm-build-bootstrap.git']]])
                dir("${WORKSPACE}" + '/droolsjbpm-build-bootstrap') {
                    sh 'pwd \\n' +
                       'git branch \\n' +
                       'git checkout -b $baseBranch\'
                } 
            }
        }
        stage('Clone all other reps') {
            steps {
                sh "sh droolsjbpm-build-bootstrap/script/release/01_cloneBranches.sh $baseBranch"
            }
        }
        stage('Update versions') {
            steps {
                sh "echo 'kieVersion: $kieVersion'"
                sh "sh droolsjbpm-build-bootstrap/script/release/03_upgradeVersions.sh $kieVersion"
            }
        }
        stage('Create clean up script') {
            steps {
                sh 'cat > "$WORKSPACE/clean-up.sh" << EOT \\n' +
                        'cd \\\\$1 \\n' +
                        '# Add test reports to the index to prevent their removal in the following step \\n' +
                        'git add --force **target/*-reports/TEST-*.xml \\n' +
                        'git clean -ffdx \\n' +
                        'EOT'
            }
        }
        stage('Deploy repositories locally'){
            steps {
                configFileProvider([configFile(fileId: '771ff52a-a8b4-40e6-9b22-d54c7314aa1e', targetLocation: 'jenkins-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                    sh "sh droolsjbpm-build-bootstrap/script/release/05c_dailyBuildDeployLocally.sh $SETTINGS_XML_FILE"
                }
            }
        }
        stage('Publish JUnit test results reports') {
            steps {
              junit '**/target/*-reports/TEST-*.xml'    
            }
        }        
        stage ('Send mail') {
            steps {
                emailext body: 'daily build #${BUILD_NUMBER} of ${baseBranch} was:' + "${currentBuild.currentResult}" +  '\\n' +
                    'Please look here: ${BUILD_URL} \\n' +
                    '${BUILD_LOG, maxLines=750}', subject: 'daily-build-${baseBranch} #${BUILD_NUMBER}:' + "${currentBuild.currentResult}", to: 'bsig@redhat.com'
            }    
        }        
        stage('Unpack zip of artifacts to QA Nexus') {
            steps {
                withCredentials([usernameColonPassword(credentialsId: 'unpacks-zip-on-qa-nexus', variable: 'kieUnpack')]) {
                    // unpack zip to QA Nexus
                    sh 'cd $deployDir\\n' +
                            'zip -r kiegroup .\\n' +
                            'curl --silent --upload-file kiegroup.zip -u $kieUnpack -v http://\${LOCAL_NEXUS_IP}:8081/nexus/service/local/repositories/kieAllBuild-$baseBranch/content-compressed\\n' +
                            'cd ..'
                }
            }
        }
        stage('Additional tests') {
            steps {
                parallel (
                    "jbpmTestCoverageMatrix" : {
                        build job: "DailyBuild-jbpmTestCoverageMatrix-${baseBranch}", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'baseBranch', value: baseBranch]]
                    },
                    "jbpmTestContainerMatrix" : {
                        build job: "DailyBuild-jbpmTestContainerMatrix-${baseBranch}", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'baseBranch', value: baseBranch]]
                    },
                    "kieWbTestsMatrix" : {
                            build job: "DailyBuild-kieWbTestsMatrix-${baseBranch}", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'baseBranch', value: baseBranch]]
                     },
                    "kieServerMatrix" : {
                            build job: "DailyBuild-kieServerMatrix-${baseBranch}", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'baseBranch', value: baseBranch]]
                    },
                    "DailyBuild-docker-images" : {
                            build job: "${dockerAbsPath}/DailyBuild-docker-images-${baseBranch}", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion]]
                    }
                )    
            } 
        }
        stage('Delete workspace when build is done') {
            steps {
                cleanWs()
            }
        }                                 
    }
    post {
        failure{
            script {
                currentBuild.result = 'FAILURE'
            }            
            emailext body: 'status of daily build #${BUILD_NUMBER} (${baseBranch} branch) was: ' + "${currentBuild.currentResult}" +  '\\n' +
                    'Please look here: ${BUILD_URL} \\n' +
                    '${BUILD_LOG, maxLines=750}', subject: 'daily-build-${baseBranch} #${BUILD_NUMBER}: ' + "${currentBuild.currentResult}", to: 'bsig@redhat.com'
        }
    }    
}
'''

pipelineJob("${folderPath}/DailyBuild-pipeline-${baseBranch}") {

    description('this is a pipeline job for the daily build of all reps')


    parameters{
        stringParam("kieVersion", "${kieVersion}", "Version of kie. This will be usually set automatically by the parent pipeline job. ")
        stringParam("baseBranch", "${baseBranch}", "kie branch. This will be usually set automatically by the parent pipeline job. ")
        stringParam("organization", "${organization}", "Name of organization. This will be usually set automatically by the parent pipeline job. ")
        wHideParameterDefinition {
            name('deployDir')
            defaultValue("${deployDir}")
            description('Please edit the deployDir')
        }
    }

    logRotator {
        numToKeep(10)
        daysToKeep(10)
    }

    definition {
        cps {
            script("${dailyBuild}")
            sandbox()
        }
    }
}

// *****************************************************************************************************
// definition of triggering script

job ("${folderPath}/DailyBuild-trigger-${baseBranch}"){

    description('This job triggers the two pipeline jobs ')

    label("kie-linux&&kie-rhel7&&kie-mem8g")

    logRotator {
        numToKeep(10)
    }

    jdk("${javadk}")

    // the UMB trigger has to have the branch name hard coded - could not have a parameter
    configure { project ->
        project / triggers << 'com.redhat.jenkins.plugins.ci.CIBuildTrigger' {
            spec ''
            providerName 'Red Hat UMB'
            overrides {
                topic 'Consumer.rh-jenkins-ci-plugin.${JENKINS_UMB_ID}-prod-daily-7-26-x-trigger.VirtualTopic.qe.ci.ba.daily-7-26-x.trigger'
            }
            selector 'label = \'rhba-ci\''
        }
    }
    wrappers{
        timestamps()
        colorizeOutput()
        preBuildCleanup()
    }

    publishers {
        downstream ("DailyBuild-pipeline-${baseBranch}", threshHoldName = 'SUCCESS')
        downstream ("../DailyBuild-prod/DailyBuild-prod-pipeline-${baseBranch}", threshHoldName = 'SUCCESS')
        wsCleanup()
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

matrixJob("${folderPath}/DailyBuild-jbpmTestCoverageMatrix-${baseBranch}") {
    description("This job: <br> - Test coverage Matrix for jbpm <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")
    parameters {
        stringParam("kieVersion", "${kieVersion}", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>(7.1.0.20170514-productized) for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
        stringParam("baseBranch", "${baseBranch}", "please edit the branch of the KIE release <br> Will be supplied by the parent job. <br> Normally the baseBranch will be supplied by parent job <br> ******************************************************** <br> ")
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

// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// run additional test: jbpmContainerTestMatrix test
def jbpmContainerTest='''#!/bin/bash -e
echo "KIE version $kieVersion"
# wget the tar.gz sources
wget -q http://\${LOCAL_NEXUS_IP}:8081/nexus/content/repositories/kieAllBuild-$baseBranch/org/jbpm/jbpm/$kieVersion/jbpm-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
mv jbpm-$kieVersion/* .
rmdir jbpm-$kieVersion
'''

matrixJob("${folderPath}/DailyBuild-jbpmTestContainerMatrix-${baseBranch}") {
    description("Version to test. Will be supplied by the parent job. Also used to donwload proper sources. <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated.")
    parameters {
        stringParam("kieVersion", "${kieVersion}", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>(7.1.0.20170514-productized) for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
        stringParam("baseBranch", "${baseBranch}", "please edit the branch of the KIE release <br> Will be supplied by the parent job. <br> Normally the baseBranch will be supplied by parent job <br> ******************************************************** <br> ")

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
wget -q http://\${LOCAL_NEXUS_IP}:8081/nexus/content/repositories/kieAllBuild-$baseBranch/org/kie/kie-wb-distributions/$kieVersion/kie-wb-distributions-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
mv kie-wb-distributions-$kieVersion/* .
rmdir kie-wb-distributions-$kieVersion'''

matrixJob("${folderPath}/DailyBuild-kieWbTestsMatrix-${baseBranch}") {
    description("This job: <br> - Runs the KIE Server integration tests on mutiple supported containers and JDKs <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

    parameters {
        stringParam("kieVersion", "${kieVersion}", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>(7.1.0.20170514-productized) for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
        stringParam("baseBranch", "${baseBranch}", "please edit the branch of the KIE release <br> Will be supplied by the parent job. <br> Normally the baseBranch will be supplied by parent job <br> ******************************************************** <br> ")

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
        mailer('bsigs@redhat.com', false, false)
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

matrixJob("${folderPath}/DailyBuild-kieServerMatrix-${baseBranch}") {
    description("This job: <br> - Runs the KIE Server integration tests on mutiple supported containers and JDKs <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

    // Label which specifies which nodes this job can run on.
    label("master")

    parameters {
        stringParam("kieVersion", "${kieVersion}", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>or <b> major.minor.micro.<yyymmdd>-productized </b>(7.1.0.20170514-productized) for <b> productization </b> <br> Version to test. Will be supplied by the parent job. <br> Normally the KIE_VERSION will be supplied by parent job <br> ******************************************************** <br> ")
        stringParam("baseBranch", "${baseBranch}", "please edit the branch of the KIE release <br> Will be supplied by the parent job. <br> Normally the baseBranch will be supplied by parent job <br> ******************************************************** <br> ")

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

// *****************************************************************************************************
// definition of kieDockerCi  script

def kieDockerCi='''
sh scripts/docker-clean.sh $kieVersion
sh scripts/update-versions.sh $kieVersion -s "$SETTINGS_XML"'''

job("${dockerPath}/DailyBuild-docker-images-${baseBranch}") {
    description("Builds CI Docker images for master branch. <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

    parameters {
        stringParam("kieVersion", "${kieVersion}-SNAPSHOT", "Please edit the version of the kie release <br> i.e. typically <b> major.minor.micro.EXT </b>i.e. 8.0.0.Beta1<br> Normally the kie version will be supplied by parent job <br> ******************************************************** <br> ")
    }

    scm {
        git {
            remote {
                github("${organization}/kie-docker-ci-images")
            }
            branch ("${baseBranch}")
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
