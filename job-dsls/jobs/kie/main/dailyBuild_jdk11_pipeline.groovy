import org.kie.jenkins.jobdsl.Constants

def javadk="kie-jdk11"
def kieVersion=Constants.KIE_PREFIX
def baseBranch=Constants.BRANCH
def organization=Constants.GITHUB_ORG_UNIT
def deployDir="deploy-dir"
def m2Dir = Constants.LOCAL_MVN_REP
def mvnVersion="kie-maven-" + Constants.MAVEN_VERSION
def AGENT_LABEL="kie-linux&&kie-rhel7&&kie-mem24g"

String EAP7_DOWNLOAD_URL = "http://download.devel.redhat.com/released/JBoss-middleware/eap7/7.3.0/jboss-eap-7.3.0.zip"

// creation of folder
folder("KIE")
folder ("KIE/${baseBranch}")
folder("KIE/${baseBranch}/daily-build-jdk11")

def folderPath = "KIE/${baseBranch}/daily-build-jdk11"

def daily_build='''
pipeline {
    agent {
        label "$AGENT_LABEL"
    }
    options{
        timestamps()
    }    
    tools {
        maven "$mvnVersion"
        jdk "$javadk"
    }
    stages {
        stage('CleanWorkspace') {
            steps {
                cleanWs()
            }
        }
        stage('build sh script') {
            steps {
                script {
                    sh 'touch trace.sh'
                    sh 'chmod 755 trace.sh'
                    sh 'echo "wget --no-check-certificate ${BUILD_URL}consoleText" >> trace.sh'
                    sh 'echo "tail -n 1000 consoleText >> error.log" >> trace.sh'
                    sh 'echo "gzip error.log" >> trace.sh'
                    sh 'cat trace.sh'                
                }
            }
        }
        stage('Calculate versions') {
            steps {
                script {
                    data = new Date().format('yyMMdd-hh')
                    kieVersion = "${kieVersion}.${data}"

                    echo "data: ${data}"
                    echo "kieVersion: ${kieVersion}"
                    echo "baseBranch: ${baseBranch}"
                    echo "organization: ${organization}"
                }
            }
        }
        stage ('Checkout droolsjbpm-build-boostrap') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '$baseBranch']], browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/$organization/droolsjbpm-build-bootstrap'], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'droolsjbpm-build-bootstrap']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'kie-ci-user-key', url: 'https://github.com/$organization/droolsjbpm-build-bootstrap.git']]])
                dir("${WORKSPACE}" + '/droolsjbpm-build-bootstrap') {
                    sh 'pwd \\n' +
                       'git branch \\n' +
                       'git checkout -b $baseBranch\'
                } 
            }
        }
        stage('Clone all other reps') {
            steps {        
                sshagent(['kie-ci-user-key']) {        
                    sh "./droolsjbpm-build-bootstrap/script/release/01_cloneBranches.sh $baseBranch"
                }
            }   
        }
        stage ('Remove M2') {
            steps {
                sh "./droolsjbpm-build-bootstrap/script/release/eraseM2.sh $m2Dir"
            }
        }         
        stage('Update versions') {
            steps {
                sh "echo 'kieVersion: $kieVersion'"
                sh "./droolsjbpm-build-bootstrap/script/release/03_upgradeVersions.sh $kieVersion"
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
                    sh "./droolsjbpm-build-bootstrap/script/release/05c_dailyBuildDeployLocally.sh $SETTINGS_XML_FILE"
                }
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
                        build job: "daily-build-jdk11-${baseBranch}-jbpmTestCoverageMatrix", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'baseBranch', value: baseBranch]]
                    },
                    "jbpmTestContainerMatrix" : {
                        build job: "daily-build-jdk11-${baseBranch}-jbpmTestContainerMatrix", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'baseBranch', value: baseBranch]]
                    },
                    "kieWbTestsMatrix" : {
                            build job: "daily-build-jdk11-${baseBranch}-kieWbTestsMatrix", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'baseBranch', value: baseBranch]]
                     },
                    "kieServerMatrix" : {
                            build job: "daily-build-jdk11-${baseBranch}-kieServerMatrix", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'baseBranch', value: baseBranch]]
                    }
                )    
            } 
        }                                  
    }
    post {
        always {
            script {
                sh './trace.sh'
            }
            junit '**/target/surefire-reports/**/*.xml'
        }
        failure{
            emailext body: 'Build log: ${BUILD_URL}consoleText\\n' +
                           'Failed tests (${TEST_COUNTS,var="fail"}): ${BUILD_URL}testReport\\n' +
                           '(IMPORTANT: For visiting the links you need to have access to Red Hat VPN. In case you do not have access to RedHat VPN please download and decompress attached file.)',
                     subject: 'Build #${BUILD_NUMBER} of daily builds ${baseBranch} branch with JDK 11 FAILED',
                     to: 'kie-jenkins-builds@redhat.com',
                     attachmentsPattern: 'error.log.gz'
            cleanWs()                     
        }
        unstable{
            emailext body: 'Build log: ${BUILD_URL}consoleText\\n' +
                           'Failed tests (${TEST_COUNTS,var="fail"}): ${BUILD_URL}testReport\\n' +
                           '***********************************************************************************************************************************************************\\n' +
                           '${FAILED_TESTS}',
                     subject: 'Build #${BUILD_NUMBER} of daily builds ${baseBranch} branch with JDK 11 was UNSTABLE',
                     to: 'kie-jenkins-builds@redhat.com'
            cleanWs()         
        }
        fixed {
            emailext body: '',
                 subject: 'Build #${BUILD_NUMBER} of daily builds ${baseBranch} branch with JDK 11 is fixed and was SUCCESSFUL',
                 to: 'kie-jenkins-builds@redhat.com'
        }
        success {
            cleanWs()
        }                    
    }      
}
'''

