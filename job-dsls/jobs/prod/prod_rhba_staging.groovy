/**
 * Artifact staging from PNC/Indy to the host rcm-host.
 */
String commands = this.getClass().getResource("job-scripts/prod_rhba_staging.jenkinsfile").text

def folderPath = "PROD"
folder(folderPath)

pipelineJob("${folderPath}/rhba-staging") {
    description("Artifact staging from PNC/Indy to the host rcm-host. \n" +
            "It retrieves from the specified PNC_API_URL the last artifact builds for the MILESTONE and stores them into the host STAGING_BASE_PATH directory.\n" +
            "The staged artifacts are restricted to the ones specified in the variable projects.\n" +
            "The staging is performed directly in the host with a remote command through SSH.\n" +
            "The downloads are verified with the md5 checksum provided by PNC.")

    logRotator {
        numToKeep(5)
    }

    parameters {
        stringParam("PNC_API_URL", "\${ORCH_PSI_URL}/pnc-rest/v2", "PNC Rest API endpoint. See: \${DOCS_ENGINEERING_URL}/display/JP/User%27s+guide")
        stringParam("STAGING_BASE_PATH", "\${RCM_GUEST_FOLDER}", "Staging path where artifacts will be deployed into the host: rcm-host")
        stringParam("MILESTONE", "", "Release version including milestone, e.g. 7.10.0.CR2")
        stringParam("RCM_HOST", "\${RCM_HOST}", "rcm-host hostname")
    }

    definition {
        cps {
            script(commands)
            sandbox()
        }
    }

}