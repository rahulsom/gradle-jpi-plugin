package org.jenkinsci.gradle.plugins.jpi

import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import spock.lang.Requires

class SpotBugsPluginSpec extends IntegrationSpec {

    private final String projectName = TestDataGenerator.generateName()
    private File settings
    private File build

    def setup() {
        settings = touchInProjectDir('settings.gradle')
        settings << """rootProject.name = \"$projectName\""""
        build = touchInProjectDir('build.gradle')
        build << """\
            plugins {
                id 'org.jenkins-ci.jpi'
            }
            """.stripIndent()
        TestSupport.CALCULATOR.writeTo(inProjectDir('src/main/java'))
        TestSupport.PASSING_TEST.writeTo(inProjectDir('src/test/java'))
    }

    @Requires({ gradle7AndAbove() })
    def "should not run SpotBugs tasks by default (Gradle 7)"() {
        given:
        build << """jenkinsPlugin {
            jenkinsVersion = '${TestSupport.RECENT_JENKINS_VERSION}'
        }""".stripIndent()

        when:
        def result = gradleRunner()
            .withArguments('build')
            .build()

        then:
        result.task(':spotbugsMain') == null
        result.task(':spotbugsTest') == null
    }

    @Requires({ gradle7AndAbove() })
    def "should run SpotBugs tasks with default and generate only xml"() {
        given:
        build << """
            jenkinsPlugin {
                jenkinsVersion = '${TestSupport.RECENT_JENKINS_VERSION}'
                enableSpotBugs()
            }""".stripIndent()

        when:
        def result = gradleRunner()
            .withArguments('build')
            .build()

        then:
        result.task(':spotbugsMain').outcome == TaskOutcome.SUCCESS
        result.task(':spotbugsTest').outcome == TaskOutcome.SUCCESS
        existsRelativeToProjectDir('build/reports/spotbugs/main.xml')
        !existsRelativeToProjectDir('build/reports/spotbugs/main.html')
    }

    @Requires({ gradle7AndAbove() })
    def "should override default SpotBugs config"() {
        given:
        build << """
            jenkinsPlugin {
                jenkinsVersion = '${TestSupport.RECENT_JENKINS_VERSION}'
                enableSpotBugs()
            }
            spotbugsMain {
                reports {
                    html {
                        required = true
                        outputLocation = file("\$buildDir/reports/spotbugs/main/spotbugs.html")
                    }
                }
            }

            """.stripIndent()

        when:
        def result = gradleRunner()
            .withArguments('build')
            .build()

        then:
        result.task(':spotbugsMain').outcome == TaskOutcome.SUCCESS
        existsRelativeToProjectDir('build/reports/spotbugs/main/spotbugs.html')
    }

    @Requires({ belowGradle7() })
    def "should not run SpotBugs tasks and not fail (Gradle 6)"() {
        given:
        build << """jenkinsPlugin {
            jenkinsVersion = '${TestSupport.RECENT_JENKINS_VERSION}'
        }""".stripIndent()

        when:
        def result = gradleRunner()
            .withArguments('build')
            .build()

        then:
        result.task(':spotbugsMain') == null
        result.task(':spotbugsTest') == null
    }

    static boolean gradle7AndAbove() {
        gradleVersionForTest >= GradleVersion.version('7.0')
    }

    static boolean belowGradle7() {
        !gradle7AndAbove()
    }

}
