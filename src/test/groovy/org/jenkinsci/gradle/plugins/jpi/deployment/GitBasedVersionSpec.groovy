package org.jenkinsci.gradle.plugins.jpi.deployment

import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.TaskOutcome
import org.jenkinsci.gradle.plugins.jpi.IntegrationSpec
import org.jenkinsci.gradle.plugins.jpi.TestDataGenerator
import org.jenkinsci.gradle.plugins.jpi.TestSupport

import java.nio.file.Files

class GitBasedVersionSpec extends IntegrationSpec {
    private final String projectName = TestDataGenerator.generateName()
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
        File settings = touchInProjectDir('settings.gradle')
        settings << """rootProject.name = \"$projectName\""""
        build = touchInProjectDir('build.gradle')
        def props = new Properties()
        props.setProperty('version', '1.0.0')
        inProjectDir('gradle.properties').withOutputStream {
            props.store(it, 'generated')
        }
    }

    def 'should fail generate on non git repository'() {
        given:
        build.text = BUILD_FILE

        when:
        def result = gradleRunner()
            .withArguments('build', '-PgitBasedVersioning')
            .buildAndFail()

        then:
        result.output.contains('repository not found')
    }

    def 'should fail generate on git repository with uncommitted changes'() {
        given:
        initGitRepo()
        build.text = BUILD_FILE

        when:
        def result = gradleRunner()
            .withArguments('build', '-PgitBasedVersioning')
            .buildAndFail()

        then:
        result.output.contains('has some pending changes')
    }

    def 'should set git based version when gitBasedVersioning is set'() {
        given:
        initGitRepo()
        build.text = BUILD_FILE

        when:
        def result = gradleRunner()
            .withArguments('build', '-PgitBasedVersioning', '-Pgit.allowDirty')
            .build()

        then:
        result.task(':build').outcome == TaskOutcome.SUCCESS
        def jarPattern = ~/${projectName}-1\.\w{12}\.jar/
        inProjectDir('build/libs').listFiles({ f -> jarPattern.matcher(f.name).matches() } as FileFilter).size() == 1
    }

    def 'specified version overrides the git based one'() {
        given:
        initGitRepo()
        build.text = """${BUILD_FILE}
            version = '1.0.1'
        """.stripIndent()

        when:
        def result = gradleRunner()
            .withArguments('build', '-PgitBasedVersioning', '-Pgit.allowDirty')
            .build()

        then:
        result.task(':build').outcome == TaskOutcome.SUCCESS
        existsRelativeToProjectDir("build/libs/${projectName}-1.0.1.jar")
    }

    def initGitRepo() {
        def gitCmd = Git.init()
        gitCmd.directory = projectDir
        Git git = gitCmd.call()
        Files.createFile(projectDir.toPath().resolve('somefile')).text = 'foo'
        git.add().addFilepattern('somefile').call()
        def commit = git.commit()
        commit.setCommitter('Anne', 'Onyme')
        commit.sign = false
        commit.message = 'Commit only a well known file'
        commit.call()
    }
}
