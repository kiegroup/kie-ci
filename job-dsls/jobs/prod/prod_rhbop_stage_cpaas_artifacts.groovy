/**
* Stage RHBOP artifacts produced by CPaaS into host rcm-guest.app.eng.bos.redhat.com.
*/
def scriptTemplate = this.getClass().getResource("job-scripts/prod_rhbop_stage_cpaas_artifacts.jenkinsfile").text
def parsedScript = scriptTemplate.replaceAll(/<%=\s*(\w+)\s*%>/) { config[it[1]] ?: '' }

def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/rhbop-stage-cpaas-artifacts") {
    description("This job adjusts RHBOP artifacts produced by CPaaS into host rcm-guest.app.eng.bos.redhat.com. \n" +
            "The staging is performed directly into the host through SSH and it adjust artifacts for the given RHBOP milestone release.")

    parameters {
        stringParam('PRODUCT_NAME', 'rhbop', 'Product name')
        stringParam('VERSION', '', ' The release candidate version, i.e. 8.29.0.CR1')
        stringParam('RCM_HOST', "\${RCM_HOST}", 'rcm host')
        stringParam('STAGING_BASE_PATH', "\${RCM_GUEST_FOLDER}", 'Staging base path inside the host')
    }

    logRotator {
        numToKeep(32)
    }

    definition {
        cps {
            script(parsedScript)
            sandbox()
        }
    }

}