/**
 * Generate properties files for kogito builds.
 */

def kogitoProps ='''
node('kie-rhel7&&!master') {
    sh 'env\'
    def REPO_URL_FOLDER_VERSION = 'main'.equals(BRANCH_NAME) ? 'main' : (KOGITO_PRODUCT_VERSION =~ /\\d+\\.\\d+/)[0]
    println "Folder [${REPO_URL_FOLDER_VERSION}] based on BRANCH_NAME [${BRANCH_NAME}] and KOGITO_PRODUCT_VERSION [${KOGITO_PRODUCT_VERSION}]"
    def REPO_URL_FINAL = REPO_URL.replace("-main-", "-${REPO_URL_FOLDER_VERSION}-")
    println "REPO_URL_FINAL [${REPO_URL_FINAL}]"

    def binding = [
            "REPO_URL"                          : REPO_URL_FINAL,
            "DELIVERABLE_REPO_URL"              : DELIVERABLE_REPO_URL,
            "KOGITO_PRODUCT_VERSION"            : KOGITO_PRODUCT_VERSION,
            "KOGITO_PRODUCT_VERSION_LONG"       : KOGITO_PRODUCT_VERSION_LONG,
            "OPTAPLANNER_PRODUCT_VERSION"       : OPTAPLANNER_PRODUCT_VERSION,
            "OPTAPLANNER_PRODUCT_VERSION_LONG"  : OPTAPLANNER_PRODUCT_VERSION_LONG,
            "PRODUCT_MILESTONE"                 : PRODUCT_MILESTONE,
            "TIME_STAMP"                        : TIME_STAMP,
            "GIT_INFORMATION_HASHES"            : GIT_INFORMATION_HASHES 
    ]
    if(Boolean.valueOf(IS_PROD)) {
        println "//TODO"    
    } else {
        publishFile("kogito-nightly-properties-template", "kogito-${TIME_STAMP}.properties", binding, "${RCM_GUEST_FOLDER}/kogito/KOGITO-${KOGITO_PRODUCT_VERSION}.NIGHTLY")
    }
}

def replaceTemplate(String fileId, Map<String, String> binding) {
    println "Replace Template ${fileId}"
    def content = ""
    configFileProvider([configFile(fileId: fileId, variable: 'PROPERTIES_FILE')]) {
        content = readFile "${env.PROPERTIES_FILE}"
        for (def bind : binding) {
            content = content.replace("\\${" + bind.getKey() + "}", bind.getValue().toString())
        }
    }
    return content
}

def publishFile(String fileId, String fileName, Map<String, String> binding, String folder) {
    println "Publishing [${fileId}], name [${fileName}] into folder [${folder}] ..."
    def content = replaceTemplate(fileId, binding)
    println content
    writeFile file: "${fileName}", text: content
    sshagent(credentials: ['rcm-publish-server']) {
        sh "ssh 'rhba@${RCM_HOST}' 'mkdir -p ${folder}'"
        sh "scp -o StrictHostKeyChecking=no ${fileName} rhba@${RCM_HOST}:${folder}"
    }
}
'''

// create needed folder(s) for where the jobs are created
folder("PROD")
def folderPath = "PROD"

pipelineJob("${folderPath}/kogito-properties-generator") {
    description("Generate properties files for kogito builds")

    parameters {
        booleanParam("IS_PROD", true, "it defines if the properties file is for prod or not")
        stringParam("BRANCH_NAME", "main", "the branch the nightly was triggered for")
        stringParam("REPO_URL", "http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/kogito-main-nightly", "Prod possibility is http://rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging")
        stringParam("DELIVERABLE_REPO_URL", "http://download.devel.redhat.com/devel/candidates","")
        stringParam("KOGITO_PRODUCT_VERSION", "1.0","")
        stringParam("KOGITO_PRODUCT_VERSION_LONG", "1.0.redhat-00001", "This is just for prod files")
        stringParam("OPTAPLANNER_PRODUCT_VERSION", "8.3.0","")
        stringParam("OPTAPLANNER_PRODUCT_VERSION_LONG", "8.3.0.redhat-00001", "")
        stringParam("PRODUCT_MILESTONE", "CR1", "This is just for prod files")
        stringParam("TIME_STAMP", "", "This is just for prod files")
        stringParam("RCM_GUEST_FOLDER", "/mnt/rcm-guest/staging")
        stringParam("RCM_HOST", "rcm-guest.app.eng.bos.redhat.com")
    }

    definition {
        cps {
            script(kogitoProps)
            sandbox()
        }
    }

}
