import org.kie.jenkins.jobdsl.Constants

def folderPath = 'OSBS/rhpam-images'
folder('OSBS')
folder('OSBS/rhpam-images')
// Job Description
String jobDescription = "Job responsible for seed jobs to building rhpam openshift image"

//Define Variables
def prodComponent = [
        'rhpam-businesscentral', 'rhpam-businesscentral-monitoring',
        'rhpam-controller', 'rhpam-kieserver', 'rhpam-smartrouter',
        'rhpam-process-migration', 'rhpam-dashbuilder']

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

prodComponent.each { Component ->

    pipelineJob("${folderPath}/${Component}") {

        parameters {
            stringParam("BUILD_DATE", "${buildDate}")
            stringParam("PROD_VERSION", "${prodVersion}")
            stringParam("PROD_COMPONENT", "${Component}")
            stringParam("OSBS_BUILD_TARGET", "${osbsBuildTarget}")
            stringParam("CEKIT_BUILD_OPTIONS", "${cekitBuildOptions}")
            stringParam("KERBEROS_PRINCIPAL", "${kerberosPrincipal}")
            stringParam("OSBS_BUILD_USER", "${osbsBuildUser}")
            stringParam("KERBEROS_KEYTAB", "${kerberosKeytab}")
            stringParam("KERBEROS_CRED", "${kerberosCred}")
            stringParam("IMAGE_REPO", "${imageRepo}")
            stringParam("IMAGE_BRANCH", "${imageBranch}")
            stringParam("IMAGE_SUBDIR", "${imageSubdir}")
            stringParam("GIT_USER", "${gitUser}")
            stringParam("GIT_EMAIL", "${gitEmail}")
            stringParam("CEKIT_CACHE_LOCAL", "${cekitCacheLocal}")
            stringParam('PROPERTY_FILE_URL', '', 'the properties file url for the given build. It is expected that the property file url points to the nightly builds and contains the build date within it.')
            stringParam("VERBOSE", "${verbose}")
        }

        logRotator {
            numToKeep(5)
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

                            stage('Building Image') {
                                steps {
                                    // Add the working directory to the current path so any scripts dropped there are on the path
                                    withEnv(["PATH+W=$WORKSPACE"]) {
                                        sh 'rm -rf ${WORKSPACE}/{*,.*} || true'
                                        script {
                                            if (env.KERBEROS_CRED) {
                                                withCredentials([usernamePassword(credentialsId: env.KERBEROS_CRED, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                                                    mainProcess(USERNAME, PASSWORD, "")
                                                }
                                            } else if (env.KERBEROS_KEYTAB) {
                                                withCredentials([file(credentialsId: env.KERBEROS_KEYTAB, variable: 'FILE')]) {
                                                    if (!env.KERBEROS_PRINCIPAL) {
                                                          echo "Reading the Kerberos Principal from provided Keytab..."
                                                          def get_principal_from_file = sh(returnStdout: true, script: \'\'\'
                                                              #!/bin/bash
                                                              klist -kt $FILE |grep REDHAT.COM | awk -F" " 'NR==1{print $4}'
                                                          \'\'\')
                                                          env.KERBEROS_PRINCIPAL = get_principal_from_file.trim()
                                                    }  
                                                    mainProcess(env.KERBEROS_PRINCIPAL, "", FILE)
                                                }
                                            } else {
                                                error "Either KERBEROS_PRINCIPAL and KERBEROS_KEYTAB must be specified, or user/password with KERBEROS_CRED"
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
                            
                            stage('execute behave tests') {
                                steps {
                                    script {
                                        // pull from brew registry
                                        echo "Pulling the ${env.BUILT_IMAGE} image..."
                                        sh "docker pull ${env.BUILT_IMAGE}"
                                        
                                        // tag to the expected image name
                                        def tagTo = "rhpam-7/${env.PROD_COMPONENT}-rhel8:${env.PROD_VERSION}"
                                        sh "docker tag ${env.BUILT_IMAGE} ${tagTo}"
                                        
                                        def get_dir = sh(returnStdout: true, script: \'\'\'
                                            #!/bin/bash
                                            echo ${PROD_COMPONENT} | cut -d- -f2-
                                        \'\'\')
                                        
                                        catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                                            sh "source ~/virtenvs/cekit/bin/activate && cd rhba-repo/${get_dir.trim()} && cekit --verbose --redhat test --overrides-file branch-overrides.yaml behave"
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
                            if(!arg){
                                error "$param.key parameter is required but was not specified."
                            }
                        }
                    }

                    private void mainProcess(user, password, keytab){
                        // Parameters available for the build.
                        def REQUIRED_BUILD_PARAMETERS = [
                            "PROD_VERSION": [PROD_VERSION, '-v'],
                            "PROD_COMPONENT": [PROD_COMPONENT, '-c'],
                            "OSBS_BUILD_TARGET": [OSBS_BUILD_TARGET, '-t'],
                        ]

                        def OPTIONAL_BUILD_PARAMETERS = [
                            "PROPERTY_FILE_URL": [PROPERTY_FILE_URL, '-f'],
                            "KERBEROS_PASSWORD": [password, '-s'],
                            "KERBEROS_PRINCIPAL": [user, '-p'],
                            "KERBEROS_KEYTAB": [keytab, '-k'],
                            "OSBS_BUILD_USER": [OSBS_BUILD_USER, '-i'],
                            "BUILD_DATE": [BUILD_DATE, '-b'],
                            "GIT_USER": [GIT_USER, '-u'],
                            "CEKIT_BUILD_OPTIONS": [CEKIT_BUILD_OPTIONS, '-o'],
                            "CEKIT_CACHE_LOCAL": [CEKIT_CACHE_LOCAL, '-l'],
                        ]

                        def OPTIONAL_BUILD_SWITCHES = [
                            "VERBOSE": [VERBOSE, '-g'],
                        ]

                        def REQUIRED_DOWNLOAD_PARAMETERS = [
                            "PROD_VERSION": [PROD_VERSION, '-v'],
                            "PROD_COMPONENT": [PROD_COMPONENT, '-c'],
                        ]

                        def OPTIONAL_DOWNLOAD_PARAMETERS = [
                            "IMAGE_REPO": [IMAGE_REPO, '-r'],
                            "IMAGE_BRANCH": [IMAGE_BRANCH, '-n'],
                            "IMAGE_SUBDIR": [IMAGE_SUBDIR, '-d'],
                        ]

                        // The download script is in the image, but build.sh and build-overrides.sh which it calls will be downloaded
                        def download_command = "/opt/rhba/download.sh"
                        def build_command = "build-osbs.sh"

                        // Create the download command to set up the build directory
                        validateParameters(REQUIRED_DOWNLOAD_PARAMETERS, OPTIONAL_DOWNLOAD_PARAMETERS)
                        for(param in REQUIRED_DOWNLOAD_PARAMETERS){
                            def arg = param.value[0]
                            def flag = param.value[1]
                            download_command += " ${flag} ${arg}"
                        }

                        for(param in OPTIONAL_DOWNLOAD_PARAMETERS){
                            def arg = param.value[0]
                            def flag = param.value[1]
                            if(arg) download_command+= " ${flag} ${arg}"
                        }

                        download_command += " -w ${WORKSPACE}"

                        // Create the build command
                        validateParameters(REQUIRED_BUILD_PARAMETERS, OPTIONAL_BUILD_PARAMETERS)

                        for(param in REQUIRED_BUILD_PARAMETERS){
                            def arg = param.value[0]
                            def flag = param.value[1]
                            build_command += " ${flag} ${arg}"
                        }

                        for(param in OPTIONAL_BUILD_PARAMETERS){
                            def arg = param.value[0]
                            def flag = param.value[1]
                            if(arg) build_command+= " ${flag} ${arg}"
                        }

                        for(param in OPTIONAL_BUILD_SWITCHES){
                            def arg = param.value[0]
                            def flag = param.value[1]
                            if(arg == "true") build_command+= " ${flag}"
                        }

                        build_command +=" -w ${WORKSPACE}"

                        // Run the download script to set up the build. This will select the right branch
                        // and return the component directory path where the build needs to take path
                        def component_path = sh(script: "$download_command 2>&1", returnStdout: true)
                        println component_path

                        // This gets the last token of output from the script, which is the path to build in
                        component_path = component_path.tokenize().last()

                        // Run the build script
                        dir(component_path) {
                            sh "source ~/virtenvs/cekit/bin/activate && $build_command | tee output.txt"
                        }

                        // post processing
                        // query the built image from osbs using brew cli
                        dir(component_path) {
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
                    }
                '''.stripIndent())
                sandbox()
            }
        }
    }
}