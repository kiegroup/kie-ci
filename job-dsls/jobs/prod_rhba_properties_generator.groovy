/**
 * Generate properties files for nightly and productized builds.
 *
 * Generated files for nightly:
 * - {rhpam|rhdm}-{timestamp}.properties - properties file pointing to nightly binaries in the staging area
 *
 * Generated files for productized builds:
 * - {rhpam|rhdm}-deliverable-list-staging.properties - properties file pointing binaries in the staging area
 * - {rhpam|rhdm}-deliverable-list.properties - properties file pointing binaries in the candidates area
 */
String commands = this.getClass().getResource("job-scripts/prod_rhba_properties_generator.jenkinsfile").text

pipelineJob("rhba-properties-generator") {
    description("Generate properties files for nightly and productized builds")

    parameters {
        booleanParam("IS_PROD", true, "it defines if the properties file is for prod or not")
        stringParam("BRANCH_NAME", "master", "the branch the nightly was triggered for")
        stringParam("REPO_URL", "http://bxms-qe.rhev-ci-vms.eng.rdu2.redhat.com:8081/nexus/content/repositories/rhba-master-nightly", "Prod possibility is http://rcm-guest.app.eng.bos.redhat.com/rcm-guest/staging")
        stringParam("DELIVERABLE_REPO_URL", "http://download.devel.redhat.com/devel/candidates")
        stringParam("PRODUCT_VERSION", "7.7.0")
        stringParam("PRODUCT_VERSION_LONG", "7.7.0.redhat-00004", "This is just for prod files")
        stringParam("PRODUCT_MILESTONE", "CR1", "this is just for prod files")
        stringParam("TIME_STAMP", "", "This is just for non-prod files")
        stringParam("KIE_VERSION", "7.30.0.Final-redhat-00003", "This is just for prod files")
        stringParam("ERRAI_VERSION")
        stringParam("MVEL_VERSION")
        stringParam("IZPACK_VERSION")
        stringParam("INSTALLER_COMMONS_VERSION")
        stringParam("JAVAPARSER_VERSION", "", "This is just for prod files")
        stringParam("RCM_GUEST_FOLDER", "/mnt/rcm-guest/staging")
        stringParam("RCM_HOST", "rcm-guest.app.eng.bos.redhat.com")
    }

    definition {
        cps {
            script(commands)
            sandbox()
        }
    }

}