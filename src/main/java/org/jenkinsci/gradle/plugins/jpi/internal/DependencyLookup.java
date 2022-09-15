package org.jenkinsci.gradle.plugins.jpi.internal;

import org.apache.tools.ant.taskdefs.optional.depend.Depend;
import org.gradle.util.GradleVersion;

import java.util.HashSet;
import java.util.Set;

public class DependencyLookup {

    public Set<DependencyFactory> find(String configuration, String jenkinsVersion) {
        GradleVersion version = GradleVersion.version(jenkinsVersion);
        boolean beforeBomExists = JenkinsVersions.beforeBomExists(jenkinsVersion);
        Set<DependencyFactory> deps = new HashSet<>();
        Set<DependencyFactory> coreSet = new HashSet<>();
        coreSet.add(new MavenDependency("org.jenkins-ci.main:jenkins-core:" + jenkinsVersion));
        if (!beforeBomExists) {
            coreSet.add(new BomDependency("org.jenkins-ci.main:jenkins-bom:" + jenkinsVersion));
        }
        DependencyFactory findbugs = findbugsFor(version, beforeBomExists);
        DependencyFactory servlet = servletFor(version);
        DependencyFactory testHarness = testHarnessFor(version);
        DependencyFactory uiSamples = uiSamplesFor(version);
        switch (configuration) {
            case "annotationProcessor":
                deps.addAll(coreSet);
                deps.add(servlet);
                return deps;
            case "compileOnly":
                deps.addAll(coreSet);
                deps.add(findbugs);
                deps.add(servlet);
                return deps;
            case "testImplementation":
                deps.addAll(coreSet);
                deps.add(testHarness);
                deps.add(uiSamples);
                if (version.compareTo(GradleVersion.version("1.505")) < 0) {
                    deps.add(new MavenDependency("junit:junit-dep:4.10"));
                }
                return deps;
            case "testCompileOnly":
                deps.add(new MavenDependency("net.jcip:jcip-annotations:1.0"));
                deps.add(findbugs);
                return deps;
            case "generatedJenkinsTestImplementation":
                deps.addAll(coreSet);
                deps.add(testHarness);
                return deps;
            case "declaredJenkinsWar":
                deps.add(new MavenDependency("org.jenkins-ci.main:jenkins-war:" + jenkinsVersion + "@war"));
                return deps;
        }
        return deps;
    }

    public Set<String> configurations() {
        Set<String> configurations = new HashSet<>();
        configurations.add("annotationProcessor");
        configurations.add("compileOnly");
        configurations.add("declaredJenkinsWar");
        configurations.add("testCompileOnly");
        configurations.add("testImplementation");
        configurations.add("generatedJenkinsTestImplementation");
        return configurations;
    }

    private static DependencyFactory findbugsFor(GradleVersion version, boolean beforeBomExists) {
        String findbugs = "com.github.spotbugs:spotbugs-annotations";
        if (version.compareTo(GradleVersion.version("1.618")) < 0) {
            findbugs = "findbugs:annotations:1.0.0";
        } else if (beforeBomExists) {
            findbugs = "com.google.code.findbugs:annotations:3.0.0";
        }
        return new MavenDependency(findbugs);
    }

    private static DependencyFactory servletFor(GradleVersion version) {
        boolean isBefore2 = version.compareTo(GradleVersion.version("2.0")) < 0;
        String notation = isBefore2 ? "javax.servlet:servlet-api:2.4" : "javax.servlet:javax.servlet-api:3.1.0";
        return new MavenDependency(notation);
    }

    private static DependencyFactory testHarnessFor(GradleVersion version) {
        String testHarness = "org.jenkins-ci.main:jenkins-test-harness:1837.vb_6efb_1790942";
        if (version.compareTo(GradleVersion.version("2.64")) < 0) {
            testHarness = "org.jenkins-ci.main:jenkins-test-harness:2.0";
        }
        if (version.compareTo(GradleVersion.version("1.644")) <= 0) {
            testHarness = "org.jenkins-ci.main:jenkins-test-harness:" + version.getVersion();
        }
        return new MavenDependency(testHarness);
    }

    private static DependencyFactory uiSamplesFor(GradleVersion version) {
        String uiSamples = "org.jenkins-ci.main:ui-samples-plugin:2.0";
        if (version.compareTo(GradleVersion.version("1.533")) < 0) {
            uiSamples = "org.jenkins-ci.main:ui-samples-plugin:" + version.getVersion();
        }
        return new MavenDependency(uiSamples);
    }
}
