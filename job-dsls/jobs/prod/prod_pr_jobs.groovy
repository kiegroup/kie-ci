def configs = [:]

configs['izpack'] = [
        mavenParams: '',
        pmeParams: '''
         -DversionOverride=${IZPACK_VERSION}
        '''
]

configs['installer-commons'] = [
        mavenParams: '',
        pmeParams: '''
         -DversionOverride=${INSTALLER_COMMONS_VERSION}
         -DizpackVersion=${IZPACK_VERSION}
         -DgroovyScripts=file:///${CONFIG_CHECKOUT_PATH}/rhba/nightly/InstallerCommonsAlignment.groovy 
        '''
]

configs['rhba-installers'] = [
        mavenParams: '',
        pmeParams: '''
         -DversionOverride=${RHPAM_VERSION}
         -DizpackVersion=${IZPACK_VERSION}
         -DinstallerCommonsVersion=${INSTALLER_COMMONS_VERSION}
         -DkieVersion=${KIE_VERSION}
         -DgroovyScripts=file:///${CONFIG_CHECKOUT_PATH}/rhba/nightly/InstallerAlignment.groovy,file:///${CONFIG_CHECKOUT_PATH}/rhba/nightly/InstallerCommonsAlignment.groovy,file:///${CONFIG_CHECKOUT_PATH}/rhba/nightly/ForceComRedhatBaVersion.groovy,file:///${CONFIG_CHECKOUT_PATH}/rhba/nightly/ForceComRedhatBaVersionLast.groovy
        '''
]

configs['bxms-patch-tools'] = [
        mavenParams: '-Prhpam,rhdm,integration-tests',
        pmeParams: '''
         -DversionOverride=${RHPAM_VERSION} 
         -DkieVersion=${KIE_VERSION} 
         -DupdatableVersionRange=7.9.0.redhat-00002,7.9.1.redhat-00003,7.10.0.redhat-00004,7.10.1.redhat-00001
         -DgroovyScripts=file:///${CONFIG_CHECKOUT_PATH}/rhba/nightly/BxmsPatchToolsPropertiesAlignment.groovy 
        '''
]

def folderPath = "PROD/main/pullrequest"
folder("PROD")
folder("PROD/main")
folder("PROD/main/pullrequest")

def commands = this.getClass().getResource("job-scripts/prod_pr_jobs.jenkinsfile").text

configs.each { repository, config ->

    def branchName = repository == 'izpack' ? 'bxms-7.0' : 'main'
    def parsedCommands = commands.replaceAll(/\$\{(\w+)\}/) { config.containsKey(it[1]) ? config[it[1]] : it[0] }

    pipelineJob("${folderPath}/${repository}-${branchName}.pr") {

        description("Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.\n" +
                    "Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info")

        logRotator {
            numToKeep(10)
        }

        properties {
            githubProjectUrl("https://github.com/jboss-integration/${repository}")
        }

        definition {
            cps {
                script(parsedCommands)
                sandbox()
            }
        }

        properties {
            pipelineTriggers {
                triggers {
                    ghprbTrigger {
                        onlyTriggerPhrase(false)
                        gitHubAuthId("kie-ci-token")
                        adminlist("")
                        orgslist("jboss-integration")
                        whitelist("")
                        cron("")
                        triggerPhrase(".*[j|J]enkins,?.*(retest|test).*")
                        allowMembersOfWhitelistedOrgsAsAdmin(true)
                        whiteListTargetBranches {
                            ghprbBranch {
                                branch("${branchName}")
                            }
                        }
                        useGitHubHooks(true)
                        permitAll(false)
                        autoCloseFailedPullRequests(false)
                        displayBuildErrorsOnDownstreamBuilds(false)
                        blackListCommitAuthor("")
                        commentFilePath("")
                        skipBuildPhrase("")
                        msgSuccess("Success")
                        msgFailure("Failure")
                        commitStatusContext("")
                        buildDescTemplate("")
                        blackListLabels("")
                        whiteListLabels("")
                        extensions {
                            ghprbSimpleStatus {
                                commitStatusContext("Linux - Pull Request")
                                addTestResults(true)
                                showMatrixStatus(false)
                                statusUrl("")
                                triggeredStatus("")
                                startedStatus("")
                            }
                            ghprbCancelBuildsOnUpdate {
                                overrideGlobal(true)
                            }
                        }
                        includedRegions("")
                        excludedRegions("")
                    }
                }
            }
        }
    }
}
