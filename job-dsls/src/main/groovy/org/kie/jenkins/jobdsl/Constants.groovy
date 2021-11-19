/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.kie.jenkins.jobdsl


class Constants {
    static final String MAVEN_VERSION = '3.8.1'
    static final String MAVEN_TOOL = "kie-maven-${MAVEN_VERSION}"
    static final String UPSTREAM_BUILD_MAVEN_VERSION = '3.8.1'
    static final String JDK_VERSION = '11'
    static final String JDK_TOOL = "kie-jdk${JDK_VERSION}"
    static final String BRANCH = 'main'
    static final String GITHUB_ORG_UNIT = 'kiegroup'
    static final String PULL_REQUEST_FOLDER = 'pullrequest'
    static final String PULL_REQUEST_FOLDER_DISPLAY_NAME = 'pullrequest'
    static final String FDB_FOLDER = 'fdb'
    static final String FDB_FOLDER_DISPLAY_NAME = 'fullDownstream'
    static final String CDB_FOLDER = 'compile'
    static final String CDB_FOLDER_DISPLAY_NAME = 'compileDownstream'
    static final String DOWNSTREAM_PRODUCT_FOLDER = 'fdbp'
    static final String DOWNSTREAM_PRODUCT_FOLDER_DISPLAY_NAME = 'downstream-production'
    static final String DEPLOY_FOLDER = 'deployedRep'
    static final String KIE_PREFIX = '7.63.0'
    static final String SONARCLOUD_FOLDER = 'sonarcloud'
    static final String REPORT_BRANCH = '7.x'
    static final String BUILD_DATE = ''
    static final String NEXT_PROD_VERSION = '7.13.0'
    static final String CURRENT_PROD_VERSION = '7.12.0'
    static final String OSBS_BUILD_TARGET = 'rhba-7-rhel-8-containers-candidate'
    static final String CEKIT_BUILD_OPTIONS = ''
    static final String KERBEROS_PRINCIPAL = ''
    static final String OSBS_BUILD_USER = ''
    static final String KERBEROS_KEYTAB = ''
    static final String KERBEROS_CRED = ''
    static final String IMAGE_REPO = ''
    static final String IMAGE_BRANCH = ''
    static final String IMAGE_SUBDIR = ''
    static final String GIT_USER = ''
    static final String GIT_EMAIL = ''
    static final String CEKIT_CACHE_LOCAL = ''
    static final String VERBOSE	= 'false'
    static final String LOCAL_MVN_REP = '/home/jenkins/.m2/repository'
    static final String REPO_FILE_URL = "https://raw.githubusercontent.com/kiegroup/droolsjbpm-build-bootstrap/${BRANCH}/script/repository-list.txt"
    static final String SRCCLR_JOBS_FOLDER = 'srcclr'
    static final String PACKER_URL = 'https://releases.hashicorp.com/packer/1.7.2/packer_1.7.2_linux_amd64.zip'
    static final String FINDBUGS_FILE = '**/spotbugsXml.xml'
    static final String CHECKSTYLE_FILE = '**/checkstyle.log'
    static final String RHBA_VERSION_PREFIX = "${KIE_PREFIX}.redhat-"
    static final String EAP7_DOWNLOAD_URL = 'http://download.devel.redhat.com/released/JBoss-middleware/eap7/7.4.0/jboss-eap-7.4.0.zip'
    /**
     * The value of Use custom child workspace field for matrix jobs - the SHORT_COMBINATION environment variable
     * is handled by the short-workspace-path Jenkins plugin.
     */
    static final String MATRIX_SHORT_CHILD_WORKSPACE = '${SHORT_COMBINATION}'
}
