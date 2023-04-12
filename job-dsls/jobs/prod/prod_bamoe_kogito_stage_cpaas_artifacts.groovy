/**
* Stage BAMOE Kogito artifacts produced by CPaaS into host rcm-host.
*/
def scriptTemplate = this.getClass().getResource("job-scripts/prod_bamoe_kogito_stage_cpaas_artifacts.jenkinsfile").text
def parsedScript = scriptTemplate.replaceAll(/<%=\s*(\w+)\s*%>/) { config[it[1]] ?: '' }

def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/bamoe-kogito-stage-cpaas-artifacts") {
    description("This job stages BAMOE Kogito artifacts produced by CPaaS into host rcm-host. \n" +
            "The staging is performed directly into the host through SSH and it adjust artifacts for the given BAMOE Kogito milestone release.")

    parameters {
        stringParam('PRODUCT_NAME', 'bamoe-kogito', 'Product name')
        stringParam('VERSION', '', ' The release candidate version, i.e. 1.13.2.CR1')
        stringParam('BAMOE_VERSION', '', ' The BAMOE release candidate version related to the BAMOE Kogito release, i.e. 8.0.0.CR1')
        stringParam('RCM_HOST', "\${RCM_HOST}", 'rcm host')
    }

    logRotator {
        numToKeep(20)
    }

    definition {
        cps {
            script(parsedScript)
            sandbox()
        }
    }

}