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

import org.kie.jenkins.jobdsl.Constants
import javaposse.jobdsl.dsl.jobs.MatrixJob

class KieServerMatrix {

    /**
     * Adds common configuration to the job.
     *
     */
    static void addDeployConfiguration(MatrixJob matrixJob,
                                       String kieVersion,
                                       String jdkVersion,
                                       String mvnTool,
                                       String downloadUrl,
                                       String nexusUrl,
                                       String settingsXml){

        //Add configuration
        matrixJob.with {
            def kieServerTest = '''#!/bin/bash -e
                                        \n echo "KIE version $kieVersion"
                                        \n echo "Nexus URL:  $nexusUrl"
                                        \n # wget the tar.gz sources
                                        \n wget -q $nexusUrl/org/drools/droolsjbpm-integration/$kieVersion/droolsjbpm-integration-$kieVersion-project-sources.tar.gz -O sources.tar.gz
                                        \n tar xzf sources.tar.gz
                                        \n rm sources.tar.gz
                                        \n mv droolsjbpm-integration-$kieVersion/* .
                                        \n rm -rf droolsjbpm-integration-$kieVersion'''.stripMargin()
            description("""This job:
                            | KIE Server tests for jbpm
                            | IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! 
                            | The changes will get lost next time the job is generated.""".stripMargin())
            parameters {
                stringParam("kieVersion", "${kieVersion}", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>. Will be supplied by the parent job. ******************************************************** <br> ")
                stringParam("nexusUrl","${nexusUrl}","please edit the URL of artifacts on Nexus")
                stringParam("settingsXml","${settingsXml}","settings.xml for this job depending of daily builds or release")
            }

            label('kie-rhel8 && !built-in')

            axes {
                labelExpression("label-exp","kie-rhel8 && !built-in")
                jdk("${jdkVersion}")
                text("container", "wildfly", "eap7", "tomcat9")
            }

            childCustomWorkspace(Constants.MATRIX_SHORT_CHILD_WORKSPACE)

            logRotator {
                numToKeep(5)
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
            }

            publishers {
                archiveJunit("**/target/*-reports/TEST-*.xml")
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
                shell(kieServerTest)
                maven{
                    mavenInstallation("${mvnTool}")
                    goals("-B -e -fae -nsu clean verify -P\$container -Pjenkins-pr-builder")
                    rootPOM("kie-server-parent/kie-server-tests/pom.xml")
                    properties("kie.server.testing.kjars.build.settings.xml":"\$SETTINGS_XML_FILE")
                    properties("maven.test.failure.ignore": true)
                    properties("deployment.timeout.millis":"240000")
                    properties("container.startstop.timeout.millis":"240000")
                    properties("eap7.download.url":downloadUrl)
                    mavenOpts("-Xms1024m -Xmx1536m")
                    providedSettings("${settingsXml}")
                }
            }
        }
    }
}

