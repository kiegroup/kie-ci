/**
 * Artifact staging from PNC/Indy to the host rcm-guest.app.eng.bos.redhat.com.
 */

def stageScr='''
import groovy.json.JsonSlurper
import java.net.URLEncoder

node {
    def projects = [
        [
            name: "kiegroup/rhba",
            artifacts: [
                [product: "rhpam", group: "org.kie.rhba", name: "rhpam", package: "zip", classifier: "add-ons"],
                [product: "rhpam", group: "org.kie.rhba", name: "rhpam", package: "zip", classifier: "business-central-eap7-deployable"],
                [product: "rhpam", group: "org.kie.rhba", name: "rhpam", package: "jar", classifier: "business-central-standalone"],
                [product: "rhpam", group: "org.kie.rhba", name: "rhpam", package: "zip", classifier: "kie-server-ee8"],
                [product: "rhpam", group: "org.kie.rhba", name: "rhpam", package: "zip", classifier: "kie-server-ee7"],
                [product: "rhpam", group: "org.kie.rhba", name: "rhpam", package: "zip", classifier: "monitoring-ee7"],
                [product: "rhpam", group: "org.kie.rhba", name: "rhpam", package: "zip", classifier: "reference-implementation"],
                [product: "rhdm", group: "org.kie.rhba", name: "rhdm", package: "zip", classifier: "add-ons"],
                [product: "rhdm", group: "org.kie.rhba", name: "rhdm", package: "zip", classifier: "decision-central-eap7-deployable"],
                [product: "rhdm", group: "org.kie.rhba", name: "rhdm", package: "jar", classifier: "decision-central-standalone"],
                [product: "rhdm", group: "org.kie.rhba", name: "rhdm", package: "zip", classifier: "kie-server-ee8"],
                [product: "rhdm", group: "org.kie.rhba", name: "rhdm", package: "zip", classifier: "kie-server-ee7"],
                [product: "rhdm", group: "org.kie.rhba", name: "rhdm", package: "zip", classifier: "reference-implementation"]
            ]
        ], [
            name: "rhba-installers",
            artifacts: [
                [product: "rhpam", group: "org.jboss.installer", name: "rhpam-installer", package: "jar"],
                [product: "rhdm", group: "org.jboss.installer", name: "rhdm-installer", package: "jar"]
            ]
        ], [
            name: "jboss-integration/bxms-patch-tools",
            artifacts: [
                [product: "rhpam", group: "org.jboss.brms-bpmsuite.patching", name: "rhpam", package: "zip", classifier: "update"],
                [product: "rhdm", group: "org.jboss.brms-bpmsuite.patching", name: "rhdm", package: "zip", classifier: "update"]
            ]
        ]
    ]

    stage("Staging") {
        for (project in projects) {
            println "[INFO] staging artifacts for project: ${project.name}"
            def projectId = getProjectId(project.name)
            def lastVersion = getLastProjectVersion(projectId, MILESTONE)

            for (artifact in project.artifacts) {
                def identifier = identifier(artifact, lastVersion)
                println "[INFO] staging artifact: ${identifier}"

                def artifactMetadata = getArtifactMetadata(identifier)
                def artifactPath = stagingPath(artifact)
                def artifactDirectoryPath = artifactPath - ~/\\/[^\\/]+$/

                println "[INFO] (re)creating staging target path in: ${artifactPath}"
                createStagingPath(artifactDirectoryPath)
                
                println "[INFO] staging artifact from: ${artifactMetadata.publicUrl}"
                stageArtifact(artifactMetadata.publicUrl, artifactPath)
                
                println "[INFO] verifying artifact checksum with: ${artifactMetadata.md5}"
                verifyStaged(artifactPath, artifactMetadata.md5)
            }
        }
    }
}

/*
 * PNC API helper
 */
def queryPNC(endpoint, params) {
    def queryURL = "${PNC_API_URL}/${endpoint}?" + encodeParams(params)
    def response = sh(script: "curl -s -X GET --header \\"Accept: application/json\\" \\"${queryURL}\\"", returnStdout: true)
    return new JsonSlurper().parseText(response)
}

def encodeParams(params) {
    return params.collect({ it.getKey() + "=" + encode(it.getValue()) }).join("&")
}

def encode(text) {
    return URLEncoder.encode(text, "UTF-8")
}

/*
 * PNC API calls
 */
def getProjectId(projectName) {
    def endpoint = "projects"
    def params = [q: "name==${projectName}"]
    def projects = queryPNC(endpoint, params)
    def project = projects.content.first()
    return project.id
}

def getLastProjectVersion(projectId, milestone) {
    def endpoint = "projects/${projectId}/builds"
    def params = [q: "productMilestone.version==${milestone};status==SUCCESS;temporaryBuild==false"]
    def projectBuilds = queryPNC(endpoint, params)
    def lastprojectBuild = projectBuilds.content.max({ it.scmTag })
    return lastprojectBuild.scmTag
}

def getArtifactMetadata(identifier) {
    def endpoint = "artifacts"
    def params = [q: "identifier==${identifier}"]
    def artifacts = queryPNC(endpoint, params)
    def artifact = artifacts.content.first()
    return [
        publicUrl: artifact.publicUrl,
        md5: artifact.md5
    ]
}

/*
 * Auxiliary methods
 */
def identifier(artifact, version) {
    def identifierParts = [artifact.group, artifact.name, artifact.package, version, artifact.classifier]
    return identifierParts.findAll({ it != null }).join(":")
}

/*
 * Staging management
 */
def stagingPath(artifact) {
    def productStagingCode = [artifact.product.toUpperCase(), MILESTONE].join("-")
    def productStagingPath = "${STAGING_BASE_PATH}/${artifact.product}/${productStagingCode}"
    def version = MILESTONE - ~/\\.[^\\.]+$/
    def artifactName = [artifact.name, version, artifact.classifier].findAll({ it != null }).join("-")
    return "${productStagingPath}/${artifactName}.${artifact.package}"
}

def createStagingPath(path) {
    remoteExec("mkdir -m 775 -p ${path}")
    remoteExec("chgrp jboss-prod ${path}")
}

def stageArtifact(url, path) {
    remoteExec("curl -s ${url} -o ${path}")
    remoteExec("chmod 664 ${path}")
    remoteExec("chgrp jboss-prod ${path}")
}

def verifyStaged(path, md5) {
    def output = remoteExec("md5sum ${path}")
    def (outputMd5, outputPath) = output.split(/\\s+/)
    if (path != outputPath) {
        error("Couldn't compute the checksum for ${path}: ${output}")
    } else if (md5 != outputMd5) {
        error("${path} checksum doesn't match. Expected ${md5}, got ${outputMd5}")
    }
}

def remoteExec(command) {
    sshagent(['rcm-publish-server']) {
        return sh(script: "ssh -o StrictHostKeyChecking=no rhba@rcm-guest.app.eng.bos.redhat.com '${command}'", returnStdout: true)
    }
}
'''

