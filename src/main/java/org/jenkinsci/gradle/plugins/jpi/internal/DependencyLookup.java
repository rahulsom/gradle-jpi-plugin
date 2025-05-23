package org.jenkinsci.gradle.plugins.jpi.internal;

import shaded.hudson.util.VersionNumber;

import java.util.HashSet;
import java.util.Set;

/**
 * Provides dependency lookup functionality for Jenkins plugins based on Jenkins version.
 * <p>
 * This class determines the appropriate dependencies for different Gradle configurations
 * based on the specified Jenkins version, handling version-specific dependencies.
 */
public class DependencyLookup {

    /**
     * Finds the appropriate dependencies for a given configuration and Jenkins version.
     *
     * @param configuration The Gradle configuration name
     * @param jenkinsVersion The Jenkins version string
     * @return A set of dependency factories for the specified configuration
     */
    public Set<DependencyFactory> find(String configuration, String jenkinsVersion) {
        VersionNumber version = new VersionNumber(jenkinsVersion);
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
            case "testAnnotationProcessor":
                deps.addAll(coreSet);
                return deps;
            case "testImplementation":
                deps.addAll(coreSet);
                deps.add(testHarness);
                if (version.isOlderThan(new VersionNumber("1.505"))) {
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
                deps.add(new MavenDependency("org.jenkins-ci.main:jenkins-war:" + jenkinsVersion));
                return deps;
        }
        return deps;
    }

    /**
     * Gets the set of Gradle configurations that this lookup supports.
     *
     * @return A set of configuration names
     */
    public Set<String> configurations() {
        Set<String> configurations = new HashSet<>();
        configurations.add("annotationProcessor");
        configurations.add("compileOnly");
        configurations.add("declaredJenkinsWar");
        configurations.add("testAnnotationProcessor");
        configurations.add("testCompileOnly");
        configurations.add("testImplementation");
        configurations.add("generatedJenkinsTestImplementation");
        return configurations;
    }

    /**
     * Determines the appropriate FindBugs/SpotBugs dependency for the given Jenkins version.
     *
     * @param version The Jenkins version
     * @param beforeBomExists Whether the version is before Jenkins BOM was introduced
     * @return A dependency factory for the appropriate FindBugs/SpotBugs dependency
     */
    private static DependencyFactory findbugsFor(VersionNumber version, boolean beforeBomExists) {
        if (version.compareTo(new VersionNumber("1.618")) < 0) {
            return new MavenDependency("findbugs:annotations:1.0.0");
        } else if (beforeBomExists) {
            return new MavenDependency("com.google.code.findbugs:annotations:3.0.0");
        } else {
            return new MavenDependency("com.github.spotbugs:spotbugs-annotations");
        }
    }

    /**
     * Determines the appropriate Servlet API dependency for the given Jenkins version.
     *
     * @param version The Jenkins version
     * @return A dependency factory for the appropriate Servlet API dependency
     */
    private static DependencyFactory servletFor(VersionNumber version) {
        if (version.isOlderThan(new VersionNumber("2.0"))) {
            return new MavenDependency("javax.servlet:servlet-api:2.4");
        } else if (version.isOlderThan(new VersionNumber("2.475"))) {
            return new MavenDependency("javax.servlet:javax.servlet-api:3.1.0");
        } else {
            return new MavenDependency("jakarta.servlet:jakarta.servlet-api:5.0.0");
        }
    }

    /**
     * Determines the appropriate Jenkins test harness dependency for the given Jenkins version.
     *
     * @param version The Jenkins version
     * @return A dependency factory for the appropriate test harness dependency
     */
    private static DependencyFactory testHarnessFor(VersionNumber version) {
        if (version.isOlderThanOrEqualTo(new VersionNumber("1.644"))) {
            return new MavenDependency("org.jenkins-ci.main:jenkins-test-harness:" + version);
        } else if (version.isOlderThan(new VersionNumber("2.64"))) {
            return new MavenDependency( "org.jenkins-ci.main:jenkins-test-harness:2.0");
        } else if (version.isOlderThan(new VersionNumber("2.475"))) {
            return new MavenDependency("org.jenkins-ci.main:jenkins-test-harness:2112.ve584e0edc63b_");
        } else {
            return new MavenDependency("org.jenkins-ci.main:jenkins-test-harness:2391.v9b_3e2d3351a_2");
        }
    }
}
