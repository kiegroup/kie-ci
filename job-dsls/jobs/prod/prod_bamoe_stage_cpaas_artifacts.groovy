/**
* Stage artifacts produced by CPaaS into host rcm-guest.app.eng.bos.redhat.com.
*/
def scriptTemplate = this.getClass().getResource("job-scripts/prod_bamoe_stage_cpaas_artifacts.jenkinsfile").text
def parsedScript = scriptTemplate.replaceAll(/<%=\s*(\w+)\s*%>/) { config[it[1]] ?: '' }

def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/bamoe-stage-cpaas-artifacts") {
    description("This job stages artifacts produced by CPaaS into host rcm-guest.app.eng.bos.redhat.com. \n" +
            "The staging is performed directly into the host through SSH and it adjust artifacts for the given BAMOE milestone release.")

    parameters {
        stringParam('PRODUCT_NAME', 'bamoe', 'Product name')
        stringParam('VERSION', '', ' The release candidate version, i.e. 8.0.1.CR1')
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