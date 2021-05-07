/**
 * Generate properties files for nightly and productized builds.
 *
 * Generated files for nightly:
 * - {rhpam|rhdm}-{timestamp}.properties - properties file pointing to nightly binaries in the staging area
 *
 * Generated files for productized builds:
 * - {rhpam|rhdm}-deliverable-list-staging.properties - properties file pointing binaries in the staging area
 * - {rhpam|rhdm}-deliverable-list.properties - properties file pointing binaries in the candidates area
 */

def propGen ='''
node('kie-rhel7-priority&&!master') {
    sh 'env\'
    def REPO_URL_FOLDER_VERSION = 'master'.equals(BRANCH_NAME) ? 'master' : (PRODUCT_VERSION =~ /\\d+\\.\\d+/)[0]
    println "Folder [${REPO_URL_FOLDER_VERSION}] based on BRANCH_NAME [${BRANCH_NAME}] and PRODUCT_VERSION [${PRODUCT_VERSION}]"
    def REPO_URL_FINAL = REPO_URL.replace("-master-", "-${REPO_URL_FOLDER_VERSION}-")
    println "REPO_URL_FINAL [${REPO_URL_FINAL}]"

    def binding = [
            "REPO_URL"                   : REPO_URL_FINAL,
            "DELIVERABLE_REPO_URL"       : DELIVERABLE_REPO_URL,
            "PRODUCT_VERSION"            : PRODUCT_VERSION,
            "PRODUCT_VERSION_LONG"       : PRODUCT_VERSION_LONG,
            "PRODUCT_MILESTONE"          : PRODUCT_MILESTONE,
            "TIME_STAMP"                 : TIME_STAMP,
            "KIE_VERSION"                : KIE_VERSION,
            "ERRAI_VERSION"              : ERRAI_VERSION,
            "MVEL_VERSION"               : MVEL_VERSION,
            "IZPACK_VERSION"             : IZPACK_VERSION,
            "INSTALLER_COMMONS_VERSION"  : INSTALLER_COMMONS_VERSION,
            "JAVAPARSER_VERSION"         : JAVAPARSER_VERSION
    ]
    if(Boolean.valueOf(IS_PROD)) {
        // RHPAM
        publishFile("6ad7aff1-2d3d-4cdc-81de-b62dae1f39e9", "rhpam-deliverable-list-staging.properties", binding, "${RCM_GUEST_FOLDER}/rhpam/RHPAM-${PRODUCT_VERSION}.${PRODUCT_MILESTONE}")
        publishFile("f5eb870f-53d8-426c-bcfa-04668965e3ef", "rhpam-deliverable-list.properties", binding, "${RCM_GUEST_FOLDER}/rhpam/RHPAM-${PRODUCT_VERSION}.${PRODUCT_MILESTONE}")

        // RHDM
        publishFile("8862cf74-d316-4eea-a99e-f74d90be6931", "rhdm-deliverable-list-staging.properties", binding, "${RCM_GUEST_FOLDER}/rhdm/RHDM-${PRODUCT_VERSION}.${PRODUCT_MILESTONE}")
        publishFile("598bedb7-780f-4f46-994f-e6314d55d8b9", "rhdm-deliverable-list.properties", binding, "${RCM_GUEST_FOLDER}/rhdm/RHDM-${PRODUCT_VERSION}.${PRODUCT_MILESTONE}")
    } else {
        publishFile("aff8076d-3a5d-4e45-b41e-413ca9b34258", "rhpam-${TIME_STAMP}.properties", binding, "${RCM_GUEST_FOLDER}/rhpam/RHPAM-${PRODUCT_VERSION}.NIGHTLY")
        publishFile("8196c1f9-71ee-4bb1-8244-6b7711715c66", "rhdm-${TIME_STAMP}.properties", binding, "${RCM_GUEST_FOLDER}/rhdm/RHDM-${PRODUCT_VERSION}.NIGHTLY")
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

pipelineJob("${folderPath}/rhba-properties-generator") {
    description("Generate properties files for nightly and productized builds")

    parameters {
        booleanParam("IS_PROD", true, "it defines if the properties file is for prod or not")
        stringParam("BRANCH_NAME", "master", "the branch the nightly was triggered for")
        stringParam("REPO_URL", "http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/rhba-master-nightly", "Prod possibility is http://rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging")
        stringParam("DELIVERABLE_REPO_URL", "http://download.devel.redhat.com/devel/candidates")
        stringParam("PRODUCT_VERSION", "7.10.0")
        stringParam("PRODUCT_VERSION_LONG", "7.10.0.redhat-00003", "This is just for prod files")
        stringParam("PRODUCT_MILESTONE", "CR2", "this is just for prod files")
        stringParam("TIME_STAMP", "", "This is just for non-prod files")
        stringParam("KIE_VERSION", "7.48.0.Final-redhat-00003", "This is just for prod files")
        stringParam("ERRAI_VERSION")
        stringParam("MVEL_VERSION")
        stringParam("IZPACK_VERSION")
        stringParam("INSTALLER_COMMONS_VERSION")
        stringParam("JAVAPARSER_VERSION", "", "This is just for prod files")
        stringParam("RCM_GUEST_FOLDER", "/mnt/rcm-guest/staging")
        stringParam("RCM_HOST", "rcm-guest.app.eng.bos.redhat.com")
    }

    definition {
        cps {
            script(propGen)
            sandbox()
        }
    }

}