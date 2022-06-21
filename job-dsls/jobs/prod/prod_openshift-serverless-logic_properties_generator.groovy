/**
 * Generate properties files for openshift serverless logic builds.
 */
import org.kie.jenkins.jobdsl.Constants

def props ="""
import groovy.json.JsonOutput

node('${Constants.LABEL_KIE_RHEL}') {
    sh 'env\'
    def REPO_URL_FOLDER_VERSION = 'main'.equals(BRANCH_NAME) ? 'main' : (PRODUCT_VERSION =~ /\\d+\\.\\d+/)[0]
    println "Folder [\${REPO_URL_FOLDER_VERSION}] based on BRANCH_NAME [\${BRANCH_NAME}] and PRODUCT_VERSION [\${PRODUCT_VERSION}]"
    def REPO_URL_FINAL = REPO_URL.replace("-main-", "-\${REPO_URL_FOLDER_VERSION}-")
    println "REPO_URL_FINAL [\${REPO_URL_FINAL}]"

    def binding = JsonOutput.toJson([
            "REPO_URL"                          : REPO_URL_FINAL,
            "DELIVERABLE_REPO_URL"              : DELIVERABLE_REPO_URL,
            "PRODUCT_VERSION"                   : PRODUCT_VERSION,
            "VERSION_LONG"                      : PRODUCT_VERSION_LONG,
            "PRODUCT_MILESTONE"                 : PRODUCT_MILESTONE,
            "TIME_STAMP"                        : TIME_STAMP,
            "GIT_INFORMATION_HASHES"            : GIT_INFORMATION_HASHES 
    ])
    if(Boolean.valueOf(IS_RELEASE)) {
        println "//TODO"    
    } else {
        def folder = "openshift-serverless-logic/OSL-\${PRODUCT_VERSION}.NIGHTLY"

        build job: env.PROPERTIES_GENERATOR_PATH, parameters: [
            [\$class: 'StringParameterValue', name: 'FILE_ID', value: 'openshift-serverless-logic-nightly-properties-template'],
            [\$class: 'StringParameterValue', name: 'FILE_NAME', value: "osl-\${TIME_STAMP}.properties"],
            [\$class: 'StringParameterValue', name: 'FOLDER_PATH', value: folder],
            [\$class: 'StringParameterValue', name: 'BINDING', value: binding]
        ]
    }
}
"""

// create needed folder(s) for where the jobs are created
def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/openshift-serverless-logic-properties-generator") {
    description("Generate properties files for OSL builds")

    parameters {
        booleanParam("IS_RELEASE", true, "it defines if the properties file is for prod or not")
        stringParam("BRANCH_NAME", "main", "the branch the nightly was triggered for")
        stringParam("REPO_URL", "http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/osl-main-nightly", "Prod possibility is http://rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging")
        stringParam("DELIVERABLE_REPO_URL", "http://download.devel.redhat.com/devel/candidates","")
        stringParam("PRODUCT_VERSION", "1.0","")
        stringParam("PRODUCT_VERSION_LONG", "1.0.redhat-00001", "This is just for prod files")
        stringParam("PRODUCT_MILESTONE", "CR1", "This is just for prod files")
        stringParam("TIME_STAMP", "", "This is just for prod files")
    }

    definition {
        cps {
            script(props)
            sandbox()
        }
    }

}
