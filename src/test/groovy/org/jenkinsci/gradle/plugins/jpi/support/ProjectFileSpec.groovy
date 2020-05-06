package org.jenkinsci.gradle.plugins.jpi.support

import spock.lang.Specification

class ProjectFileSpec extends Specification {
    Indenter indenter = FourSpaceIndenter.create()

    def 'should write empty'() {
        expect:
        ProjectFile.newBuilder().build().emit(indenter) == ''
    }

    def 'should write file with plugin'() {
        expect:
        ProjectFile.newBuilder()
                .withPlugins(PluginsBlock.newBuilder()
                        .withPlugin('org.jenkins-ci.jpi')
                        .build())
                .build()
                .emit(indenter) == '''\
                    plugins {
                        id 'org.jenkins-ci.jpi'
                    }
                    '''.stripIndent()
    }

    def 'should handle plugins with version'() {
        expect:
        ProjectFile.newBuilder()
                .withPlugins(PluginsBlock.newBuilder()
                        .withPlugin('org.jenkins-ci.jpi')
                        .withPlugin('nebula.maven-publish', '17.0.5')
                        .build())
                .build()
                .emit(indenter) == '''\
                    plugins {
                        id 'org.jenkins-ci.jpi'
                        id 'nebula.maven-publish' version '17.0.5'
                    }
                    '''.stripIndent()
    }

    def 'should configure extension with replacements'() {
        expect:
        ProjectFile.newBuilder()
                .withBlock(CodeBlock.newBuilder('jenkinsPlugin')
                        .addStatement('configurePublishing = $L', true)
                        .addStatement('fileExtension = $S', 'jpi')
                        .build())
                .build()
                .emit(indenter) == '''\
                    jenkinsPlugin {
                        configurePublishing = true
                        fileExtension = 'jpi'
                    }
                    '''.stripIndent()
    }

    def 'should handle dependencies'() {
        expect:
        ProjectFile.newBuilder()
                .withDependencies(DependenciesBlock.newBuilder()
                        .addImplementation('com.google.guava:guava:19.0')
                        .add('testImplementation', 'junit:junit:4.12')
                        .build())
                .build()
                .emit(indenter) == '''\
                    dependencies {
                        implementation 'com.google.guava:guava:19.0'
                        testImplementation 'junit:junit:4.12'
                    }
                    '''.stripIndent()
    }

    def 'should clear dependencies'() {
        given:
        def builder = ProjectFile.newBuilder()
                .withDependencies(DependenciesBlock.newBuilder()
                        .addImplementation('com.google.guava:guava:19.0')
                        .add('testImplementation', 'junit:junit:4.12')
                        .build())
                .clearDependencies()
                .withDependencies(DependenciesBlock.newBuilder()
                        .addImplementation('com.squareup.okio:okio:2.6.0')
                        .build())

        when:
        def actual = builder.build().emit(indenter)

        then:
        actual == '''\
                    dependencies {
                        implementation 'com.squareup.okio:okio:2.6.0'
                    }
                    '''.stripIndent()
    }

    def 'should do it all'() {
        expect:
        ProjectFile.newBuilder()
                .withPlugins(PluginsBlock.newBuilder()
                        .withPlugin('org.jenkins-ci.jpi')
                        .build())
                .withBlock(CodeBlock.newBuilder('jenkinsPlugin')
                        .addStatement('coreVersion = $S', '2.222.3')
                        .build())
                .withDependencies(DependenciesBlock.newBuilder()
                        .addImplementation('com.google.guava:guava:19.0')
                        .build())
                .build()
                .emit(indenter) == '''\
                    plugins {
                        id 'org.jenkins-ci.jpi'
                    }

                    jenkinsPlugin {
                        coreVersion = '2.222.3'
                    }

                    dependencies {
                        implementation 'com.google.guava:guava:19.0'
                    }
                    '''.stripIndent()
    }
}
