package osbs

import org.kie.jenkins.jobdsl.Constants

def folderPath = 'OSBS/rhpam-kogito-images'
folder('OSBS')
folder('OSBS/rhpam-kogito-images')
// Job Description
String jobDescription = 'Job responsible for seed jobs to building rhpam kogito images'

//Define Variables
def prodComponent = [
        'rhpam-kogito-builder-rhel8', 'rhpam-kogito-runtime-jvm-rhel8', 'rhpam-kogito-runtime-native-rhel8']

def buildDate = Constants.BUILD_DATE
def prodVersion = Constants.NEXT_PROD_VERSION
def osbsBuildTarget = Constants.OSBS_BUILD_TARGET
def cekitBuildOptions = Constants.CEKIT_BUILD_OPTIONS
def osbsBuildUser = Constants.OSBS_BUILD_USER
def kerberosPrincipal = Constants.KERBEROS_PRINCIPAL
def kerberosKeytab = Constants.KERBEROS_KEYTAB
def kerberosCred = Constants.KERBEROS_CRED
def imageRepo = Constants.IMAGE_REPO
def imageBranch = Constants.IMAGE_BRANCH
def imageSubdir = Constants.IMAGE_SUBDIR
def gitUser = Constants.GIT_USER
def gitEmail = Constants.GIT_EMAIL
def cekitCacheLocal = Constants.CEKIT_CACHE_LOCAL
def verbose = Constants.VERBOSE
def githubOrgUnit = Constants.GITHUB_ORG_UNIT
def rhpamKogitoImageBranch = Constants.RHPAM_KOGITO_IMAGE_BRANCH
def rhpamKogitoImageCekitOSBSSubdir = Constants.RHPAM_BA_OPERTOR_CEKIT_OSBS_SUBDIR

