package org.jenkinsci.gradle.plugins.jpi

import org.gradle.testkit.runner.TaskOutcome
import spock.lang.IgnoreIf
import spock.lang.Unroll

class JpiLocalizerTaskIntegrationSpec extends IntegrationSpec {

    @Unroll
    def 'single-module project should be able to run LocalizerTask (#dir)'(String dir, String expected) {
        given:
        projectDir.newFile('build.gradle') << """\
            plugins {
                id 'org.jenkins-ci.jpi'
            }
            jenkinsPlugin {
                localizerOutputDir = $dir
                jenkinsVersion = '${TestSupport.RECENT_JENKINS_VERSION}'
            }
            """.stripIndent()
        mkDirInProjectDir('src/main/resources/org/example')
        projectDir.newFile('src/main/resources/org/example/Messages.properties') << 'key1=value1\nkey2=value2'
        projectDir.newFile('src/main/resources/Messages.properties') << 'key3=value1\nkey4=value2'

        when:
        def result = gradleRunner()
                .withArguments('localizer')
                .build()

        then:
        result.task(':localizer').outcome == TaskOutcome.SUCCESS
        def orgExampleMessagesPath = "$expected/org/example/Messages.java"
        def orgExampleMessages = new File(projectDir.root, orgExampleMessagesPath)
        existsRelativeToProjectDir(orgExampleMessagesPath)
        orgExampleMessages.text.contains('public static String key1()')
        orgExampleMessages.text.contains('public static String key2()')
        def messagesPath = "$expected/Messages.java"
        def messages = new File(projectDir.root, messagesPath)
        existsRelativeToProjectDir(messagesPath)
        messages.text.contains('public static String key3()')
        messages.text.contains('public static String key4()')

        where:
        dir     | expected
        null    | 'build/generated-src/localizer'
        "''"    | 'build/generated-src/localizer'
        "'foo'" | 'foo'
    }

    @IgnoreIf({ isBeforeConfigurationCache() })
    def 'should support configuration cache'() {
        given:
        projectDir.newFile('build.gradle') << """\
            plugins {
                id 'org.jenkins-ci.jpi'
            }
            jenkinsPlugin {
                jenkinsVersion = '${TestSupport.RECENT_JENKINS_VERSION}'
            }
            """.stripIndent()
        mkDirInProjectDir('src/main/resources/org/example')
        projectDir.newFile('src/main/resources/org/example/Messages.properties') << 'key1=value1\nkey2=value2'

        when:
        def result = gradleRunner()
                .withArguments('--configuration-cache', 'localizer', '-i')
                .build()

        then:
        result.task(':localizer').outcome == TaskOutcome.SUCCESS
    }

    def 'multi-module project should be able to run LocalizerTask'() {
        given:
        projectDir.newFile('build.gradle') << ''
        projectDir.newFile('settings.gradle') << 'include ":plugin"'
        mkDirInProjectDir('plugin/src/main/resources')
        projectDir.newFile('plugin/build.gradle') << """\
            plugins { id 'org.jenkins-ci.jpi' }
            jenkinsPlugin {
                jenkinsVersion = '${TestSupport.RECENT_JENKINS_VERSION}'
            }
            """.stripIndent()
        projectDir.newFile('plugin/src/main/resources/Messages.properties') << 'key3=value3\nkey4=value4'

        when:
        def result = gradleRunner()
                .withArguments(':plugin:localizer')
                .build()

        then:
        result.task(':plugin:localizer').outcome == TaskOutcome.SUCCESS
        def generatedJavaFile = new File(projectDir.root, 'plugin/build/generated-src/localizer/Messages.java')
        generatedJavaFile.exists()
        generatedJavaFile.text.contains('public static String key3()')
        generatedJavaFile.text.contains('public static String key4()')
    }
}
