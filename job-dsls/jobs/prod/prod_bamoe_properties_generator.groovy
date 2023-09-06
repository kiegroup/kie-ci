/**
 * Generate properties files for nightly and productized builds.
 *
 * Generated files for nightly:
 * - {bamoe}-{timestamp}.properties - properties file pointing to nightly binaries in the staging area
 *
 * Generated files for productized builds:
 * - {bamoe-deliverable-list-staging.properties - properties file pointing binaries in the staging area
 * - {bamoe}-deliverable-list.properties - properties file pointing binaries in the candidates area
 */
import org.kie.jenkins.jobdsl.Constants

def propGen ="""
import groovy.json.JsonOutput

node('${Constants.LABEL_KIE_RHEL}') {

    sh 'env\'
    def REPO_URL_FOLDER_VERSION = 'main'.equals(BRANCH_NAME) ? 'main' : (PRODUCT_VERSION =~ /\\d+\\.\\d+/)[0]
    println "Folder [\${REPO_URL_FOLDER_VERSION}] based on BRANCH_NAME [\${BRANCH_NAME}] and PRODUCT_VERSION [\${PRODUCT_VERSION}]"
    def REPO_URL_FINAL = REPO_URL.replace("-main-", "-\${REPO_URL_FOLDER_VERSION}-")
    println "REPO_URL_FINAL [\${REPO_URL_FINAL}]"

    def binding = JsonOutput.toJson([
            "REPO_URL"                   : REPO_URL_FINAL,
            "DELIVERABLE_REPO_URL"       : DELIVERABLE_REPO_URL,
            "TAG_HASH"                   : TAG_HASH,
            "PRODUCT_VERSION"            : PRODUCT_VERSION,
            "PRODUCT_VERSION_LONG"       : PRODUCT_VERSION_LONG,
            "PRODUCT_MILESTONE"          : PRODUCT_MILESTONE,
            "TIME_STAMP"                 : TIME_STAMP,
            "BOM_VERSION"                : BOM_VERSION,
            "KIE_VERSION"                : KIE_VERSION,
            "MVEL_VERSION"               : MVEL_VERSION,
            "IZPACK_VERSION"             : IZPACK_VERSION,
            "INSTALLER_COMMONS_VERSION"  : INSTALLER_COMMONS_VERSION,
            "JAVAPARSER_VERSION"         : JAVAPARSER_VERSION
    ])
    if(Boolean.valueOf(IS_RELEASE)) {
        def folder = "bamoe/bamoe-\${PRODUCT_VERSION}.\${PRODUCT_MILESTONE}"

        build job: env.PROPERTIES_GENERATOR_PATH, parameters: [
            [\$class: 'StringParameterValue', name: 'FILE_ID', value: "\${RELEASE_STAGING_FILE_ID}"],
            [\$class: 'StringParameterValue', name: 'FILE_NAME', value: 'bamoe-deliverable-list-staging.properties'],
            [\$class: 'StringParameterValue', name: 'FOLDER_PATH', value: folder],
            [\$class: 'StringParameterValue', name: 'BINDING', value: binding]
        ]
        build job: env.PROPERTIES_GENERATOR_PATH, parameters: [
            [\$class: 'StringParameterValue', name: 'FILE_ID', value: "\${RELEASE_FILE_ID}"],
            [\$class: 'StringParameterValue', name: 'FILE_NAME', value: 'bamoe-deliverable-list.properties'],
            [\$class: 'StringParameterValue', name: 'FOLDER_PATH', value: folder],
            [\$class: 'StringParameterValue', name: 'BINDING', value: binding]
        ]
    } else {
        def folder = "bamoe/BAMOE-\${PRODUCT_VERSION}.NIGHTLY"

        build job: env.PROPERTIES_GENERATOR_PATH, parameters: [
            [\$class: 'StringParameterValue', name: 'FILE_ID', value: "\${NIGHTLY_FILE_ID}"],
            [\$class: 'StringParameterValue', name: 'FILE_NAME', value: "bamoe-\${TIME_STAMP}.properties"],
            [\$class: 'StringParameterValue', name: 'FOLDER_PATH', value: folder],
            [\$class: 'StringParameterValue', name: 'BINDING', value: binding]
        ]
    }
}
"""
// create needed folder(s) for where the jobs are created
def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/bamoe-properties-generator") {
    description("Generate properties files for BAMOE nightly and productized builds")

    parameters {
        booleanParam("IS_RELEASE", true, "it defines if the properties file is for prod or not")
        stringParam("BRANCH_NAME", "main", "the branch the nightly was triggered for")
        stringParam("TAG_HASH", "", "The hash of the nightly tag in productization repository. This is just for nightly")
        stringParam("REPO_URL", "\${BXMS_QE_NEXUS}/content/repositories/rhba-main-nightly", "Prod possibility is \${STAGING_SERVER_URL}")
        stringParam("DELIVERABLE_REPO_URL", "\${DOWNLOAD_CANDIDATES}/middleware")
        stringParam("PRODUCT_VERSION", "8.0.0")
        stringParam("PRODUCT_VERSION_LONG", "8.0.0.redhat-00001", "This is just for prod files")
        stringParam("PRODUCT_MILESTONE", "CR1", "This is just for prod files")
        stringParam("TIME_STAMP", "", "This is just for non-prod files")
        stringParam("BOM_VERSION", "\${PRODUCT_VERSION_LONG}", "This is just for prod files")
        stringParam("KIE_VERSION", "7.67.2.Final-redhat-00001", "This is just for prod files")
        stringParam("MVEL_VERSION", "2.4.15.Final-redhat-00001")
        stringParam("IZPACK_VERSION", "4.5.4.rhba-redhat-00017")
        stringParam("INSTALLER_COMMONS_VERSION", "2.4.0.rhba-redhat-00018")
        stringParam("JAVAPARSER_VERSION", "3.13.10", "This is just for prod files")
        stringParam("RCM_GUEST_FOLDER", "\${RCM_GUEST_FOLDER}")
        stringParam("RCM_HOST", "\${RCM_HOST}")
        stringParam("NIGHTLY_FILE_ID", "bamoe-nightly-properties-template")
        stringParam("RELEASE_STAGING_FILE_ID", "bamoe-prod-properties-template")
        stringParam("RELEASE_FILE_ID", "bamoe-prod-deliverable-properties-template")
    }

    logRotator {
        numToKeep(10)
    }

    definition {
        cps {
            script(propGen)
            sandbox()
        }
    }

}
