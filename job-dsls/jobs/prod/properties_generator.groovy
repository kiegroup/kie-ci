/**
 * Generate properties files based on a template and uploads to a host.
 * Generated files:
 *
 * FILE_ID             the jenkins' file ID
 * FILE_NAME           the file name to store the file in the host
 * FOLDER_PATH         the folder to store the file in the host
 * BINDING             the array as string for binding properties. Use something like groovy.json.JsonOutput.toJson( myArray ) to transform the array
 * RCM_GUEST_FOLDER    the host folder
 * RCM_HOST            the host to upload the files
*/
import org.kie.jenkins.jobdsl.Constants

def propGen ="""
import groovy.json.JsonSlurperClassic

node('${Constants.LABEL_KIE_RHEL}') {
    sh 'env\'
    println "Folder '\${RCM_GUEST_FOLDER}/\${FOLDER_PATH}'"

    def binding = new JsonSlurperClassic().parseText(BINDING)
    println "Parsed binding: \${binding}"

    publishFile("\${FILE_ID}", "\${FILE_NAME}", binding, "\${RCM_GUEST_FOLDER}/\${FOLDER_PATH}")
}

def replaceTemplate(String fileId, Map<String, String> binding) {
    println "Replace Template \${fileId}"
    def content = ""
    configFileProvider([configFile(fileId: fileId, variable: 'PROPERTIES_FILE')]) {
        content = readFile "\${env.PROPERTIES_FILE}"
        for (def bind : binding) {
            content = content.replace("\\\${" + bind.getKey() + "}", bind.getValue().toString())
        }
    }
    return content
}

def publishFile(String fileId, String fileName, Map<String, String> binding, String folder) {
    println "Publishing [\${fileId}], name [\${fileName}] into folder [\${folder}] ..."
    def content = replaceTemplate(fileId, binding)
    println content
    writeFile file: "\${fileName}", text: content
    sshagent(credentials: ['rcm-publish-server']) {
        sh "ssh 'rhba@\${RCM_HOST}' 'mkdir -p \${folder}'"
        sh "scp -o StrictHostKeyChecking=no \${fileName} rhba@\${RCM_HOST}:\${folder}"
    }
}
"""
// create needed folder(s) for where the jobs are created
def folderPath = "Tools"
folder(folderPath)

pipelineJob("${folderPath}/properties-generator") {
    description("Generate properties files")

    parameters {
        stringParam("FILE_ID", "", "the jenkins' file ID")
        stringParam("FILE_NAME", "", "the file name to store the file in the host")
        stringParam("FOLDER_PATH", "", "the folder to store the file in the host")
        stringParam("BINDING", "[]", "The array as string for binding properties. Use something like groovy.json.JsonOutput.toJson( myArray ) to transform the array")
        stringParam("RCM_GUEST_FOLDER", "/mnt/rcm-guest/staging")
        stringParam("RCM_HOST", "rcm-guest.app.eng.bos.redhat.com")
    }

    definition {
        cps {
            script(propGen)
            sandbox()
        }
    }

}
