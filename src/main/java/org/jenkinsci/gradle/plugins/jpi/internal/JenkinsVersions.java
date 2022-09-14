package org.jenkinsci.gradle.plugins.jpi.internal;

import org.gradle.util.GradleVersion;

class JenkinsVersions {
    static final String FIRST_BOM_VERSION = "2.195";

    private JenkinsVersions() {
    }

    static boolean beforeBomExists(String jenkinsVersion) {
        return GradleVersion.version(jenkinsVersion).compareTo(GradleVersion.version(FIRST_BOM_VERSION)) < 0;
    }
}
