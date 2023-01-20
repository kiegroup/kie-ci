/**
 * Generate properties files for nightly and productized builds.
 *
 * Generated files for nightly:
 * - kogito-{timestamp}.properties - properties file pointing to nightly binaries in the staging area
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
    def REPO_URL_FOLDER_VERSION = 'main'.equals(BRANCH_NAME) ? 'main' : (KOGITO_PRODUCT_VERSION =~ /\\d+\\.\\d+/)[0]
    println "Folder [\${REPO_URL_FOLDER_VERSION}] based on BRANCH_NAME [\${BRANCH_NAME}] and KOGITO_PRODUCT_VERSION [\${KOGITO_PRODUCT_VERSION}]"
    def REPO_URL_FINAL = REPO_URL.replace("-main-", "-\${REPO_URL_FOLDER_VERSION}-")
    println "REPO_URL_FINAL [\${REPO_URL_FINAL}]"

    def binding = JsonOutput.toJson([
            "REPO_URL"                          : REPO_URL_FINAL,
            "DELIVERABLE_REPO_URL"              : DELIVERABLE_REPO_URL,
            "KOGITO_PRODUCT_VERSION"            : KOGITO_PRODUCT_VERSION,
            "RHPAM_PRODUCT_VERSION"             : RHPAM_PRODUCT_VERSION,
            "PRODUCT_MILESTONE"                 : PRODUCT_MILESTONE,
            "TIME_STAMP"                        : TIME_STAMP,
            "KOGITO_VERSION"                    : KOGITO_VERSION,
            "OPTAPLANNER_PRODUCT_VERSION"       : OPTAPLANNER_PRODUCT_VERSION,
            "OPTAWEB_PRODUCT_VERSION"           : OPTAWEB_PRODUCT_VERSION,
            "PLATFORM_QUARKUS_BOM_VERSION"      : PLATFORM_QUARKUS_BOM_VERSION,
            "GIT_INFORMATION_HASHES"            : GIT_INFORMATION_HASHES
    ])
    if(Boolean.valueOf(IS_RELEASE)) {
        def folder = "kogito/kogito-\${KOGITO_PRODUCT_VERSION}.\${PRODUCT_MILESTONE}"

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

    } else {
        def folder = "kogito/KOGITO-\${KOGITO_PRODUCT_VERSION}.NIGHTLY"

        build job: env.PROPERTIES_GENERATOR_PATH, parameters: [
            [\$class: 'StringParameterValue', name: 'FILE_ID', value: "\${NIGHTLY_FILE_ID}"],
            [\$class: 'StringParameterValue', name: 'FILE_NAME', value: "kogito-\${TIME_STAMP}.properties"],
            [\$class: 'StringParameterValue', name: 'FOLDER_PATH', value: folder],
            [\$class: 'StringParameterValue', name: 'BINDING', value: binding]
        ]
    }
}
"""

// create needed folder(s) for where the jobs are created
def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/kogito-properties-generator") {
    description("Generate properties files for kogito builds")

    parameters {
        booleanParam("IS_RELEASE", true, "it defines if the properties file is for prod or not")
        stringParam("BRANCH_NAME", "main", "the branch the nightly was triggered for")
        stringParam("REPO_URL", "\${BXMS_QE_NEXUS}/content/repositories/kogito-main-nightly", "Prod possibility is \${STAGING_SERVER_URL}")
        stringParam("DELIVERABLE_REPO_URL", "\${DOWNLOAD_CANDIDATES}")
        stringParam("KOGITO_PRODUCT_VERSION", "1.13.0")
        stringParam("RHPAM_PRODUCT_VERSION", "7.13.0")
        stringParam("PRODUCT_MILESTONE", "CR1", "This is just for prod files")
        stringParam("TIME_STAMP", "", "This is just for non-prod files")
        stringParam("GIT_INFORMATION_HASHES", "", "This is just for non-prod files")
        stringParam("KOGITO_VERSION", "1.13.0.redhat-00001")
        stringParam("OPTAPLANNER_PRODUCT_VERSION", "8.13.0.Final-redhat-00001")
        stringParam("OPTAWEB_PRODUCT_VERSION", "8.13.0.Final-redhat-00001")
        stringParam("PLATFORM_QUARKUS_BOM_VERSION", "2.7.6.Final-redhat-00006")
        stringParam("RCM_GUEST_FOLDER", "\${RCM_GUEST_FOLDER}")
        stringParam("RCM_HOST", "\${RCM_HOST}")
        stringParam("NIGHTLY_FILE_ID", "kogito-nightly-properties-template")
        stringParam("RELEASE_STAGING_FILE_ID", "kogito-prod-properties-template")
        stringParam("RELEASE_FILE_ID", "kogito-prod-deliverable-properties-template")
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
