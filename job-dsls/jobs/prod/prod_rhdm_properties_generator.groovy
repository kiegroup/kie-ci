/**
 * Generate properties files for nightly and productized builds.
 *
 * Generated files for nightly:
 * - {rhdm}-{timestamp}.properties - properties file pointing to nightly binaries in the staging area
 *
 * Generated files for productized builds:
 * - {rhdm}-deliverable-list-staging.properties - properties file pointing binaries in the staging area
 * - {rhdm}-deliverable-list.properties - properties file pointing binaries in the candidates area
 */

def propGen ='''
import groovy.json.JsonOutput

node('kie-rhel7&&!master') {

    sh 'env\'
    def REPO_URL_FOLDER_VERSION = 'main'.equals(BRANCH_NAME) ? 'main' : (PRODUCT_VERSION =~ /\\d+\\.\\d+/)[0]
    println "Folder [${REPO_URL_FOLDER_VERSION}] based on BRANCH_NAME [${BRANCH_NAME}] and PRODUCT_VERSION [${PRODUCT_VERSION}]"
    def REPO_URL_FINAL = REPO_URL.replace("-main-", "-${REPO_URL_FOLDER_VERSION}-")
    println "REPO_URL_FINAL [${REPO_URL_FINAL}]"

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
        def folder = "rhdm/RHDM-${PRODUCT_VERSION}.${PRODUCT_MILESTONE}"

        build job: env.PROPERTIES_GENERATOR_PATH, parameters: [
            [$class: 'StringParameterValue', name: 'FILE_ID', value: '8862cf74-d316-4eea-a99e-f74d90be6931'],
            [$class: 'StringParameterValue', name: 'FILE_NAME', value: 'rhdm-deliverable-list-staging.properties'],
            [$class: 'StringParameterValue', name: 'FOLDER_PATH', value: folder],
            [$class: 'StringParameterValue', name: 'BINDING', value: binding]
        ]
        build job: env.PROPERTIES_GENERATOR_PATH, parameters: [
            [$class: 'StringParameterValue', name: 'FILE_ID', value: '598bedb7-780f-4f46-994f-e6314d55d8b9'],
            [$class: 'StringParameterValue', name: 'FILE_NAME', value: 'rhdm-deliverable-list.properties'],
            [$class: 'StringParameterValue', name: 'FOLDER_PATH', value: folder],
            [$class: 'StringParameterValue', name: 'BINDING', value: binding]
        ]
    } else {
        def folder = "rhdm/RHDM-${PRODUCT_VERSION}.NIGHTLY"

        build job: env.PROPERTIES_GENERATOR_PATH, parameters: [
            [$class: 'StringParameterValue', name: 'FILE_ID', value: '8196c1f9-71ee-4bb1-8244-6b7711715c66'],
            [$class: 'StringParameterValue', name: 'FILE_NAME', value: "rhdm-${TIME_STAMP}.properties"],
            [$class: 'StringParameterValue', name: 'FOLDER_PATH', value: folder],
            [$class: 'StringParameterValue', name: 'BINDING', value: binding]
        ]
    }
}
'''
// create needed folder(s) for where the jobs are created
folder("PROD")
def folderPath = "PROD"

pipelineJob("${folderPath}/rhdm-properties-generator") {
    description("Generate properties files for RHDM nightly and productized builds")

    parameters {
        booleanParam("IS_RELEASE", true, "it defines if the properties file is for release or not")
        stringParam("BRANCH_NAME", "main", "the branch the nightly was triggered for")
        stringParam("REPO_URL", "http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/rhba-main-nightly", "Prod possibility is http://download.devel.redhat.com/rcm-guest/staging/")
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
    }

    definition {
        cps {
            script(propGen)
            sandbox()
        }
    }

}
