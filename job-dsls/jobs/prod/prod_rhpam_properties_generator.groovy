/**
 * Generate properties files for nightly and productized builds.
 *
 * Generated files for nightly:
 * - {rhpam}-{timestamp}.properties - properties file pointing to nightly binaries in the staging area
 *
 * Generated files for productized builds:
 * - {rhpam}-deliverable-list-staging.properties - properties file pointing binaries in the staging area
 * - {rhpam}-deliverable-list.properties - properties file pointing binaries in the candidates area
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
    ])
    if(Boolean.valueOf(IS_RELEASE)) {
        def folder = "rhpam/RHPAM-\${PRODUCT_VERSION}.\${PRODUCT_MILESTONE}"

        build job: env.PROPERTIES_GENERATOR_PATH, parameters: [
            [\$class: 'StringParameterValue', name: 'FILE_ID', value: '6ad7aff1-2d3d-4cdc-81de-b62dae1f39e9'],
            [\$class: 'StringParameterValue', name: 'FILE_NAME', value: 'rhpam-deliverable-list-staging.properties'],
            [\$class: 'StringParameterValue', name: 'FOLDER_PATH', value: folder],
            [\$class: 'StringParameterValue', name: 'BINDING', value: binding]
        ]
        build job: env.PROPERTIES_GENERATOR_PATH, parameters: [
            [\$class: 'StringParameterValue', name: 'FILE_ID', value: 'f5eb870f-53d8-426c-bcfa-04668965e3ef'],
            [\$class: 'StringParameterValue', name: 'FILE_NAME', value: 'rhpam-deliverable-list.properties'],
            [\$class: 'StringParameterValue', name: 'FOLDER_PATH', value: folder],
            [\$class: 'StringParameterValue', name: 'BINDING', value: binding]
        ]
    } else {
        def folder = "rhpam/RHPAM-\${PRODUCT_VERSION}.NIGHTLY"

        build job: env.PROPERTIES_GENERATOR_PATH, parameters: [
            [\$class: 'StringParameterValue', name: 'FILE_ID', value: 'aff8076d-3a5d-4e45-b41e-413ca9b34258'],
            [\$class: 'StringParameterValue', name: 'FILE_NAME', value: "rhpam-\${TIME_STAMP}.properties"],
            [\$class: 'StringParameterValue', name: 'FOLDER_PATH', value: folder],
            [\$class: 'StringParameterValue', name: 'BINDING', value: binding]
        ]
    }
}
"""
// create needed folder(s) for where the jobs are created
def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/rhpam-properties-generator") {
    description("Generate properties files for RHPAM nightly and productized builds")

    parameters {
        booleanParam("IS_RELEASE", true, "it defines if the properties file is for prod or not")
        stringParam("BRANCH_NAME", "main", "the branch the nightly was triggered for")
        stringParam("REPO_URL", "http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/rhba-main-nightly", "Prod possibility is http://download.devel.redhat.com/rcm-guest/staging/")
        stringParam("DELIVERABLE_REPO_URL", "http://download.devel.redhat.com/devel/candidates")
        stringParam("PRODUCT_VERSION", "7.10.0")
        stringParam("PRODUCT_VERSION_LONG", "7.10.0.redhat-00003", "This is just for prod files")
        stringParam("PRODUCT_MILESTONE", "CR2", "this is just for prod files")
        stringParam("TIME_STAMP", "", "This is just for non-prod files")
        stringParam("BOM_VERSION", "\${PRODUCT_VERSION_LONG}", "This is just for prod files")
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
