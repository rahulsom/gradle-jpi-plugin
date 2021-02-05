package org.jenkinsci.gradle.plugins.jpi.internal

import groovy.transform.CompileStatic
import org.gradle.util.GradleVersion

@CompileStatic
class DependencyLookup {
    Set<String> find(String configuration, String jenkinsVersion) {
        String core = "org.jenkins-ci.main:jenkins-core:${jenkinsVersion}"
        String war = "org.jenkins-ci.main:jenkins-war:${jenkinsVersion}"
        String findbugs = 'com.google.code.findbugs:annotations:3.0.0'
        def version = GradleVersion.version(jenkinsVersion)
        if (version < GradleVersion.version('1.618')) {
            findbugs = 'findbugs:annotations:1.0.0'
        }
        String servlet = 'javax.servlet:javax.servlet-api:3.1.0'
        if (version < GradleVersion.version('2.0')) {
            servlet = 'javax.servlet:servlet-api:2.4'
        }
        String testHarness = 'org.jenkins-ci.main:jenkins-test-harness:2.71'
        if (version < GradleVersion.version('2.64')) {
            testHarness = 'org.jenkins-ci.main:jenkins-test-harness:2.0'
        }
        if (version <= GradleVersion.version('1.644')) {
            testHarness = "org.jenkins-ci.main:jenkins-test-harness:${jenkinsVersion}"
        }
        String uiSamples = 'org.jenkins-ci.main:ui-samples-plugin:2.0'
        if (version < GradleVersion.version('1.533')) {
            uiSamples = "org.jenkins-ci.main:ui-samples-plugin:${jenkinsVersion}"
        }
        switch (configuration) {
            case 'annotationProcessor':
                return [core, servlet] as Set
            case 'compileOnly':
                return [core, findbugs, servlet] as Set
            case 'testAnnotationProcessor':
                return ['net.java.sezpoz:sezpoz:1.13'] as Set
            case 'testImplementation':
                Set<String> deps = [core, testHarness, uiSamples] as Set
                if (version < GradleVersion.version('1.505')) {
                    deps.add('junit:junit-dep:4.10')
                }
                return deps
            case 'testCompileOnly':
                return ['net.jcip:jcip-annotations:1.0', findbugs] as Set
            case 'testRuntimeOnly':
                return [war] as Set
            case 'generatedJenkinsTestImplementation':
                return [war, testHarness] as Set
            default:
                [] as Set
        }
    }

    Set<String> configurations() {
        [
                'annotationProcessor',
                'compileOnly',
                'testCompileOnly',
                'testImplementation',
                'testRuntimeOnly',
                'generatedJenkinsTestImplementation',
        ] as Set
    }
}
