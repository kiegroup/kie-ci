/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.jenkins.jobdsl.templates.additionalTests

import javaposse.jobdsl.dsl.jobs.MatrixJob

class KieWbTestMatrix {

    /**
     * Adds common configuration to the job.
     *
     */
    static void addDeployConfiguration(MatrixJob matrixJob,
                                       String kieVersion,
                                       String jdkVersion,
                                       String mvnTool,
                                       String nexusUrl,
                                       String settingsXml){

        //Add configuration
        matrixJob.with {
            def kieWbTest = '''#!/bin/bash -e
                                   \n echo "KIE version $kieVersion"
                                   \n echo "Nexus URL:  $nexusUrl"
                                   \n echo "KIE version $kieVersion - kie-wb-distributions"
                                   \n wget -q $nexusUrl/org/kie/kie-wb-distributions/$kieVersion/kie-wb-distributions-$kieVersion-project-sources.tar.gz -O sources.tar.gz
                                   \n tar xzf sources.tar.gz
                                   \n rm sources.tar.gz
                                   \n mv kie-wb-distributions-$kieVersion/* .
                                   \n rm -rf kie-wb-distributions-$kieVersion
                                   \n echo "KIE version $kieVersion - kie-wb-common"
                                   \n wget -q $nexusUrl/org/kie/workbench/kie-wb-common/$kieVersion/kie-wb-common-$kieVersion-project-sources.tar.gz -O sources.tar.gz
                                   \n tar xzf sources.tar.gz
                                   \n rm sources.tar.gz
                                   \n mv kie-wb-common-$kieVersion/* .
                                   \n rm -rf kie-wb-common-$kieVersion'''.stripMargin()
            description("""This job:
                            | KIE Workbench tests for jbpm
                            | IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! 
                            | The changes will get lost next time the job is generated.""".stripMargin())
            parameters {
                stringParam("kieVersion", "${kieVersion}", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>. Will be supplied by the parent job. ******************************************************** <br> ")
                stringParam("nexusUrl","${nexusUrl}","please edit the URL of artifacts on Nexus")
                stringParam("settingsXml","${settingsXml}","settings.xml for this job depending of daily builds or release")
            }

            label('kie-rhel7&&!master')

            axes {
                labelExpression("label_exp", "kie-rhel7&&gui-testing")
                text("container", "wildfly")
                text("war", "business-central")
                jdk("${jdkVersion}")
                text("browser", "firefox")
            }

            logRotator {
                numToKeep(5)
            }

            properties {
                rebuild {
                    autoRebuild()
                }
            }

            throttleConcurrentBuilds {
                maxPerNode(1)
                maxTotal(5)
                throttleMatrixConfigurations()
            }

            wrappers {
                timeout {
                    absolute(180)
                }
                timestamps()
                colorizeOutput()
                preBuildCleanup()
                configFiles {
                    mavenSettings("${settingsXml}") {
                        variable("SETTINGS_XML_FILE")
                        targetLocation("jenkins-settings.xml")
                    }
                }
                xvnc {
                    useXauthority()
                }
            }

            publishers {
                archiveJunit("**/target/*-reports/TEST-*.xml")
                mailer('kie-jenkins-builds@redhat.com', false, false)
                wsCleanup()
            }

            configure { project ->
                project / 'buildWrappers' << 'org.jenkinsci.plugins.proccleaner.PreBuildCleanup' {
                    cleaner(class: 'org.jenkinsci.plugins.proccleaner.PsCleaner') {
                        killerType 'org.jenkinsci.plugins.proccleaner.PsAllKiller'
                        killer(class: 'org.jenkinsci.plugins.proccleaner.PsAllKiller')
                        username 'jenkins'
                    }
                }
            }

            steps {
                shell(kieWbTest)
                maven {
                    mavenInstallation("${mvnTool}")
                    goals("-nsu -B -e -fae clean verify -P\$container,\$war")
                    rootPOM("business-central-tests/pom.xml")
                    properties("maven.test.failure.ignore": true)
                    properties("deployment.timeout.millis": "240000")
                    properties("container.startstop.timeout.millis": "240000")
                    properties("webdriver.firefox.bin": "/opt/tools/firefox-60esr/firefox-bin")
                    mavenOpts("-Xms1024m -Xmx1536m")
                    providedSettings("${settingsXml}")
                }
            }
        }
    }
}