// create needed folder(s) for where the jobs are created
folder("PROD")
def folderPath = "PROD"

pipelineJob("${folderPath}/rhba-staging") {
    description("Artifact staging from PNC/Indy to the host rcm-guest.app.eng.bos.redhat.com. \n" +
            "It retrieves from the specified PNC_API_URL the last artifact builds for the MILESTONE and stores them into the host STAGING_BASE_PATH directory.\n" +
            "The staged artifacts are restricted to the ones specified in the variable projects.\n" +
            "The staging is performed directly in the host with a remote command through SSH.\n" +
            "The downloads are verified with the md5 checksum provided by PNC.")

    logRotator {
        numToKeep(5)
    }

    parameters {
        stringParam("PNC_API_URL", "http://orch.psi.redhat.com/pnc-rest/v2", "PNC Rest API endpoint. See: https://docs.engineering.redhat.com/display/JP/User%27s+guide")
        stringParam("STAGING_BASE_PATH", "/mnt/rcm-guest/staging", "Staging path where artifacts will be deployed into the host: rcm-guest.app.eng.bos.redhat.com")
        stringParam("MILESTONE", "", "Release version including milestone, e.g. 7.10.0.CR2")
    }

    definition {
        cps {
            script(stageScr)
            sandbox()
        }
    }

}