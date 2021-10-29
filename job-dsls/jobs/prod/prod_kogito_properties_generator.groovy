/**
 * Generate properties files for kogito builds.
 */

def kogitoProps ='''
import groovy.json.JsonOutput

node('kie-rhel7&&!master') {
    sh 'env\'
    def REPO_URL_FOLDER_VERSION = 'main'.equals(BRANCH_NAME) ? 'main' : (KOGITO_PRODUCT_VERSION =~ /\\d+\\.\\d+/)[0]
    println "Folder [${REPO_URL_FOLDER_VERSION}] based on BRANCH_NAME [${BRANCH_NAME}] and KOGITO_PRODUCT_VERSION [${KOGITO_PRODUCT_VERSION}]"
    def REPO_URL_FINAL = REPO_URL.replace("-main-", "-${REPO_URL_FOLDER_VERSION}-")
    println "REPO_URL_FINAL [${REPO_URL_FINAL}]"

    def binding = JsonOutput.toJson([
            "REPO_URL"                          : REPO_URL_FINAL,
            "DELIVERABLE_REPO_URL"              : DELIVERABLE_REPO_URL,
            "KOGITO_PRODUCT_VERSION"            : KOGITO_PRODUCT_VERSION,
            "KOGITO_PRODUCT_VERSION_LONG"       : KOGITO_PRODUCT_VERSION_LONG,
            "OPTAPLANNER_PRODUCT_VERSION"       : OPTAPLANNER_PRODUCT_VERSION,
            "OPTAPLANNER_PRODUCT_VERSION_LONG"  : OPTAPLANNER_PRODUCT_VERSION_LONG,
            "PRODUCT_MILESTONE"                 : PRODUCT_MILESTONE,
            "TIME_STAMP"                        : TIME_STAMP,
            "GIT_INFORMATION_HASHES"            : GIT_INFORMATION_HASHES 
    ])
    if(Boolean.valueOf(IS_RELEASE)) {
        println "//TODO"    
    } else {
        def folder = "kogito/KOGITO-${KOGITO_PRODUCT_VERSION}.NIGHTLY"

        build job: env.PROPERTIES_GENERATOR_PATH, parameters: [
            [$class: 'StringParameterValue', name: 'FILE_ID', value: 'kogito-nightly-properties-template'],
            [$class: 'StringParameterValue', name: 'FILE_NAME', value: "kogito-${TIME_STAMP}.properties"],
            [$class: 'StringParameterValue', name: 'FOLDER_PATH', value: folder],
            [$class: 'StringParameterValue', name: 'BINDING', value: binding]
        ]
    }
}
'''

// create needed folder(s) for where the jobs are created
folder("PROD")
def folderPath = "PROD"

pipelineJob("${folderPath}/kogito-properties-generator") {
    description("Generate properties files for kogito builds")

    parameters {
        booleanParam("IS_RELEASE", true, "it defines if the properties file is for prod or not")
        stringParam("BRANCH_NAME", "main", "the branch the nightly was triggered for")
        stringParam("REPO_URL", "http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/kogito-main-nightly", "Prod possibility is http://rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging")
        stringParam("DELIVERABLE_REPO_URL", "http://download.devel.redhat.com/devel/candidates","")
        stringParam("KOGITO_PRODUCT_VERSION", "1.0","")
        stringParam("KOGITO_PRODUCT_VERSION_LONG", "1.0.redhat-00001", "This is just for prod files")
        stringParam("OPTAPLANNER_PRODUCT_VERSION", "8.3.0","")
        stringParam("OPTAPLANNER_PRODUCT_VERSION_LONG", "8.3.0.redhat-00001", "")
        stringParam("PRODUCT_MILESTONE", "CR1", "This is just for prod files")
        stringParam("TIME_STAMP", "", "This is just for prod files")
    }

    definition {
        cps {
            script(kogitoProps)
            sandbox()
        }
    }

}