prodComponent.each { Component ->

    pipelineJob("${folderPath}/${Component}") {
        parameters {
            stringParam('BUILD_DATE', "${buildDate}")
            stringParam('PROD_VERSION', "${prodVersion}")
            stringParam('PROD_COMPONENT', "${Component}")
            stringParam('OSBS_BUILD_TARGET', "${osbsBuildTarget}")
            stringParam('CEKIT_BUILD_OPTIONS', "${cekitBuildOptions}")
            stringParam('KERBEROS_PRINCIPAL', "${kerberosPrincipal}")
            stringParam('OSBS_BUILD_USER', "${osbsBuildUser}")
            stringParam('KERBEROS_KEYTAB', "${kerberosKeytab}")
            stringParam('KERBEROS_CRED', "${kerberosCred}")
            stringParam('IMAGE_REPO', "${imageRepo}")
            stringParam('IMAGE_BRANCH', "${imageBranch}")
            stringParam('IMAGE_SUBDIR', "${imageSubdir}")
            stringParam('GIT_USER', "${gitUser}")
            stringParam('GIT_EMAIL', "${gitEmail}")
            stringParam('CEKIT_CACHE_LOCAL', "${cekitCacheLocal}")
            stringParam('VERBOSE', "${verbose}")
            stringParam('GITHUB_ORG_UNIT', "${githubOrgUnit}")
            stringParam('RHPAM_KOGITO_IMAGE_BRANCH', "${rhpamKogitoImageBranch}")
            stringParam('CEKIT_OSBS_SUBDIR', "/home/jenkins/.cekit/osbs/containers/${Component}")
        }

        definition {
            cps {
                script('''

                    library 'jenkins-pipeline-shared-libraries'
                    
                    TIMEOUT = 2

                    pipeline {
                        options {
                            timeout(time: TIMEOUT, unit: 'HOURS')
                        }
                    
                        agent {
                            label 'osbs-builder && docker && rhel8'
                        }
                    
                        environment {
                            PRODUCT_NAME = PROD_COMPONENT.trim()
                            VERSION = PROD_VERSION.trim()
                        }
                    
                        stages {
                            stage('Clean workspace') {
                                steps {
                                    cleanWs()
                                }
                            }
                    
                            stage('Building image') {
                                steps {
                                    // Add the working directory to the current path so any scripts dropped there are on the path
                                    withEnv(['PATH+W=$WORKSPACE']) {
                                        sh "rm -rf ${WORKSPACE}/{*,.*} || true"
                                        script {
                                            if (env.KERBEROS_CRED) {
                                                withCredentials([usernamePassword(credentialsId: env.KERBEROS_CRED, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                                                    mainProcess(USERNAME, PASSWORD, '')
                                                }
                                            } else if (env.KERBEROS_KEYTAB) {
                                                withCredentials([file(credentialsId: env.KERBEROS_KEYTAB, variable: 'FILE')]) {
                                                    if (!env.KERBEROS_PRINCIPAL) {
                                                        echo 'Reading the Kerberos Principal from provided Keytab...'
                                                        def get_principal_from_file = sh(returnStdout: true, script: \'\'\'
                                                            #!/bin/bash
                                                            klist -kt $FILE |grep REDHAT.COM | awk -F" " 'NR==1{print $4}'
                                                        \'\'\')
                                                        env.KERBEROS_PRINCIPAL = get_principal_from_file.trim()
                                                    }
                                                    mainProcess(env.KERBEROS_PRINCIPAL, '', FILE)
                                                }
                                            } else {
                                                error 'Either KERBEROS_PRINCIPAL and KERBEROS_KEYTAB must be specified, or user/password with KERBEROS_CRED'
                                            }
                                        }
                                    }
                                }
                            }
                            
                            stage('persist image name into a txt file') {
                                steps {
                                    echo "Persisting the ${env.BUILT_IMAGE} to a file..."
                                    writeFile file: "${PROD_COMPONENT}-image-location.txt", text: "${env.BUILT_IMAGE}"
                                }    
                            }
                            
                            stage('Prepare kogito-examples') {
                                steps {
                                    script {
                                        catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                                            sh "make clone-repos"
                                        }
                                    }
                                }    
                            }
                            
                            stage('execute behave tests') {
                                steps {
                                    script {
                                        // pull from brew registry
                                        echo "Pulling the ${env.BUILT_IMAGE} image..."
                                        sh "docker pull ${env.BUILT_IMAGE}"
                                        
                                        // tag to the expected image name
                                        def tagTo = "rhpam-7/${env.PROD_COMPONENT}:${env.PROD_VERSION}"
                                        sh "docker tag ${env.BUILT_IMAGE} ${tagTo}"
                                        // set IMAGE_VERSION with rc on it to avoid the image tag.
                                        catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                                            sh "source ~/virtenvs/cekit/bin/activate && make build-image image_name=${env.PROD_COMPONENT} ignore_build=true ignore_test=false IMAGE_VERSION=0.0.0-rc"
                                        }
                                    }    
                                }    
                            }                             
                        }    
                        post {
                            always {
                                archiveArtifacts artifacts: "${PROD_COMPONENT}-image-location.txt", onlyIfSuccessful: true
                            }
                            cleanup {
                                cleanWs()
                            }
                        }                      
                    }
                    
                    
                    
                    // Auxiliary Functions
                    private void validateParameters(required, optionals){
                        // Check if all required params are supplied
                        for ( param in required ) {
                            def arg = param.value[0]
                            def flag = param.value[1]
                            if(!param.value[0]){
                                error "${param.key} parameter is required but was not specified."
                            }
                        }
                    }
                                        
                    private void mainProcess(user, password, keytab){
                        // Parameters available for the build.
                        def REQUIRED_BUILD_PARAMETERS = [
                                'PROD_VERSION': [PROD_VERSION, '-v'],
                                'PROD_COMPONENT': [PROD_COMPONENT, '-c'],
                                'OSBS_BUILD_TARGET': [OSBS_BUILD_TARGET, '-t'],
                                'CEKIT_OSBS_SUBDIR': [CEKIT_OSBS_SUBDIR, '-d'],
                        ]
                        def OPTIONAL_BUILD_PARAMETERS = [
                                'KERBEROS_PASSWORD': [password, '-s'],
                                'KERBEROS_PRINCIPAL': [user, '-p'],
                                'KERBEROS_KEYTAB': [keytab, '-k'],
                                'OSBS_BUILD_USER': [OSBS_BUILD_USER, '-i'],
                                'BUILD_DATE': [BUILD_DATE, '-b'],
                                'GIT_USER': [GIT_USER, '-u'],
                                'GIT_EMAIL': [GIT_EMAIL, '-e'],
                                'CEKIT_BUILD_OPTIONS': [CEKIT_BUILD_OPTIONS, '-o'],
                                'CEKIT_CACHE_LOCAL': [CEKIT_CACHE_LOCAL, '-l'],
                        ]
                        def OPTIONAL_BUILD_SWITCHES = [
                                'VERBOSE': [VERBOSE, '-g'],
                        ]
                        
                        // The build script is in the image, but build.sh and build-overrides.sh which it calls will be downloaded
                        def build_command = 'build-osbs.sh'
                    
                        // Create the build command
                        validateParameters(REQUIRED_BUILD_PARAMETERS, OPTIONAL_BUILD_PARAMETERS)
                    
                        for(param in REQUIRED_BUILD_PARAMETERS){
                            build_command += " ${param.value[1]} ${param.value[0]}"
                        }
                       
                        for(param in OPTIONAL_BUILD_PARAMETERS){
                            def arg = param.value[0]
                            def flag = param.value[1]
                            build_command+= arg ? " ${flag} ${arg}" : ''
                        } 
                        
                        for(param in OPTIONAL_BUILD_SWITCHES){
                            def arg = param.value[0]
                            def flag = param.value[1]
                            build_command+= arg == 'true' ? " ${flag}" : ''
                        }             
                        
                        build_command +=" -w ${WORKSPACE}"
                        // checkout the kogito-images repository in the target branch.                  
                        checkout(githubscm.resolveRepository('kogito-images', GITHUB_ORG_UNIT, RHPAM_KOGITO_IMAGE_BRANCH, false))
                          
                        // Run the build script that should be into the operator hack folder
                        dir('scripts') {
                            sh "source ~/virtenvs/cekit/bin/activate && ./${build_command} | tee ../output.txt"
                        }
                        
                        // post processing
                        // query the built image from osbs using brew cli  
                        def get_image_name = sh(returnStdout: true, script: \'\'\'
                            RESULT=$(/usr/bin/brew call --json-output getTaskResult $( cat output.txt| grep -oP 'Task \\\\d{8}' | cut -d" " -f2) | jq -nre "input.repositories[0]")
                            if [ $? != 0 ]; then
                                echo "Unable to find build image - $RESULT"
                                exit 1
                            fi
                            # if no issue happens, the result should be the built image
                            echo ${RESULT}
                            \'\'\')
                        env.BUILT_IMAGE = "${get_image_name.trim()}"  
                    }
                '''.stripIndent())
                sandbox()
            }
        }
    }
}