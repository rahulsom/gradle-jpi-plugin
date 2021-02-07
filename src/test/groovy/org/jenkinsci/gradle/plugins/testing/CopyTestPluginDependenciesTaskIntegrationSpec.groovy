package org.jenkinsci.gradle.plugins.testing

import groovy.text.SimpleTemplateEngine
import org.gradle.testkit.runner.TaskOutcome
import org.jenkinsci.gradle.plugins.jpi.IntegrationSpec
import org.jenkinsci.gradle.plugins.jpi.TestDataGenerator
import org.jenkinsci.gradle.plugins.jpi.TestSupport
import spock.lang.IgnoreIf
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import static org.jenkinsci.gradle.plugins.jpi.TestSupport.ANT_1_10
import static org.jenkinsci.gradle.plugins.jpi.TestSupport.ANT_1_11
import static org.jenkinsci.gradle.plugins.jpi.TestSupport.LOG4J_API_2_13_0
import static org.jenkinsci.gradle.plugins.jpi.TestSupport.LOG4J_API_2_14_0

class CopyTestPluginDependenciesTaskIntegrationSpec extends IntegrationSpec {
    private final String projectName = TestDataGenerator.generateName()
    private final String taskPath = ':copyTestPluginDependencies'
    private File build
    private final engine = new SimpleTemplateEngine()
    @SuppressWarnings('GStringExpressionWithinString')
    private final buildTemplate = '''\
        plugins {
            id 'org.jenkins-ci.jpi'
        }
        jenkinsPlugin {
            jenkinsVersion = '$jenkinsVersion'
        }
        dependencies {
        <% for (dep in dependencies) { %>
            ${dep.configuration} '${dep.coordinate}'
        <% } %>
        }
        '''.stripIndent()
    private final template = engine.createTemplate(buildTemplate)

    def setup() {
        File settings = projectDir.newFile('settings.gradle')
        settings << """rootProject.name = \"$projectName\""""
        build = projectDir.newFile('build.gradle')
    }

