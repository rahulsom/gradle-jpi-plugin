package org.jenkinsci.gradle.plugins.manifest

import org.gradle.testkit.runner.TaskOutcome
import org.jenkinsci.gradle.plugins.jpi.IntegrationSpec
import org.jenkinsci.gradle.plugins.jpi.TestDataGenerator
import org.jenkinsci.gradle.plugins.jpi.TestSupport
import spock.lang.Unroll

import static org.jenkinsci.gradle.plugins.jpi.TestSupport.q

class GenerateJenkinsManifestTaskIntegrationSpec extends IntegrationSpec {
    private final String projectName = TestDataGenerator.generateName()
    private final String taskName = GenerateJenkinsManifestTask.NAME
    private final String taskPath = ':' + GenerateJenkinsManifestTask.NAME
    private static final String MIN_BUILD_FILE = """\
            plugins {
                id 'org.jenkins-ci.jpi'
            }
            """.stripIndent()
    private static final String BUILD_FILE = """\
            $MIN_BUILD_FILE
            jenkinsPlugin {
                jenkinsVersion.set('${TestSupport.RECENT_JENKINS_VERSION}')
            }
            """.stripIndent()
    private File build

    def setup() {
        File settings = projectDir.newFile('settings.gradle')
        settings << """rootProject.name = \"$projectName\""""
        build = projectDir.newFile('build.gradle')
        def props = new Properties()
        props.setProperty('version', '1.0.0')
        new File(projectDir.root, 'gradle.properties').withOutputStream {
            props.store(it, 'generated')
        }
    }

    def 'should rerun if group changes'() {
        given:
        build.text = """\
            $BUILD_FILE
            group = 'a'
            """.stripIndent()

        when:
        def result = gradleRunner()
                .withArguments(taskName)
                .build()

        then:
        result.task(taskPath).outcome == TaskOutcome.SUCCESS

        when:
        build.text = """\
            $BUILD_FILE
            group = 'b'
            """.stripIndent()
        def rerunResult = gradleRunner()
                .withArguments(taskName)
                .build()

        then:
        rerunResult.task(taskPath).outcome == TaskOutcome.SUCCESS

        when:
        def thirdRun = gradleRunner()
                .withArguments(taskName)
                .build()

        then:
        thirdRun.task(taskPath).outcome == TaskOutcome.UP_TO_DATE
    }

    def 'should rerun if version changes'() {
        given:
        build.text = """\
            $BUILD_FILE
            version = '1.0'
            """.stripIndent()

        when:
        def result = gradleRunner()
                .withArguments(taskName)
                .build()

        then:
        result.task(taskPath).outcome == TaskOutcome.SUCCESS

        when:
        build.text = """\
            $BUILD_FILE
            version = '2.0'
            """.stripIndent()
        def rerunResult = gradleRunner()
                .withArguments(taskName)
                .build()

        then:
        rerunResult.task(taskPath).outcome == TaskOutcome.SUCCESS

        when:
        def thirdRun = gradleRunner()
                .withArguments(taskName)
                .build()

        then:
        thirdRun.task(taskPath).outcome == TaskOutcome.UP_TO_DATE
    }

    def 'should rerun if target compatibility changes'() {
        given:
        build.text = """\
            $BUILD_FILE
            java {
                targetCompatibility = JavaVersion.VERSION_1_8
            }
            """.stripIndent()

        when:
        def result = gradleRunner()
                .withArguments(taskName)
                .build()

        then:
        result.task(taskPath).outcome == TaskOutcome.SUCCESS

        when:
        build.text = """\
            $BUILD_FILE
            java {
                targetCompatibility = JavaVersion.VERSION_11
            }
            """.stripIndent()
        def rerunResult = gradleRunner()
                .withArguments(taskName)
                .build()

        then:
        rerunResult.task(taskPath).outcome == TaskOutcome.SUCCESS

        when:
        def thirdRun = gradleRunner()
                .withArguments(taskName)
                .build()

        then:
        thirdRun.task(taskPath).outcome == TaskOutcome.UP_TO_DATE
    }

    @Unroll
    def 'should rerun if #field changes'(String field, String before, String after) {
        given:
        build.text = """\
            $MIN_BUILD_FILE
            jenkinsPlugin {
                jenkinsVersion.set('${TestSupport.RECENT_JENKINS_VERSION}')
                $field = $before
            }
            """.stripIndent()

        when:
        def result = gradleRunner()
                .withArguments(taskName)
                .build()

        then:
        result.task(taskPath).outcome == TaskOutcome.SUCCESS

        when:
        build.text = """\
            $MIN_BUILD_FILE
            jenkinsPlugin {
                jenkinsVersion.set('${TestSupport.RECENT_JENKINS_VERSION}')
                $field = $after
            }
            """.stripIndent()
        def rerunResult = gradleRunner()
                .withArguments(taskName, '-i')
                .build()

        then:
        !rerunResult.output.contains('Duplicate name in Manifest')
        rerunResult.task(taskPath).outcome == TaskOutcome.SUCCESS

        when:
        def thirdRun = gradleRunner()
                .withArguments(taskName)
                .build()

        then:
        thirdRun.task(taskPath).outcome == TaskOutcome.UP_TO_DATE

        where:
        field                    | before                  | after
        'shortName'              | q('a')                  | q('b')
        'displayName'            | q('a')                  | q('b')
        'url'                    | q('http://localhost/a') | q('http://localhost/b')
        'compatibleSinceVersion' | q('2.235.1')            | q('2.249.1')
        'sandboxStatus'          | 'true'                  | 'false'
        'pluginFirstClassLoader' | 'true'                  | 'false'
        'maskClasses'            | q('com.google.guava.')  | q('com.google.guava. org.junit.')
    }

    def 'should rerun if plugin developers change'(List<String> before, List<String> after) {
        given:
        build.text = """\
            $MIN_BUILD_FILE
            jenkinsPlugin {
                jenkinsVersion.set('${TestSupport.RECENT_JENKINS_VERSION}')
                developers {
                    ${before.collect { "developer { id = '$it' }" }}
                }
            }
            """.stripIndent()

        when:
        def result = gradleRunner()
                .withArguments(taskName)
                .build()

        then:
        result.task(taskPath).outcome == TaskOutcome.SUCCESS

        when:
        build.text = """\
            $MIN_BUILD_FILE
            jenkinsPlugin {
                jenkinsVersion.set('${TestSupport.RECENT_JENKINS_VERSION}')
                developers {
                    ${after.collect { "developer { id = '$it' }" }}
                }
            }
            """.stripIndent()
        def rerunResult = gradleRunner()
                .withArguments(taskName)
                .build()

        then:
        rerunResult.task(taskPath).outcome == TaskOutcome.SUCCESS

        when:
        def thirdResult = gradleRunner()
                .withArguments(taskName)
                .build()

        then:
        thirdResult.task(taskPath).outcome == TaskOutcome.UP_TO_DATE

        where:
        before     | after
        ['a']      | ['b']
        ['a', 'b'] | ['b', 'c']
    }
}
