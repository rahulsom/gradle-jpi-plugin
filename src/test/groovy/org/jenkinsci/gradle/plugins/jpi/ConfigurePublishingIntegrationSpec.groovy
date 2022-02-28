package org.jenkinsci.gradle.plugins.jpi

import groovy.json.JsonSlurper
import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll

import java.nio.file.Paths

class ConfigurePublishingIntegrationSpec extends IntegrationSpec {
    private final String projectName = TestDataGenerator.generateName()
    private File settings
    private File build

    def setup() {
        settings = touchInProjectDir('settings.gradle')
        settings << """rootProject.name = \"$projectName\""""
        build = touchInProjectDir('build.gradle')
        build << '''\
            plugins {
                id 'org.jenkins-ci.jpi'
            }

            tasks.register('discoverPublishingRepos') {
                doLast {
                    def repos = project.publishing.repositories
                        .findAll { it instanceof org.gradle.api.artifacts.repositories.MavenArtifactRepository }
                        .collect { [name: it.name, uri: it.url.toASCIIString()] }
                        .toSet()
                    println groovy.json.JsonOutput.toJson([repositories: repos])
                }
            }

            tasks.register('discoverPublications') {
                doLast {
                    def publications = publishing.publications.collectEntries {
                        [(it.name): [
                                groupId     : it.groupId,
                                artifactId  : it.artifactId,
                                version     : it.version,
                                pomPackaging: it.pom.packaging,
                                artifacts   : it.artifacts.collect {
                                    [classifier: it.classifier,
                                     extension : it.extension,
                                     file      : project.projectDir.toPath().relativize(it.file.toPath()).toString()]
                                }
                        ]
                        ]
                    }
                    println groovy.json.JsonOutput.toJson([publications: publications])
                }
            }
            '''.stripIndent()
    }

    def 'does not create sources and javadoc jars if configurePublishing is disabled'() {
        given:
        build << """
            jenkinsPlugin {
                configurePublishing = false
                jenkinsVersion = '${TestSupport.RECENT_JENKINS_VERSION}'
            }
            """.stripIndent()
        when:
        gradleRunner()
                .withArguments('build')
                .build()

        then:
        existsRelativeToProjectDir("build/libs/${projectName}.hpi")
        existsRelativeToProjectDir("build/libs/${projectName}.jar")
        !existsRelativeToProjectDir("build/libs/${projectName}-sources.jar")
        !existsRelativeToProjectDir("build/libs/${projectName}-javadoc.jar")
    }

    def 'javadoc jar can be created if configurePublishing is disabled but other plugin does it'() {
        given:
        build.text = """
            plugins {
                id 'org.jenkins-ci.jpi'
                id "nebula.maven-publish" version "17.0.5"
                id "nebula.javadoc-jar" version "17.0.5"
             }
            jenkinsPlugin {
                configurePublishing = false
                jenkinsVersion = '${TestSupport.RECENT_JENKINS_VERSION}'
            }
            """.stripIndent()

        when:
        gradleRunner()
                .withArguments('build')
                .build()

        then:
        existsRelativeToProjectDir("build/libs/${projectName}.hpi")
        existsRelativeToProjectDir("build/libs/${projectName}.jar")
        existsRelativeToProjectDir("build/libs/${projectName}-javadoc.jar")
    }

    def 'javadoc and source jar can be created if configurePublishing is disabled but plugin consumer configures publication'() {
        given:
        build.text = """
            plugins {
                id 'org.jenkins-ci.jpi'
                id "maven-publish"
                id "nebula.source-jar" version "17.0.5"
                id "nebula.javadoc-jar" version "17.0.5"
            }
            jenkinsPlugin {
                configurePublishing = false
                jenkinsVersion = '${TestSupport.RECENT_JENKINS_VERSION}'
            }

            afterEvaluate {
                publishing {
                    publications {
                        mavenJpi(MavenPublication) {
                            groupId = 'org.jenkinsci.sample'
                            artifactId = '${projectName}'
                            version = '1.0'
                            artifact jar
                            artifact sourceJar
                            artifact javadocJar
                        }
                    }
                    repositories {
                        maven {
                            name = 'testRepo'
                            url = 'build/testRepo'
                        }
                    }
                }
            }
            """.stripIndent()

        when:
        gradleRunner()
                .withArguments('publishMavenJpiPublicationToTestRepoRepository')
                .build()

        then:
        existsRelativeToProjectDir("build/testRepo/org/jenkinsci/sample/${projectName}/1.0/${projectName}-1.0-javadoc.jar")
        existsRelativeToProjectDir("build/testRepo/org/jenkinsci/sample/${projectName}/1.0/${projectName}-1.0-sources.jar")
    }

