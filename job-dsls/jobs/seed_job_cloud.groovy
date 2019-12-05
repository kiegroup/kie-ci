import org.kie.jenkins.jobdsl.Constants

// Job Description
String jobDescription = "Job responsible for seed jobs to building rhpm/rhdm openshift image"

//Define Variables
def folderPath = 'OSBS'
def prodComponent = ['rhpam-businesscentral','rhpam-businesscentral-monitoring','rhpam-controller','rhpam-kieserver','rhpam-smartrouter','rhdm-decisioncentral','rhdm-controller','rhdm-kieserver','rhdm-optaweb-employee-rostering']

folder(folderPath) 
      
prodComponent.each { COMPONENT ->
            
   pipelineJob("${folderPath}/${COMPONENT}")  {

    parameters {
        stringParam("BUILD_DATE", "${BUILD_DATE}")
        stringParam("PROD_VERSION", "${PROD_VERSION}")
        stringParam("PROD_COMPONENT", "${COMPONENT}")
        stringParam("OSBS_BUILD_TARGET", "${OSBS_BUILD_TARGET}")
        stringParam("CEKIT_BUILD_OPTIONS", "${CEKIT_BUILD_OPTIONS}")
        stringParam("KERBEROS_PRINCIPAL", "${KERBEROS_PRINCIPAL}")
        stringParam("OSBS_BUILD_USER", "${OSBS_BUILD_USER}")
        stringParam("KERBEROS_KEYTAB", "${KERBEROS_KEYTAB}")
        stringParam("KERBEROS_CRED", "${KERBEROS_CRED}")
        stringParam("IMAGE_REPO", "${IMAGE_REPO}")
        stringParam("IMAGE_BRANCH", "${IMAGE_BRANCH}")
        stringParam("IMAGE_SUBDIR", "${IMAGE_SUBDIR}")
        stringParam("GIT_USER", "${GIT_USER}")
        stringParam("GIT_EMAIL", "${GIT_EMAIL}")
        stringParam("CEKIT_CACHE_LOCAL", "${CEKIT_CACHE_LOCAL}")
        stringParam("VERBOSE", "${VERBOSE}")
    }
    
  definition {
    cps {

        script('''
  
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
  
                          """ Parameters available for the build. """
                          def REQUIRED_BUILD_PARAMETERS = [
                                  "PROD_VERSION": [PROD_VERSION, '-v'],
                                  "PROD_COMPONENT": [PROD_COMPONENT, '-c'],
                                  "OSBS_BUILD_TARGET": [OSBS_BUILD_TARGET, '-t'],
                          ]
                          
                          def OPTIONAL_BUILD_PARAMETERS = [
                                  "KERBEROS_PASSWORD": [password, '-s'],
                                  "KERBEROS_PRINCIPAL": [user, '-p'],
                                  "KERBEROS_KEYTAB": [keytab, '-k'],
                                  "OSBS_BUILD_USER": [OSBS_BUILD_USER, '-i'],
                                  "BUILD_DATE": [BUILD_DATE, '-b'],
                                  "GIT_USER": [GIT_USER, '-u'],
                                  "GIT_EMAIL": [GIT_EMAIL, '-e'],
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
                          String DOWNLOAD_SCRIPT = "/opt/rhba/download.sh"
                          def download_command = "$DOWNLOAD_SCRIPT"  
                          
                          String BUILD_SCRIPT = "build-osbs.sh"
                          def build_command = "$BUILD_SCRIPT"
  
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
                              sh "source ~/virtenvs/cekit/bin/activate && $build_command"
                          }
                          
                          // post processing 
                      }
  
                      node("osbs-builder"){
  
                      stage("building ${PROD_COMPONENT}") {
  
                          ws {
                              // Add the working directory to the current path so any scripts dropped there are on the path
                              withEnv(["PATH+W=$WORKSPACE"]) {  
                                  sh 'rm -rf ${WORKSPACE}/{*,.*} || true'
                                  if (env.KERBEROS_CRED) {
                                  withCredentials([usernamePassword(credentialsId: env.KERBEROS_CRED, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                                          mainProcess(USERNAME, PASSWORD, "")
                                      }
                                  } else if (env.KERBEROS_KEYTAB && env.KERBEROS_PRINCIPAL) {
                                      withCredentials([file(credentialsId: env.KERBEROS_KEYTAB, variable: 'FILE')]) {
                                          mainProcess(env.KERBEROS_PRINCIPAL, "", FILE)
                                      }
                                  } else {
                                      error "Either KERBEROS_PRINCIPAL and KERBEROS_KEYTAB must be specified, or user/password with KERBEROS_CRED"
                                  }
                              }
                            }
                          }
                      }
  
        '''.stripIndent())
        sandbox()  
    }
    }
  }
}