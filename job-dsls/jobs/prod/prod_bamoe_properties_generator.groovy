/**
 * Generate properties files for productized builds.
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
    def binding = JsonOutput.toJson([
            "REPO_URL"                   : REPO_URL,
            "DELIVERABLE_REPO_URL"       : DELIVERABLE_REPO_URL,
            "PRODUCT_VERSION"            : PRODUCT_VERSION,
            "PRODUCT_VERSION_LONG"       : PRODUCT_VERSION_LONG,
            "PRODUCT_MILESTONE"          : PRODUCT_MILESTONE,
            "BOM_VERSION"                : BOM_VERSION,
            "KIE_VERSION"                : KIE_VERSION,
            "MVEL_VERSION"               : MVEL_VERSION,
            "IZPACK_VERSION"             : IZPACK_VERSION,
            "INSTALLER_COMMONS_VERSION"  : INSTALLER_COMMONS_VERSION,
            "JAVAPARSER_VERSION"         : JAVAPARSER_VERSION
    ])
    def folder = "bamoe/bamoe-\${PRODUCT_VERSION}.\${PRODUCT_MILESTONE}"

    build job: env.PROPERTIES_GENERATOR_PATH, parameters: [
        [\$class: 'StringParameterValue', name: 'FILE_ID', value: "\${RELEASE_STAGING_FILE_ID}"],
        [\$class: 'StringParameterValue', name: 'FILE_NAME', value: 'rhpam-deliverable-list-staging.properties'],
        [\$class: 'StringParameterValue', name: 'FOLDER_PATH', value: folder],
        [\$class: 'StringParameterValue', name: 'BINDING', value: binding]
    ]
    build job: env.PROPERTIES_GENERATOR_PATH, parameters: [
        [\$class: 'StringParameterValue', name: 'FILE_ID', value: "\${RELEASE_FILE_ID}"],
        [\$class: 'StringParameterValue', name: 'FILE_NAME', value: 'rhpam-deliverable-list.properties'],
        [\$class: 'StringParameterValue', name: 'FOLDER_PATH', value: folder],
        [\$class: 'StringParameterValue', name: 'BINDING', value: binding]
    ]
}
"""
// create needed folder(s) for where the jobs are created
def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/bamoe-properties-generator") {
    description("Generate properties files for BAMOE productized builds")

    parameters {
        stringParam("REPO_URL", "\${STAGING_SERVER_URL}")
        stringParam("DELIVERABLE_REPO_URL", "\${DOWNLOAD_CANDIDATES}/middleware")
        stringParam("PRODUCT_VERSION", "8.0.0")
        stringParam("PRODUCT_VERSION_LONG", "8.0.0.redhat-00001")
        stringParam("PRODUCT_MILESTONE", "CR1")
        stringParam("BOM_VERSION", "\${PRODUCT_VERSION_LONG}")
        stringParam("KIE_VERSION", "7.67.2.Final-redhat-00001")
        stringParam("MVEL_VERSION", "2.4.12.Final-redhat-00001")
        stringParam("IZPACK_VERSION", "4.5.4.rhba-redhat-00017")
        stringParam("INSTALLER_COMMONS_VERSION", "2.4.0.rhba-redhat-00018")
        stringParam("JAVAPARSER_VERSION", "3.13.10")
        stringParam("RCM_GUEST_FOLDER", "\${RCM_GUEST_FOLDER}")
        stringParam("RCM_HOST", "\${RCM_HOST}")
        stringParam("RELEASE_STAGING_FILE_ID", "bamoe-prod-properties-template")
        stringParam("RELEASE_FILE_ID", "bamoe-prod-deliverable-properties-template")
    }

    definition {
        cps {
            script(propGen)
            sandbox()
        }
    }

}
