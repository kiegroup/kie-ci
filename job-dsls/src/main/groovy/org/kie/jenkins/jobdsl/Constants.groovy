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
    static final String MAVEN_VERSION = "3.6.3"
    static final String UPSTREAM_BUILD_MAVEN_VERSION = "3.6.3"
    static final String JDK_VERSION = "kie-jdk1.8"
    static final String BRANCH = "master"
    static final String GITHUB_ORG_UNIT = "kiegroup"
    static final String PULL_REQUEST_FOLDER = "pullrequest"
    static final String PULL_REQUEST_FOLDER_DISPLAY_NAME = "pullrequest"
    static final String FDB_FOLDER = "fdb"
    static final String FDB_FOLDER_DISPLAY_NAME = "fullDownstream"
    static final String CDB_FOLDER = "compile"
    static final String CDB_FOLDER_DISPLAY_NAME = "compileDownstream"
    static final String UPSTREAM_FOLDER = "upstream"
    static final String UPSTREAM_FOLDER_DISPLAY_NAME = "upstream"
    static final String DOWNSTREAM_PRODUCT_FOLDER = "fdbp"
    static final String DOWNSTREAM_PRODUCT_FOLDER_DISPLAY_NAME = "downstream-production"
    static final String DEPLOY_FOLDER = "deployedRep"
    static final String KIE_PREFIX = "7.50.0"
    static final String NUMBER_OF_KIE_USERS = "10"
    static final String SONARCLOUD_FOLDER = "sonarcloud"
    static final String REPORT_BRANCH = "7.x"
    static final String BUILD_DATE = ""
    static final String PROD_VERSION = "7.11.0"
    static final String OSBS_BUILD_TARGET = "rhba-7-rhel-8-containers-candidate"
    static final String CEKIT_BUILD_OPTIONS = ""
    static final String KERBEROS_PRINCIPAL = ""
    static final String OSBS_BUILD_USER = ""
    static final String KERBEROS_KEYTAB = ""
    static final String KERBEROS_CRED = ""
    static final String IMAGE_REPO = ""
    static final String IMAGE_BRANCH = ""
    static final String IMAGE_SUBDIR = ""
    static final String GIT_USER = ""
    static final String GIT_EMAIL = ""
    static final String CEKIT_CACHE_LOCAL = ""
    static final String VERBOSE	= "false"
    static final String LOCAL_MVN_REP = "/home/jenkins/.m2/repository"
    static final String REPO_FILE_URL = "https://raw.githubusercontent.com/kiegroup/droolsjbpm-build-bootstrap/master/script/repository-list.txt"
    static final String SRCCLR_JOBS_FOLDER = "srcclr"

}
