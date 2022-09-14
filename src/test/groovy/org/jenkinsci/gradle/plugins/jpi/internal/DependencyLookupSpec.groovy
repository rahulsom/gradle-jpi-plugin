package org.jenkinsci.gradle.plugins.jpi.internal

import spock.lang.Specification
import spock.lang.Unroll

class DependencyLookupSpec extends Specification {
    private final DependencyLookup lookup = new DependencyLookup()

    def 'should get annotationProcessor dependencies by version'() {
        when:
        def actual = this.lookup.find('annotationProcessor', '2.0')

        then:
        actual == [
                'org.jenkins-ci.main:jenkins-core:2.0',
                'javax.servlet:javax.servlet-api:3.1.0',
        ] as Set
    }

    def 'should get testAnnotationProcessor dependencies by version'() {
        when:
        def actual = lookup.find('testAnnotationProcessor', '2.0')

        then:
        actual == [
                'net.java.sezpoz:sezpoz:1.13',
        ] as Set
    }

    @Unroll
    def 'should get compileOnly dependencies for #version'(String version, Set<String> expected) {
        when:
        def actual = lookup.find('compileOnly', version)

        then:
        actual == expected

        where:
        version | expected
        '1.617' | ['org.jenkins-ci.main:jenkins-core:1.617', 'findbugs:annotations:1.0.0', 'javax.servlet:servlet-api:2.4'] as Set
        '1.618' | ['org.jenkins-ci.main:jenkins-core:1.618', 'com.google.code.findbugs:annotations:3.0.0', 'javax.servlet:servlet-api:2.4'] as Set
        '2.0'   | ['org.jenkins-ci.main:jenkins-core:2.0', 'com.google.code.findbugs:annotations:3.0.0', 'javax.servlet:javax.servlet-api:3.1.0'] as Set
    }

    @Unroll
    def 'should get testImplementation dependencies for #version'(String version, Set<String> expected) {
        when:
        def actual = lookup.find('testImplementation', version)

        then:
        actual == expected

        where:
        version | expected
        '1.504' | ['org.jenkins-ci.main:jenkins-core:1.504', 'org.jenkins-ci.main:jenkins-test-harness:1.504', 'org.jenkins-ci.main:ui-samples-plugin:1.504', 'junit:junit-dep:4.10'] as Set
        '1.532' | ['org.jenkins-ci.main:jenkins-core:1.532', 'org.jenkins-ci.main:jenkins-test-harness:1.532', 'org.jenkins-ci.main:ui-samples-plugin:1.532'] as Set
        '1.644' | ['org.jenkins-ci.main:jenkins-core:1.644', 'org.jenkins-ci.main:jenkins-test-harness:1.644', 'org.jenkins-ci.main:ui-samples-plugin:2.0'] as Set
        '1.645' | ['org.jenkins-ci.main:jenkins-core:1.645', 'org.jenkins-ci.main:jenkins-test-harness:2.0', 'org.jenkins-ci.main:ui-samples-plugin:2.0'] as Set
        '2.64'  | ['org.jenkins-ci.main:jenkins-core:2.64', 'org.jenkins-ci.main:jenkins-test-harness:1837.vb_6efb_1790942', 'org.jenkins-ci.main:ui-samples-plugin:2.0'] as Set
    }

    @Unroll
    def 'should get testCompileOnly dependencies for #version'(String version, Set<String> expected) {
        when:
        def actual = lookup.find('testCompileOnly', version)

        then:
        actual == expected

        where:
        version   | expected
        '1.617'   | ['findbugs:annotations:1.0.0', 'net.jcip:jcip-annotations:1.0'] as Set
        '2.222.3' | ['com.google.code.findbugs:annotations:3.0.0', 'net.jcip:jcip-annotations:1.0'] as Set
    }

    def 'should get declaredJenkinsWar dependencies for version'() {
        when:
        def actual = lookup.find('declaredJenkinsWar', '2.222.3')

        then:
        actual == [
                'org.jenkins-ci.main:jenkins-war:2.222.3@war',
        ] as Set<String>
    }

    def 'should get generatedJenkinsTestImplementation dependencies for version'() {
        when:
        def actual = lookup.find('generatedJenkinsTestImplementation', '2.222.3')

        then:
        actual == [
                'org.jenkins-ci.main:jenkins-core:2.222.3',
                'org.jenkins-ci.main:jenkins-test-harness:1837.vb_6efb_1790942',
        ] as Set<String>
    }
}
