import org.kie.jenkins.jobdsl.Constants

def kieVersion=Constants.KIE_PREFIX
def baseBranch=Constants.BRANCH
def releaseBranch="r7.29.0.Final"
def organization=Constants.GITHUB_ORG_UNIT
def m2Dir = Constants.LOCAL_MVN_REP
def MAVEN_OPTS="-Xms1g -Xmx3g"
def commitMsg="Upgraded version to "
def javadk=Constants.JDK_VERSION
def mvnVersion="kie-maven-3.5.2"
def binariesNR=1
String EAP7_DOWNLOAD_URL = "http://download.devel.redhat.com/released/JBoss-middleware/eap7/7.2.0/jboss-eap-7.2.0.zip"

// creation of folder
folder("community-release")

def folderPath="community-release"

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
                    sh 'sh droolsjbpm-build-bootstrap/script/git-clone-others.sh -b $baseBranch'
                }    
            }
        }
        // checks if release branch already exists
        stage ('Check if branch exists') {
            steps{
                sshagent(['kie-ci-user-key']) {
                    dir("${WORKSPACE}" + '/droolsjbpm-build-bootstrap') {
                        script {
                            branchExists = sh(script: 'git ls-remote --heads origin ${releaseBranch} | wc -l', returnStdout: true).trim()
                        } 
                    }
                }
            }
        }
        stage ('log results') {
            steps {
                echo 'branchExists: ' + "$branchExists"
                script {
                    if ( "$branchExists" == "1") {
                        echo "branch exists"
                    } else {
                        echo "branch does not exist"
                    } 
                }
            }
        }
        // if release branches doesn't exist they will be created
        stage('Create release branches') {
            when{
                expression { branchExists == '0'}
            }        
            steps {
                sshagent(['kie-ci-user-key']) {
                    sh 'sh droolsjbpm-build-bootstrap/script/release/02_createReleaseBranches.sh $releaseBranch $baseBranch'
                }    
            }
        } 
        // part of the Maven rep will be erased
        stage ('Remove M2') {
            steps {
                sh "sh droolsjbpm-build-bootstrap/script/release/eraseM2.sh $m2Dir"
            }
        }
        // poms will be upgraded to new version ($kieVersion)                         
        stage('Update versions') {
            when{
                expression { branchExists == '0'}
            }
            steps {
                echo 'kieVersion: ' + "{$kieVersion}"
                sh 'sh droolsjbpm-build-bootstrap/script/release/03_upgradeVersions.sh $kieVersion'
            }
        }
        stage ('Add and commit version upgrades') {
            when{
                expression { branchExists == '0'}
            }        
            steps {
                echo 'kieVersion: ' + "{$kieVersion}"
                echo 'commitMsg: ' + "{$commitMsg}"                
                sh 'sh droolsjbpm-build-bootstrap/script/release/addAndCommit.sh "$commitMsg" $kieVersion'
            }
        }
        //release branches are pushed to github
        stage('Push release branches') {
            when{
                expression { branchExists == '0'}
            }        
            steps {
                sshagent(['kie-ci-user-key']) {
                    sh 'sh droolsjbpm-build-bootstrap/script/release/04_pushReleaseBranches.sh $releaseBranch'
                }    
            }
        }
        // if the release branches already exist they will be fetched from github
        stage('Pull from existing release Branches') {
            when{
                expression { branchExists == '1'}
            }         
            steps {
                sshagent(['kie-ci-user-key']) {
                    sh 'sh droolsjbpm-build-bootstrap/script/git-all.sh fetch origin'
                    sh 'sh droolsjbpm-build-bootstrap/script/git-all.sh checkout ' + "$releaseBranch"
                }            
            }
        }
        // mvn clean deploy of each repository to a locally directory that will be uploaded later on - this saves time        
        stage('Build & deploy repositories locally'){
            when{
                expression { repBuild == 'YES'}
            }        
            steps {
                configFileProvider([configFile(fileId: '771ff52a-a8b4-40e6-9b22-d54c7314aa1e', targetLocation: 'jenkins-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                    sh './droolsjbpm-build-bootstrap/script/release/05a_communityDeployLocally.sh $SETTINGS_XML_FILE'
                }
            }
        }
        // create a directory where the binaries to upload to filemgmt.jboss.org are stored 
        stage('Create upload dir') {
            when{
                expression { repBuild == 'YES'}
            }        
            steps {
                script {
                    sh 'sh droolsjbpm-build-bootstrap/script/release/prepareUploadDir.sh'
                    sh 'cd "${kieVersion}"_uploadBinaries \\n' +
                       'totSize=$(du -sh) \\n' +
                       'echo "Total size of directory: " $totSize >> dirSize.txt \\n' +
                       'echo "" >> dirSize.txt \\n' +
                       'ls -l >> dirSize.txt'          
                }
            }        
        }
        // the directory of binaries will be compressed  
        stage('tar.gz uploadDir & archive artifacts'){
            when{
                expression { repBuild == 'YES'}
            }         
            steps {
                sh 'tar -czvf "${kieVersion}"_uploadBinaries.tar.gz "${kieVersion}"_uploadBinaries'
                archiveArtifacts '*.tar.gz'        
            }
        }                           
        stage('Publish JUnit test results reports') {
            when{
                expression { repBuild == 'YES'}
            }         
            steps {
              junit '**/target/*-reports/TEST-*.xml'    
            }
        }         
        // binaries created in previous step will be uploaded to Nexus
        stage('Upload binaries to staging repository to Nexus') {
            when{
                expression { repBuild == 'YES'}
            }         
            steps {
                configFileProvider([configFile(fileId: '3f317dd7-4d08-4ee4-b9bb-969c309e782c', targetLocation: 'uploadNexus-settings.xml', variable: 'SETTINGS_XML_FILE')]) {
                    sh './droolsjbpm-build-bootstrap/script/release/06_uploadBinariesToNexus.sh $SETTINGS_XML_FILE'
                }    
            }
        }
        // additional tests in separate Jenkins jobs will be exucuted
        stage('Additional tests') {
            steps {
            when{
                expression { repBuild == 'YES'}
            }            
                parallel (
                    "community-release-jbpmTestCoverageMatrix" : {
                        build job: "community-release-${baseBranch}-jbpmTestCoverageMatrix", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'baseBranch', value: baseBranch]]
                    },
                    "community-release-kieWbTestsMatrix" : {
                            build job: "community-release-${baseBranch}-kieWbTestsMatrix", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'baseBranch', value: baseBranch]]
                     },
                    "community-release-kieServerMatrix" : {
                            build job: "community-release-${baseBranch}-kieServerMatrix", propagate: false, parameters: [[$class: 'StringParameterValue', name: 'kieVersion', value: kieVersion], [$class: 'StringParameterValue', name: 'baseBranch', value: baseBranch]]
                    }
                )    
            } 
        }
        // after a first build this email will be send               
        stage ('1st email send with BUILD result') {
            when{
                expression { branchExists == '0' && repBuild == 'YES'}
            }        
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
                    '${BUILD_LOG, maxLines=750}', subject: 'community-release-${baseBranch} ${kieVersion} status and artefacts for sanity checks', to: 'kie-jenkins-builds@redhat.com'
            }    
        }
        // if after sanity checks a second build is requested this mail will be send
        stage ('2nd email send with BUILD result') {
            when{
                expression { branchExists == '1' && repBuild == 'YES'}
            }        
            steps {
                emailext body: 're-build of community ${kieVersion} after sanity checks was:  ' + "${currentBuild.currentResult}" +  '\\n' +
                    ' \\n' +
                    'PLEASE CHECK IF THE BLOCKERS DETECTED DURING SANITY CHECKS ARE FIXED NOW \\n' +
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
                    '${BUILD_LOG, maxLines=750}', subject: 'community-release-${baseBranch} ${kieVersion} re-build after sanity checks', to: 'kie-jenkins-builds@redhat.com\'
            }    
        }        
        // user interaction required: continue or abort
        stage('Approval (Point of NO return)') {
            steps {
                input message: 'Was the build stable enough to do a release', ok: 'Continue with releasing'
            }
        }
        // the tags of the release will be created and pushed to github
        stage('Push community tag') {
            steps {
                sh 'sh droolsjbpm-build-bootstrap/script/release/08a_communityPushTags.sh'
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
                 'kie = $kieVersion', subject: 'community-release-${baseBranch} $kieVersion was released', to: 'bsig@redhat.com\'
            }
        }         
        // if a the pipeline job was executed again but without building the binaries from uploading to filemgmt.jboss.org are needed
        stage('BUILD NUMBER of desired binaries') {
            when{
                expression { repBuild == 'NO'}
            }
            //interactive step: user should select the BUILD Nr of the rtifacts to restore            
            steps {
                script {
                    binariesNR = input id: 'binariesID', message: 'Which build number has the desired binaries \\n DBN (desired build number)', parameters: [string(defaultValue: '', description: '', name: 'DBN')]
                    echo 'BUILD_NUMBER= ' + "$binariesNR"
                }    
            }
        }
        // binaries to upload to filemgmt.jbosss.org, saved in a previous build will be untared
        stage('Pull binaries of previous build') {
            when{
                expression { repBuild == 'NO'}
            } 
            steps {
                echo 'BUILD NUMBER= ' + "$binariesNR"
                step([  $class: 'CopyArtifact',
                    filter: '*.tar.gz',
                    fingerprintArtifacts: true,
                    projectName: '${JOB_NAME}',
                    selector: [$class: 'SpecificBuildSelector', buildNumber: "$binariesNR"]
                ])
                sh 'tar -xzvf "${kieVersion}"_uploadBinaries.tar.gz \\n' +
                   'ls -al'
            }        
        }
        stage('Archive artifacts') {
            when{
                expression { repBuild == 'NO'}
            }            
            steps {
                archiveArtifacts '*.tar.gz'
            }
        }        
        stage('Create jbpm installers') {
            steps {
                sh 'sh droolsjbpm-build-bootstrap/script/release/09_createjBPM_installers.sh'
            }
        }        
        stage('Push binaries to filemgmgt.jboss.org') {
            steps {
                sshagent(['jenkins-ci-filemgmt']) {
                    sh 'sh droolsjbpm-build-bootstrap/script/release/10_uploadBinariesToFilemgmt.sh'
                }    
            }
        }                                                                      
    }
}
'''


pipelineJob("${folderPath}/community-release-pipeline-${baseBranch}") {

    description('this is a pipeline job for a community release /tag of all reps')


    parameters{
        stringParam("kieVersion", "${kieVersion}", "Please edit the version of kie i.e 7.28.0.Final ")
        stringParam("baseBranch", "${baseBranch}", "Please edit the name of the kie branch ")
        stringParam("releaseBranch", "${releaseBranch}", "Please edit name of the releaseBranch - i.e. r7.28.0.Final ")
        stringParam("organization", "${organization}", "Please edit the name of organization ")
        choiceParam("repBuild",["YES", "NO"],"Please select if<br>you want to do a new build = YES<br>a new build is not required and artifacts are already uploaded to Nexus = NO ")
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
        wHideParameterDefinition {
            name('m2Dir')
            defaultValue("${m2Dir}")
            description('Path to .m2/repository')
        }
        wHideParameterDefinition {
            name('binariesNR')
            defaultValue("${binariesNR}")
            description('')
        }
    }

    logRotator {
        numToKeep(3)
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
# wget the tar.gz sources
wget -q https://repository.jboss.org/nexus/content/groups/kie-group/org/jbpm/jbpm/$kieVersion/jbpm-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
mv jbpm-$kieVersion/* .
rmdir jbpm-$kieVersion
'''

