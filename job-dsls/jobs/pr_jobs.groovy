/**
 * Creates pullrequest (PR) jobs for appformer (formerly known as uberfire) and kiegroup GitHub org. units.
 */
import org.kie.jenkins.jobdsl.Constants

def final DEFAULTS = [
        ghOrgUnit              : Constants.GITHUB_ORG_UNIT,
        branch                 : Constants.BRANCH,
        timeoutMins            : 90,
        ghAuthTokenId          : "kie-ci2-token",
        label                  : "kie-rhel7 && kie-mem8g",
        upstreamMvnArgs        : "-B -e -T1C -s \$SETTINGS_XML_FILE -Dkie.maven.settings.custom=\$SETTINGS_XML_FILE -DskipTests -Dgwt.compiler.skip=true -Dgwt.skipCompilation=true -Denforcer.skip=true -Dcheckstyle.skip=true -Dspotbugs.skip=true -Drevapi.skip=true clean install",
        mvnGoals               : "-B -e -nsu -fae -Pwildfly clean install",
        mvnProps               : [
                "full"                     : "true",
                "container"                : "wildfly",
                "container.profile"        : "wildfly",
                "integration-tests"        : "true",
                "maven.test.failure.ignore": "true"],
        ircNotificationChannels: [],
        artifactsToArchive     : [
                "**/target/*.log",
                "**/target/testStatusListener*"
        ],
        excludedArtifacts      : [
                "**/target/checkstyle.log"
        ]
]

// override default config for specific repos (if needed)
def final REPO_CONFIGS = [
        "lienzo-core"               : [
                timeoutMins: 30,
                label: "kie-rhel7 && kie-mem4g"
        ],
        "lienzo-tests"              : [
                timeoutMins: 30,
                label: "kie-rhel7 && kie-mem4g"
        ],
        "droolsjbpm-build-bootstrap": [
                timeoutMins: 30,
                label      : "kie-rhel7 && kie-mem4g"
        ],
        "kie-soup"                  : [
                label: "kie-rhel7 && kie-mem4g"
        ],
        "appformer"                 : [
                label    : "kie-rhel7 && kie-mem16g"
        ],
        "droolsjbpm-knowledge"      : [
                label: "kie-rhel7 && kie-mem4g"
        ],
        "drools"                    : [],
        "optaplanner"               : [
                mvnGoals: "-B -e -nsu -fae clean install",
                mvnProps: [
                    "full"                     : "true",
                    "integration-tests"        : "true",
                    "maven.test.failure.ignore": "true"
                ]
        ],
        "optaweb-employee-rostering" : [
                artifactsToArchive: DEFAULTS["artifactsToArchive"] + [
                        "**/cypress/screenshots/**",
                        "**/cypress/videos/**"
                ]
        ],
        "optaweb-vehicle-routing" : [
                artifactsToArchive: DEFAULTS["artifactsToArchive"] + [
                        "**/cypress/screenshots/**",
                        "**/cypress/videos/**"
                ]
        ],
        "jbpm"                      : [
                timeoutMins: 120
        ],
        "kie-jpmml-integration"     : [
                label: "kie-rhel7 && kie-mem4g"
        ],
        "droolsjbpm-integration"    : [
                timeoutMins: 180,
                label    : "kie-rhel7 && kie-mem24g", // once https://github.com/kiegroup/kie-jenkins-scripts/pull/652 is reverted it will switch back to 16GB
                artifactsToArchive     : DEFAULTS["artifactsToArchive"] + [
                        "**/target/kie-server-*ee7.war",
                        "**/target/kie-server-*webc.war",
                        "**/gclog" // this is a temporary file used to do some analysis: Once https://github.com/kiegroup/kie-jenkins-scripts/pull/652 is reverted this will disappear
                ]
        ],
        "openshift-drools-hacep"    : [],
        "droolsjbpm-tools"          : [],
        "kie-uberfire-extensions"   : [
                label: "kie-rhel7 && kie-mem4g"
        ],
        "kie-wb-playground"         : [
                label: "kie-rhel7 && kie-mem4g"
        ],
        "kie-wb-common"             : [
                timeoutMins: 120,
                label: "kie-rhel7 && kie-mem16g && gui-testing",
                mvnProps          : DEFAULTS["mvnProps"] + [
                        "webdriver.firefox.bin"    : "/opt/tools/firefox-60esr/firefox-bin"
                ],
        ],
        "drools-wb"                 : [
                label: "kie-rhel7 && kie-mem16g"
        ],
        "optaplanner-wb"            : [],
        "jbpm-designer"             : [
                label: "kie-rhel7 && kie-mem16g"
        ],
        "jbpm-work-items"           : [
                label      : "kie-linux && kie-mem4g",
                timeoutMins: 30,
        ],
        "jbpm-wb"                   : [
                label: "kie-rhel7 && kie-mem16g",
                artifactsToArchive     : DEFAULTS["artifactsToArchive"] + [
                        "**/target/jbpm-wb-case-mgmt-showcase*.war",
                        "**/target/jbpm-wb-showcase.war"
                ]
        ],
        "kie-wb-distributions"      : [
                label             : "kie-linux && kie-mem24g && gui-testing",
                timeoutMins       : 120,
                mvnGoals          : DEFAULTS["mvnGoals"] + " -Pbusiness-central",
                mvnProps          : DEFAULTS["mvnProps"] + [
                        "gwt.compiler.localWorkers": 1,
                        "webdriver.firefox.bin"    : "/opt/tools/firefox-60esr/firefox-bin",
                        "gwt.memory.settings"      : "-Xmx10g"
                ],
                artifactsToArchive: DEFAULTS["artifactsToArchive"] + [
                        "**/target/screenshots/**",
                        "**/target/business-central*wildfly*.war",
                        "**/target/business-central*eap*.war",
                        "**/target/jbpm-server*dist*.zip"
                ]
        ]

]