pipelineJob("${folderPath}/daily-build-jdk11-pipeline-${baseBranch}") {

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
        wHideParameterDefinition {
            name('m2Dir')
            defaultValue("${m2Dir}")
            description('Path to .m2/repository')
        }
        wHideParameterDefinition {
            name('AGENT_LABEL')
            defaultValue("${AGENT_LABEL}")
            description('name of machine where to run this job')
        }
        wHideParameterDefinition {
            name('mvnVersion')
            defaultValue("${mvnVersion}")
            description('version of maven')
        }
        wHideParameterDefinition {
            name('javadk')
            defaultValue("${javadk}")
            description('version of jdk')
        }
    }

    logRotator {
        numToKeep(5)
    }

    definition {
        cps {
            script("${daily_build}")
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
wget -q https://\${LOCAL_NEXUS_IP}:8443/nexus/content/repositories/kieAllBuild-$baseBranch/org/jbpm/jbpm/$kieVersion/jbpm-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
rm sources.tar.gz
mv jbpm-$kieVersion/* .
rm -rf jbpm-$kieVersion
'''

matrixJob("${folderPath}/daily-build-jdk11-${baseBranch}-jbpmTestCoverageMatrix") {
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
        numToKeep(5)
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
        mailer('kie-jenkins-builds@redhat.com', false, false)
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
wget -q https://\${LOCAL_NEXUS_IP}:8443/nexus/content/repositories/kieAllBuild-$baseBranch/org/jbpm/jbpm/$kieVersion/jbpm-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
rm sources.tar.gz
mv jbpm-$kieVersion/* .
rm -rf jbpm-$kieVersion
'''

matrixJob("${folderPath}/daily-build-jdk11-${baseBranch}-jbpmTestContainerMatrix") {
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
        numToKeep(5)
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
        mailer('kie-jenkins-builds@redhat.com', false, false)
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
echo "KIE version $kieVersion - kie-wb-distributions"
wget -q https://\${LOCAL_NEXUS_IP}:8443/nexus/content/repositories/kieAllBuild-$baseBranch/org/kie/kie-wb-distributions/$kieVersion/kie-wb-distributions-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
rm sources.tar.gz
mv kie-wb-distributions-$kieVersion/* .
rm -rf kie-wb-distributions-$kieVersion

echo "KIE version $kieVersion - kie-wb-common"
wget -q https://\${LOCAL_NEXUS_IP}:8443/nexus/content/repositories/kieAllBuild-$baseBranch/org/kie/workbench/kie-wb-common/$kieVersion/kie-wb-common-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
rm sources.tar.gz
mv kie-wb-common-$kieVersion/* .
rm -rf kie-wb-common-$kieVersion'''

matrixJob("${folderPath}/daily-build-jdk11-${baseBranch}-kieWbTestsMatrix") {
    description("This job: <br> - Runs the KIE WB integration tests on mutiple supported containers and JDKs <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

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
        numToKeep(5)
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
        archiveJunit("**/target/*-reports/TEST-*.xml")
        archiveArtifacts("**/target/screenshots/*")
        mailer('kie-jenkins-builds@redhat.com', false, false)
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
wget -q https://\${LOCAL_NEXUS_IP}:8443/nexus/content/repositories/kieAllBuild-$baseBranch/org/drools/droolsjbpm-integration/$kieVersion/droolsjbpm-integration-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
rm sources.tar.gz
mv droolsjbpm-integration-$kieVersion/* .
rm -rf droolsjbpm-integration-$kieVersion'''

matrixJob("${folderPath}/daily-build-jdk11-${baseBranch}-kieServerMatrix") {
    description("This job: <br> - Runs the KIE Server integration tests on mutiple supported containers and JDKs <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

    // Label which specifies which nodes this job can run on.
    label("kie-rhel7&&kie-mem8g&&!master")

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
        numToKeep(5)
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
        mailer('kie-jenkins-builds@redhat.com', false, false)
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

