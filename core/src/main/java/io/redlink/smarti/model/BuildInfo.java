/*
 * Copyright 2017 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.redlink.smarti.model;

/**
 */
public class BuildInfo {

    private final String buildVersion;
    private final String buildTime;
    private final String buildUserName;
    private final String buildUserEmail;
    private final String buildHost;
    private final String commitId;
    private final String commitIdAbbrev;
    private final String commitDescribe;
    private final String commitDescribeShort;

    private final String branch;
    private final String tags;

    public BuildInfo(String buildVersion, String buildTime, String buildUserName, String buildUserEmail, String buildHost, String commitId, String commitIdAbbrev, String commitDescribe, String commitDescribeShort, String branch, String tags) {
        this.buildVersion = buildVersion;
        this.buildTime = buildTime;
        this.buildUserName = buildUserName;
        this.buildUserEmail = buildUserEmail;
        this.buildHost = buildHost;
        this.commitId = commitId;
        this.commitIdAbbrev = commitIdAbbrev;
        this.commitDescribe = commitDescribe;
        this.commitDescribeShort = commitDescribeShort;
        this.branch = branch;
        this.tags = tags;
    }


    public String getBuildVersion() {
        return buildVersion;
    }

    public String getBuildTime() {
        return buildTime;
    }

    public String getBuildUserName() {
        return buildUserName;
    }

    public String getBuildUserEmail() {
        return buildUserEmail;
    }

    public String getBuildHost() {
        return buildHost;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getCommitIdAbbrev() {
        return commitIdAbbrev;
    }

    public String getCommitDescribe() {
        return commitDescribe;
    }

    public String getCommitDescribeShort() {
        return commitDescribeShort;
    }

    public String getBranch() {
        return branch;
    }

    public String getTags() {
        return tags;
    }

    public String getFullVersion() {
        return getBuildVersion();
    }

    public String getMajorVersion() {
        return getBuildVersion().replaceFirst("^(\\w+)\\.\\w+\\.\\w(?i:-SNAPSHOT)?", "$1");
    }

    public String getMajorMinorVersion() {
        return getBuildVersion().replaceFirst("^(\\w+\\.\\w+)\\.\\w(?i:-SNAPSHOT)?", "$1");
    }

}