    @Unroll
    def 'should configure publishing repository for #status'(String status,
                                                             String version,
                                                             String declaration,
                                                             String expected) {
        given:
        build << """
            jenkinsPlugin {
                $declaration
                jenkinsVersion = '${TestSupport.RECENT_JENKINS_VERSION}'
            }
            version = '$version'
            """.stripIndent()

        when:
        def result = gradleRunner()
                .withArguments('discoverPublishingRepos', '-q')
                .build()
        def actual = deserializeReposFrom(result)

        then:
        actual == [expected]

        where:
        status     | version        | declaration                  | expected
        'snapshot' | '1.0-SNAPSHOT' | 'configurePublishing = true' | 'https://repo.jenkins-ci.org/snapshots'
        'release'  | '1.0'          | 'configurePublishing = true' | 'https://repo.jenkins-ci.org/releases'
        'snapshot' | '1.0-SNAPSHOT' | null                         | 'https://repo.jenkins-ci.org/snapshots'
        'release'  | '1.0'          | null                         | 'https://repo.jenkins-ci.org/releases'
    }

    @Unroll
    def 'should not configure publishing for #status'(String status, String version) {
        given:
        build << """
            jenkinsPlugin {
                configurePublishing = false
                jenkinsVersion = '${TestSupport.RECENT_JENKINS_VERSION}'
            }
            version = '$version'
            """.stripIndent()

        when:
        def result = gradleRunner()
                .withArguments('discoverPublishingRepos', '-q')
                .buildAndFail()

        then:
        result.output.contains("> Could not get unknown property 'publishing' for root project '$projectName' of type org.gradle.api.Project.")

        where:
        status     | version
        'snapshot' | '1.0-SNAPSHOT'
        'release'  | '1.0'
    }

    def 'should configure publication'(String declaration) {
        given:
        def version = '12.1'
        build << """
            jenkinsPlugin {
                $declaration
                jenkinsVersion = '${TestSupport.RECENT_JENKINS_VERSION}'
            }
            version = '$version'
            """.stripIndent()
        def expected = ['mavenJpi': [
                'groupId'     : '',
                'artifactId'  : projectName,
                'version'     : version,
                'pomPackaging': 'hpi',
                'artifacts'   : [
                        ['classifier': null, 'extension': 'jar', 'file': inBuildLibs("${projectName}-${version}.jar")],
                        ['classifier': null, 'extension': 'hpi', 'file': inBuildLibs("${projectName}.hpi")],
                        ['classifier': 'sources', 'extension': 'jar', 'file': inBuildLibs("${projectName}-${version}-sources.jar")],
                        ['classifier': 'javadoc', 'extension': 'jar', 'file': inBuildLibs("${projectName}-${version}-javadoc.jar")],
                ]
        ]]

        when:
        def result = gradleRunner()
                .withArguments('discoverPublications', '-q')
                .build()
        def actual = deserializePublicationsFrom(result)

        then:
        actual == expected

        where:
        declaration << [null, 'configurePublishing = true']
    }

    @Unroll
    def 'should register #task task'(String task, String declaration) {
        given:
        build << """
            jenkinsPlugin {
                $declaration
                jenkinsVersion = '${TestSupport.RECENT_JENKINS_VERSION}'
            }
            """.stripIndent()

        expect:
        gradleRunner()
                .withArguments(task, '-m')
                .build()

        where:
        task         | declaration
        'javadocJar' | null
        'sourcesJar' | null
        'javadocJar' | 'configurePublishing = true'
        'sourcesJar' | 'configurePublishing = true'
    }

    private static List<String> deserializeReposFrom(BuildResult result) {
        new JsonSlurper().parseText(result.output)['repositories']*.uri
    }

    private static Map<String, Map<String, Object>> deserializePublicationsFrom(BuildResult result) {
        new JsonSlurper().parseText(result.output)['publications'] as Map<String, Map<String, Object>>
    }

    private static String inBuildLibs(String rest) {
        Paths.get('build', 'libs', rest).toString()
    }
}
