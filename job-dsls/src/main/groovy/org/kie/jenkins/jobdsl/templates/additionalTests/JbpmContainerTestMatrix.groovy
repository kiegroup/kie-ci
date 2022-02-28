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

class JbpmContainerTestMatrix {

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
        matrixJob.with{
            def jbpmContainerTest = '''#!/bin/bash -e
                                            \n echo "KIE version $kieVersion"
                                            \n echo "Nexus URL:  $nexusUrl"
                                            \n wget -q $nexusUrl/org/jbpm/jbpm/$kieVersion/jbpm-$kieVersion-project-sources.tar.gz -O sources.tar.gz
                                            \n tar xzf sources.tar.gz
                                            \n rm sources.tar.gz
                                            \n mv jbpm-$kieVersion/* .
                                            \n rm -rf jbpm-$kieVersion'''.stripMargin()
            description ("""This job:
                            | Container Matrix Test for jbpm
                            | IMPORTANT: Created automatically by Jenkins job DSL plugin. Do not edit manually! 
                            | The changes will get lost next time the job is generated.""".stripMargin())
            parameters {
                stringParam("kieVersion", "${kieVersion}", "please edit the version of the KIE release <br> i.e. typically <b> major.minor.micro.<extension> </b>7.1.0.Beta1 for <b> community </b>. Will be supplied by the parent job. ******************************************************** <br> ")
                stringParam("nexusUrl","${nexusUrl}","please edit the URL of artifacts on Nexus")
                stringParam("settingsXml","${settingsXml}","settings.xml for this job depending of daily builds or release")
            }

            label('kie-rhel7&&!master')

            axes {
                labelExpression("label-exp","kie-rhel7")
                jdk("${jdkVersion}")
                text("container", "tomcat9", "wildfly")
            }

            childCustomWorkspace("\${SHORT_COMBINATION}")

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
                    mavenSettings("${settingsXml}"){
                        variable("SETTINGS_XML_FILE")
                        targetLocation("jenkins-settings.xml")
                    }
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
                shell(jbpmContainerTest)
                maven{
                    mavenInstallation("${mvnTool}")
                    goals("-e -B clean install")
                    rootPOM("jbpm-container-test/pom.xml")
                    mavenOpts("-Xmx3g")
                    providedSettings("${settingsXml}")
                    properties("maven.test.failure.ignore": true)
                    properties("container.profile":"\$container")
                    properties("org.apache.maven.user-settings":"\$SETTINGS_XML_FILE")
                }
            }
        }
    }
}