//creation of script for log compression
def errorSh='''#!/bin/bash -e
cd $WORKSPACE
touch trace.sh
chmod 755 trace.sh
echo "wget  --no-check-certificate  ${BUILD_URL}consoleText" >> trace.sh
echo "tail -n 1000 consoleText >> error.log" >> trace.sh
echo "gzip error.log" >> trace.sh'''

def final SONARCLOUD_ENABLED_REPOSITORIES = ["optaplanner", "drools", "appformer", "jbpm", "drools-wb", "kie-soup", "droolsjbpm-integration", "kie-wb-common", "openshift-drools-hacep"]

for (repoConfig in REPO_CONFIGS) {
    Closure<Object> get = { String key -> repoConfig.value[key] ?: DEFAULTS[key] }

    String repo = repoConfig.key
    String repoBranch = get("branch")
    String ghOrgUnit = get("ghOrgUnit")
    String ghAuthTokenId = get("ghAuthTokenId")

    // Creation of folders where jobs are stored
    folder(Constants.PULL_REQUEST_FOLDER)


    // jobs for master branch don't use the branch in the name
    String jobName = (repoBranch == "master") ? Constants.PULL_REQUEST_FOLDER + "/$repo-pullrequests" : Constants.PULL_REQUEST_FOLDER + "/$repo-pullrequests-$repoBranch"
    job(jobName) {

        description("""Created automatically by Jenkins job DSL plugin. Do not edit manually! The changes will be lost next time the job is generated.
                    |
                    |Every configuration change needs to be done directly in the DSL files. See the below listed 'Seed job' for more info.
                    |""".stripMargin())

        logRotator {
            daysToKeep(7)
        }

        parameters {
            stringParam("sha1")
        }

        scm {
            git {
                remote {
                    github("${ghOrgUnit}/${repo}")
                    branch("\${sha1}")
                    name("origin")
                    refspec("+refs/pull/*:refs/remotes/origin/pr/*")
                }
                extensions {
                    cloneOptions {
                        reference("/home/jenkins/git-repos/${repo}.git")
                    }
                    relativeTargetDirectory("${repo}")
                }
            }
        }
        concurrentBuild()

        properties {
            ownership {
                primaryOwnerId("mbiarnes")
                coOwnerIds("mbiarnes")
            }
        }

        jdk("kie-jdk1.8")

        label(get("label"))

        // creates script for building error.log.gz
        steps {
            shell(errorSh)
        }

        triggers {
            githubPullRequest {
                orgWhitelist(["appformer", "kiegroup"])
                allowMembersOfWhitelistedOrgsAsAdmin()
                cron("H/7 * * * *")
                whiteListTargetBranches([repoBranch])
                extensions {
                    commitStatus {
                        context('Linux')
                        addTestResults(true)
                    }
                    if (repo == "kie-docs") {
                        buildStatus {
                            completedStatus("SUCCESS",
                                    """|Build successful! See generated HTML docs:
                                       |
                                       |\$BUILD_URL/artifact/docs/drools-docs/target/generated-docs/html_single/index.html
                                       |\$BUILD_URL/artifact/docs/jbpm-docs/target/generated-docs/html_single/index.html
                                       |\$BUILD_URL/artifact/docs/optaplanner-wb-es-docs//target/generated-docs/html_single/index.html
                                       |""".stripMargin())
                        }
                    }
                }
            }
        }

        wrappers {
            if (repo == "kie-wb-distributions" || repo == "kie-wb-common") {
                xvnc {
                    useXauthority(false)
                }
            }
            timeout {
                elastic(250, 3, get("timeoutMins"))
            }
            timestamps()
            colorizeOutput()

            configFiles {
                mavenSettings("settings-local-maven-repo-nexus"){
                    variable("SETTINGS_XML_FILE")
                    targetLocation("jenkins-settings.xml")
                }
            }
            
            if (repo in SONARCLOUD_ENABLED_REPOSITORIES) {
                credentialsBinding { // Injects SONARCLOUD_TOKEN credentials into an environment variable.
                    string("SONARCLOUD_TOKEN", "SONARCLOUD_TOKEN")
                }
            }
            preBuildCleanup()
        }

        steps {
            if (repo != "jbpm-work-items") {
                configure { project ->
                    project / 'builders' << 'org.kie.jenkinsci.plugins.kieprbuildshelper.UpstreamReposBuilder' {
                        mavenBuildConfig {
                            mavenHome("/opt/tools/apache-maven-${Constants.UPSTREAM_BUILD_MAVEN_VERSION}")
                            delegate.mavenOpts("-Xmx4g")
                            mavenArgs(get("upstreamMvnArgs"))
                        }
                    }
                }
            }

            def mavenGoals =
                repo in SONARCLOUD_ENABLED_REPOSITORIES ? "-Prun-code-coverage ${get('mvnGoals')}" : get("mvnGoals")


            // this is a temporary solution, once https://github.com/kiegroup/kie-jenkins-scripts/pull/652 was reverted this will disappear

            if  (repo == "droolsjbpm-integration") {
                maven {
                    mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")
                    mavenOpts("-Xms1g -Xmx4g -XX:+CMSClassUnloadingEnabled -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gclog")
                    goals(mavenGoals)
                    properties(get("mvnProps"))
                    providedSettings("settings-local-maven-repo-nexus")
                    rootPOM("${repo}")
                }

            } else {
                maven {
                        mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")
                        mavenOpts("-Xms1g -Xmx4g -XX:+CMSClassUnloadingEnabled")
                        goals(mavenGoals)
                        properties(get("mvnProps"))
                        providedSettings("settings-local-maven-repo-nexus")
                        rootPOM("${repo}")
                    }
             }



            if (repo in SONARCLOUD_ENABLED_REPOSITORIES) { // additional maven build step to report results to SonarCloud
                maven {
                    mavenInstallation("kie-maven-${Constants.MAVEN_VERSION}")
                    mavenOpts("-Xms1g -Xmx4g -XX:+CMSClassUnloadingEnabled")
                    goals("-B -e -nsu -fae generate-resources -Psonarcloud-analysis")
                    providedSettings("settings-local-maven-repo-nexus")
                    rootPOM("${repo}")
                }
            }
        }

        publishers {

            archiveJunit('**/target/*-reports/TEST-*.xml') {
                allowEmptyResults()
            }
            findbugs("**/spotbugsXml.xml")

            checkstyle("**/checkstyle-result.xml")
            def artifactsToArchive = get("artifactsToArchive")
            def excludedArtifacts = get("excludedArtifacts")
            if (artifactsToArchive) {
                archiveArtifacts {
                    allowEmpty(true)
                    for (artifactPattern in artifactsToArchive) {
                        pattern(artifactPattern)
                    }
                    onlyIfSuccessful(false)
                    if (excludedArtifacts) {
                        for (excludePattern in excludedArtifacts) {
                            exclude(excludePattern)
                        }
                    }
                }
            }

            // adds POST BUILD scripts
            configure { project ->
                project / 'publishers' << 'org.jenkinsci.plugins.postbuildscript.PostBuildScript' {
                    'config' {
                        'scriptFiles' {
                            'org.jenkinsci.plugins.postbuildscript.model.ScriptFile '{
                                'results' {
                                    'string'('SUCCESS')
                                    'string'('FAILURE')
                                    'string'('UNSTABLE')
                                }
                                'filePath'('trace.sh')
                                'scriptType'('GENERIC')
                            }
                        }
                        'groovyScripts'()
                        'buildSteps'()
                        'executeOn'('BOTH')
                        'markBuildUnstable'(false)
                        'sandboxed'(true)
                    }
                }
            }

            // Adds authentication token id for github.
            configure { node ->
                node / 'triggers' / 'org.jenkinsci.plugins.ghprb.GhprbTrigger' <<
                        'gitHubAuthId'(ghAuthTokenId)

            }

            extendedEmail{
                recipientList('$ghprbActualCommitAuthorEmail')
                defaultSubject('$DEFAULT_SUBJECT')
                defaultContent('$DEFAULT_CONTENT')
                contentType('default')
                triggers {
                    failure {
                        subject('Pull request #$ghprbPullId of $ghprbGhRepository: $ghprbPullTitle failed')
                        content('Pull request #$ghprbPullId of $ghprbGhRepository: $ghprbPullTitle  FAILED\n' +
                                'Build log: ${BUILD_URL}consoleText\n' +
                                'Failed tests (${TEST_COUNTS,var="fail"}): ${BUILD_URL}testReport\n' +
                                '(IMPORTANT: For visiting the links you need to have access to Red Hat VPN. In case you don\'t have access to RedHat VPN please download and decompress attached file.)')
                        attachmentPatterns('error.log.gz')
                        sendTo {
                            recipientList()
                        }
                    }
                    unstable {
                        subject('Pull request #$ghprbPullId of $ghprbGhRepository: $ghprbPullTitle was unstable')
                        content('Pull request #$ghprbPullId of $ghprbGhRepository: $ghprbPullTitle was UNSTABLE\n' +
                                'Build log: ${BUILD_URL}consoleText\n' +
                                'Failed tests (${TEST_COUNTS,var="fail"}): ${BUILD_URL}testReport\n' +
                                '***********************************************************************************************************************************************************\n' +
                                '${FAILED_TESTS}')
                        sendTo {
                            recipientList()
                        }
                    }
                    fixed {
                        subject('Pull request #$ghprbPullId of $ghprbGhRepository: $ghprbPullTitle is fixed and was SUCCESSFUL')
                        content('')
                        sendTo {
                            recipientList()
                        }
                    }
                }
            }

            wsCleanup()
        }
    }
}
