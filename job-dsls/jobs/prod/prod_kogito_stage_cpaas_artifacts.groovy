/**
* Stage Kogito artifacts produced by CPaaS into host rcm-host.
*/
def scriptTemplate = this.getClass().getResource("job-scripts/prod_kogito_stage_cpaas_artifacts.jenkinsfile").text
def parsedScript = scriptTemplate.replaceAll(/<%=\s*(\w+)\s*%>/) { config[it[1]] ?: '' }

def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/kogito-stage-cpaas-artifacts") {
    description("This job stages Kogito artifacts produced by CPaaS into host rcm-host. \n" +
            "The staging is performed directly into the host through SSH and it adjust artifacts for the given Kogito milestone release.")

    parameters {
        stringParam('PRODUCT_NAME', 'kogito', 'Product name')
        stringParam('VERSION', '', ' The release candidate version, i.e. 1.13.1.CR1')
        stringParam('RHPAM_VERSION', '', ' The RHPAM version related to the Kogito release, i.e. 7.13.1')
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