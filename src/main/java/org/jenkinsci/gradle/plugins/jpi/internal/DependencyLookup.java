package org.jenkinsci.gradle.plugins.jpi.internal;

import org.gradle.util.GradleVersion;

import java.util.HashSet;
import java.util.Set;

public class DependencyLookup {
    
    public Set<String> find(String configuration, String jenkinsVersion) {
        Set<String> deps = new HashSet<>();
        String core = "org.jenkins-ci.main:jenkins-core:" + jenkinsVersion;
        String findbugs = "com.google.code.findbugs:annotations:3.0.0";
        GradleVersion version = GradleVersion.version(jenkinsVersion);
        if (version.compareTo(GradleVersion.version("1.618")) < 0) {
            findbugs = "findbugs:annotations:1.0.0";
        }
        String servlet = "javax.servlet:javax.servlet-api:3.1.0";
        if (version.compareTo(GradleVersion.version("2.0")) < 0) {
            servlet = "javax.servlet:servlet-api:2.4";
        }
        String testHarness = "org.jenkins-ci.main:jenkins-test-harness:1529.v4fd5bafdcd33";
        if (version.compareTo(GradleVersion.version("2.64")) < 0) {
            testHarness = "org.jenkins-ci.main:jenkins-test-harness:2.0";
        }
        if (version.compareTo(GradleVersion.version("1.644")) <= 0) {
            testHarness = "org.jenkins-ci.main:jenkins-test-harness:" + jenkinsVersion;
        }
        String uiSamples = "org.jenkins-ci.main:ui-samples-plugin:2.0";
        if (version.compareTo(GradleVersion.version("1.533")) < 0) {
            uiSamples = "org.jenkins-ci.main:ui-samples-plugin:" + jenkinsVersion;
        }
        switch (configuration) {
            case "annotationProcessor":
                deps.add(core);
                deps.add(servlet);
                return deps;
            case "compileOnly":
                deps.add(core);
                deps.add(findbugs);
                deps.add(servlet);
                return deps;
            case "testAnnotationProcessor":
                deps.add("net.java.sezpoz:sezpoz:1.13");
                return deps;
            case "testImplementation":
                deps.add(core);
                deps.add(testHarness);
                deps.add(uiSamples);
                if (version.compareTo(GradleVersion.version("1.505")) < 0) {
                    deps.add("junit:junit-dep:4.10");
                }
                return deps;
            case "testCompileOnly":
                deps.add("net.jcip:jcip-annotations:1.0");
                deps.add(findbugs);
                return deps;
            case "generatedJenkinsTestImplementation":
                deps.add(core);
                deps.add(testHarness);
                return deps;
            case "declaredJenkinsWar":
                deps.add("org.jenkins-ci.main:jenkins-war:" + jenkinsVersion + "@war");
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
}
