/**
* Stage artifacts produced by CPaaS into host rcm-guest.app.eng.bos.redhat.com.
*/
def scriptTemplate = this.getClass().getResource("job-scripts/prod_rhba_stage_cpaas_artifacts.jenkinsfile").text
def parsedScript = scriptTemplate.replaceAll(/<%=\s*(\w+)\s*%>/) { config[it[1]] ?: '' }

def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/stage-cpaas-artifacts") {
    description("This job stages artifacts produced by CPaaS  into host rcm-guest.app.eng.bos.redhat.com. \n" +
            "The staging is performed directly into the host through SSH and it copies artifacts from RHBA milestone folder to the proper product milestone staging folders.")

    parameters {
        stringParam('PRODUCT_NAME', 'RHPAM', 'Product name')
        stringParam('VERSION', '', ' The release candidate version, i.e. 7.12.1.CR1')
        stringParam('RCM_HOST', "\${env.RCM_HOST}", 'rcm host')
        stringParam('STAGING_BASE_PATH', "\${env.RCM_GUEST_FOLDER}", 'Staging base path inside the host')
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