matrixJob("${folderPath}/community-release-${baseBranch}-jbpmTestCoverageMatrix") {
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
            mavenSettings("3f317dd7-4d08-4ee4-b9bb-969c309e782c"){
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
            providedSettings("3f317dd7-4d08-4ee4-b9bb-969c309e782c")
        }
    }
}

// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

//  run additional test: kieWbTestsMatrix
def kieWbTest='''#!/bin/bash -e
# wget the tar.gz sources
wget -q https://repository.jboss.org/nexus/content/groups/kie-group/org/kie/kie-wb-distributions/$kieVersion/kie-wb-distributions-$kieVersion-project-sources.tar.gz  -O sources.tar.gz
tar xzf sources.tar.gz
mv kie-wb-distributions-$kieVersion/* .
rmdir kie-wb-distributions-$kieVersion

echo "KIE version $kieVersion - kie-wb-common"
wget -q https://repository.jboss.org/nexus/content/groups/kie-group/org/kie/workbench/kie-wb-common/$kieVersion/kie-wb-common-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
rm sources.tar.gz
mv kie-wb-common-$kieVersion/* .
rmdir kie-wb-common-$kieVersion'''

matrixJob("${folderPath}/community-release-${baseBranch}-kieWbTestsMatrix") {
    description("This job: <br> - Runs the KIE WB integration tests on mutiple supported containers and JDKs <br> IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will get lost next time the job is generated. ")

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
            mavenSettings("3f317dd7-4d08-4ee4-b9bb-969c309e782c") {
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
            providedSettings("3f317dd7-4d08-4ee4-b9bb-969c309e782c")
        }
        maven{
            mavenInstallation("${mvnVersion}")
            goals("-nsu -B -e -fae clean verify -Dintegration-tests=true")
            rootPOM("kie-wb-common-dmn/kie-wb-common-dmn-webapp-kogito-runtime/pom.xml")
            properties("webdriver.firefox.bin":"/opt/tools/firefox-60esr/firefox-bin")
            mavenOpts("-Xms1024m -Xmx1536m")
            providedSettings("3f317dd7-4d08-4ee4-b9bb-969c309e782c")
        }
    }
}

// +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  run additional test: kieServerMatrix
def kieServerTest='''#!/bin/bash -e
# wget the tar.gz sources
wget -q https://repository.jboss.org/nexus/content/groups/kie-group/org/drools/droolsjbpm-integration/$kieVersion/droolsjbpm-integration-$kieVersion-project-sources.tar.gz -O sources.tar.gz
tar xzf sources.tar.gz
mv droolsjbpm-integration-$kieVersion/* .
rmdir droolsjbpm-integration-$kieVersion'''

matrixJob("${folderPath}/community-release-${baseBranch}-kieServerMatrix") {
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
            mavenSettings("3f317dd7-4d08-4ee4-b9bb-969c309e782c") {
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
            providedSettings("3f317dd7-4d08-4ee4-b9bb-969c309e782c")
        }
    }
}