    @Unroll
    @SuppressWarnings('ParameterCount')
    def 'should rerun #rerunOutcome when #configuration dependencies #change'(String configuration,
                                                                              List<String> before,
                                                                              List<String> after,
                                                                              String change,
                                                                              TaskOutcome rerunOutcome,
                                                                              List<String> lines) {
        given:
        def made = template.make([
                'jenkinsVersion': TestSupport.RECENT_JENKINS_VERSION,
                'dependencies'  : before.collect { ['configuration': configuration, 'coordinate': it] },
        ])
        build.withWriter { made.writeTo(it) }

        when:
        def result = gradleRunner()
                .withArguments(taskPath, '-s')
                .build()

        then:
        result.task(taskPath).outcome == SUCCESS

        when:
        def remade = template.make([
                'jenkinsVersion': TestSupport.RECENT_JENKINS_VERSION,
                'dependencies'  : after.collect { ['configuration': configuration, 'coordinate': it] },
        ])
        build.withWriter { remade.writeTo(it) }
        def rerunResult = gradleRunner()
                .withArguments(taskPath, '-s')
                .build()

        then:
        rerunResult.task(taskPath).outcome == rerunOutcome
        def actual = new File(projectDir.root, 'build/jpi-plugin/plugins-for-test/test-dependencies/index')
        if (actual.exists()) {
            actual.readLines().toSorted() == lines.toSorted()
            actual.eachLine {
                assert new File(projectDir.root, "build/jpi-plugin/plugins-for-test/test-dependencies/${it}.jpi")
            }
        }

        where:
        configuration        | before             | after              | change            | rerunOutcome | lines
        'compileOnly'        | []                 | [LOG4J_API_2_13_0] | 'added library'   | UP_TO_DATE   | ['ui-samples-plugin']
        'compileOnly'        | [LOG4J_API_2_13_0] | [LOG4J_API_2_14_0] | 'changed library' | UP_TO_DATE   | ['ui-samples-plugin']
        'compileOnly'        | [LOG4J_API_2_13_0] | []                 | 'removed library' | UP_TO_DATE   | ['ui-samples-plugin']

        'compileOnly'        | []                 | [ANT_1_11]         | 'added plugin'    | UP_TO_DATE   | ['ui-samples-plugin']
        'compileOnly'        | [ANT_1_10]         | [ANT_1_11]         | 'changed plugin'  | UP_TO_DATE   | ['ui-samples-plugin']
        'compileOnly'        | [ANT_1_10]         | []                 | 'removed plugin'  | UP_TO_DATE   | ['ui-samples-plugin']

        'implementation'     | []                 | [LOG4J_API_2_13_0] | 'added library'   | UP_TO_DATE   | ['ui-samples-plugin']
        'implementation'     | [LOG4J_API_2_13_0] | [LOG4J_API_2_14_0] | 'changed library' | UP_TO_DATE   | ['ui-samples-plugin']
        'implementation'     | [LOG4J_API_2_13_0] | []                 | 'removed library' | UP_TO_DATE   | ['ui-samples-plugin']

        'implementation'     | []                 | [ANT_1_11]         | 'added plugin'    | SUCCESS      | ['ant', 'ui-samples-plugin', 'structs']
        'implementation'     | [ANT_1_10]         | [ANT_1_11]         | 'changed plugin'  | SUCCESS      | ['ant', 'ui-samples-plugin', 'structs']
        'implementation'     | [ANT_1_10]         | []                 | 'removed plugin'  | SUCCESS      | ['ui-samples-plugin']

        'runtimeOnly'        | []                 | [LOG4J_API_2_13_0] | 'added library'   | UP_TO_DATE   | ['ui-samples-plugin']
        'runtimeOnly'        | [LOG4J_API_2_13_0] | [LOG4J_API_2_14_0] | 'changed library' | UP_TO_DATE   | ['ui-samples-plugin']
        'runtimeOnly'        | [LOG4J_API_2_13_0] | []                 | 'removed library' | UP_TO_DATE   | ['ui-samples-plugin']

        'runtimeOnly'        | []                 | [ANT_1_11]         | 'added plugin'    | SUCCESS      | ['ant', 'ui-samples-plugin', 'structs']
        'runtimeOnly'        | [ANT_1_10]         | [ANT_1_11]         | 'changed plugin'  | SUCCESS      | ['ant', 'ui-samples-plugin', 'structs']
        'runtimeOnly'        | [ANT_1_10]         | []                 | 'removed plugin'  | SUCCESS      | ['ui-samples-plugin']

        'testCompileOnly'    | []                 | [LOG4J_API_2_13_0] | 'added library'   | UP_TO_DATE   | ['ui-samples-plugin']
        'testCompileOnly'    | [LOG4J_API_2_13_0] | [LOG4J_API_2_14_0] | 'changed library' | UP_TO_DATE   | ['ui-samples-plugin']
        'testCompileOnly'    | [LOG4J_API_2_13_0] | []                 | 'removed library' | UP_TO_DATE   | ['ui-samples-plugin']

        'testCompileOnly'    | []                 | [ANT_1_11]         | 'added plugin'    | UP_TO_DATE   | ['ui-samples-plugin']
        'testCompileOnly'    | [ANT_1_10]         | [ANT_1_11]         | 'changed plugin'  | UP_TO_DATE   | ['ui-samples-plugin']
        'testCompileOnly'    | [ANT_1_10]         | []                 | 'removed plugin'  | UP_TO_DATE   | ['ui-samples-plugin']

        'testImplementation' | []                 | [LOG4J_API_2_13_0] | 'added library'   | UP_TO_DATE   | ['ui-samples-plugin']
        'testImplementation' | [LOG4J_API_2_13_0] | [LOG4J_API_2_14_0] | 'changed library' | UP_TO_DATE   | ['ui-samples-plugin']
        'testImplementation' | [LOG4J_API_2_13_0] | []                 | 'removed library' | UP_TO_DATE   | ['ui-samples-plugin']

        'testImplementation' | []                 | [ANT_1_11]         | 'added plugin'    | SUCCESS      | ['ant', 'ui-samples-plugin', 'structs']
        'testImplementation' | [ANT_1_10]         | [ANT_1_11]         | 'changed plugin'  | SUCCESS      | ['ant', 'ui-samples-plugin', 'structs']
        'testImplementation' | [ANT_1_10]         | []                 | 'removed plugin'  | SUCCESS      | ['ui-samples-plugin']

        'testRuntimeOnly'    | []                 | [LOG4J_API_2_13_0] | 'added library'   | UP_TO_DATE   | ['ui-samples-plugin']
        'testRuntimeOnly'    | [LOG4J_API_2_13_0] | [LOG4J_API_2_14_0] | 'changed library' | UP_TO_DATE   | ['ui-samples-plugin']
        'testRuntimeOnly'    | [LOG4J_API_2_13_0] | []                 | 'removed library' | UP_TO_DATE   | ['ui-samples-plugin']

        'testRuntimeOnly'    | []                 | [ANT_1_11]         | 'added plugin'    | SUCCESS      | ['ant', 'ui-samples-plugin', 'structs']
        'testRuntimeOnly'    | [ANT_1_10]         | [ANT_1_11]         | 'changed plugin'  | SUCCESS      | ['ant', 'ui-samples-plugin', 'structs']
        'testRuntimeOnly'    | [ANT_1_10]         | []                 | 'removed plugin'  | SUCCESS      | ['ui-samples-plugin']
    }

    @IgnoreIf({ isBeforeConfigurationCache() })
    def 'should work with configuration cache'() {
        given:
        def made = template.make([
                'jenkinsVersion': TestSupport.RECENT_JENKINS_VERSION,
                'dependencies'  : [
                        ['configuration': 'implementation', 'coordinate': ANT_1_11]
                ],
        ])
        build.withWriter { made.writeTo(it) }

        when:
        def result = gradleRunner()
                .withArguments(taskPath, '--configuration-cache')
                .build()

        then:
        result.task(taskPath).outcome == SUCCESS
    }
}
