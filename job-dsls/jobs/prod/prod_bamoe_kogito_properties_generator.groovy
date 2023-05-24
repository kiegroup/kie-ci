/**
 * Generate properties files for productized builds.
 *
 * Generated files for productized builds:
 * - kogito-deliverable-list-staging.properties - properties file pointing binaries in the staging area
 * - kogito-deliverable-list.properties - properties file pointing binaries in the candidates area
 */
import org.kie.jenkins.jobdsl.Constants

def kogitoProps ="""
import groovy.json.JsonOutput

node('${Constants.LABEL_KIE_RHEL}') {
    sh 'env\'
    def binding = JsonOutput.toJson([
            "REPO_URL"                          : REPO_URL,
            "DELIVERABLE_REPO_URL"              : DELIVERABLE_REPO_URL,
            "BAMOE_PRODUCT_VERSION"             : BAMOE_PRODUCT_VERSION,
            "PRODUCT_MILESTONE"                 : PRODUCT_MILESTONE,
            "KOGITO_VERSION"                    : KOGITO_VERSION,
            "QUARKUS_BOM_VERSION"               : QUARKUS_BOM_VERSION,
            "PLATFORM_QUARKUS_BOM_VERSION"      : PLATFORM_QUARKUS_BOM_VERSION
    ])

    def folder = "bamoe/bamoe-\${BAMOE_PRODUCT_VERSION}.\${PRODUCT_MILESTONE}"

    build job: env.PROPERTIES_GENERATOR_PATH, parameters: [
        [\$class: 'StringParameterValue', name: 'FILE_ID', value: "\${RELEASE_STAGING_FILE_ID}"],
        [\$class: 'StringParameterValue', name: 'FILE_NAME', value: 'kogito-deliverable-list-staging.properties'],
        [\$class: 'StringParameterValue', name: 'FOLDER_PATH', value: folder],
        [\$class: 'StringParameterValue', name: 'BINDING', value: binding]
    ]
    build job: env.PROPERTIES_GENERATOR_PATH, parameters: [
        [\$class: 'StringParameterValue', name: 'FILE_ID', value: "\${RELEASE_FILE_ID}"],
        [\$class: 'StringParameterValue', name: 'FILE_NAME', value: 'kogito-deliverable-list.properties'],
        [\$class: 'StringParameterValue', name: 'FOLDER_PATH', value: folder],
        [\$class: 'StringParameterValue', name: 'BINDING', value: binding]
    ]
}
"""

// create needed folder(s) for where the jobs are created
def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/bamoe-kogito-properties-generator") {
    description("Generate properties files for BAMOE Kogito productized builds")

    parameters {
        stringParam("REPO_URL", "\${STAGING_SERVER_URL}")
        stringParam("DELIVERABLE_REPO_URL", "\${DOWNLOAD_CANDIDATES}/middleware")
        stringParam("BAMOE_PRODUCT_VERSION", "8.0.0")
        stringParam("PRODUCT_MILESTONE", "CR1")
        stringParam("KOGITO_VERSION", "1.13.2.redhat-00001")
        stringParam("QUARKUS_BOM_VERSION", "2.13.7.Final-redhat-00003")
        stringParam("PLATFORM_QUARKUS_BOM_VERSION", "2.13.7.SP2-redhat-00002")
        stringParam("RCM_GUEST_FOLDER", "\${RCM_GUEST_FOLDER}")
        stringParam("RCM_HOST", "\${RCM_HOST}")
        stringParam("RELEASE_STAGING_FILE_ID", "kogito-blue-prod-properties-template")
        stringParam("RELEASE_FILE_ID", "kogito-blue-prod-deliverable-properties-template")
    }

    logRotator {
        numToKeep(10)
    }

    definition {
        cps {
            script(kogitoProps)
            sandbox()
        }
    }

}
