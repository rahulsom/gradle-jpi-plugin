package org.jenkinsci.gradle.plugins.jpi.version

import org.eclipse.jgit.api.Git
import org.jenkinsci.gradle.plugins.jpi.IntegrationSpec
import org.jenkinsci.gradle.plugins.jpi.TestDataGenerator
import org.jenkinsci.gradle.plugins.jpi.TestSupport

import java.nio.file.Files

class GenerateGitVersionTaskSpec extends IntegrationSpec {
    private final String projectName = TestDataGenerator.generateName()
    private static final String MIN_BUILD_FILE = """\
            plugins {
                id 'org.jenkins-ci.jpi'
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

    def customBuildFile(String content = '') {
        """\
            $MIN_BUILD_FILE
            jenkinsPlugin {
                jenkinsVersion.set('${TestSupport.RECENT_JENKINS_VERSION}')
                ${content}
            }
            """.stripIndent()
    }

    def 'should generate on git repository'() {
        given:
        build.text = customBuildFile()
        initGitRepo()

        when:
        gradleRunner()
            .withArguments('generateGitVersion', '--stacktrace')
            .build()

        then:
        inProjectDir('build/generated/version/version.txt').text ==~ /1\.\w{12}/
    }

    def 'should generate jar named according to generated version'() {
        given:
        build.text = """${customBuildFile()}
            tasks.named('generateGitVersion') {
                doLast {
                    project.version = outputFile.get().getAsFile().text
                }
            }

            tasks.named('jar') {
                dependsOn(tasks.named('generateGitVersion'))
            }
        """.stripIndent()
        initGitRepo()

        when:
        gradleRunner()
            .withArguments('build', '--stacktrace')
            .build()

        then:
        inProjectDir('build/generated/version/version.txt').text ==~ /1\.\w{12}/
        def jarPattern = ~/${projectName}-1\.\w{12}\.jar/
        inProjectDir('build/libs').listFiles({ f -> jarPattern.matcher(f.name).matches() } as FileFilter).size() == 1
    }

    def 'should fail generate on non git repository'() {
        given:
        build.text = customBuildFile()

        when:
        def result = gradleRunner()
            .withArguments('generateGitVersion')
            .buildAndFail()

        then:
        result.output.contains('repository not found')
    }

    def 'should fail generate with uncommitted changes'() {
        given:
        initGitRepo()
        Files.createFile(projectDir.toPath().resolve('foo')).text = 'bar'
        build.text = customBuildFile()

        when:
        def result = gradleRunner()
            .withArguments('generateGitVersion')
            .buildAndFail()

        then:
        result.output.contains('has some pending changes')
    }

    def 'should generate with uncommitted changes but with allowDirty = true'() {
        given:
        initGitRepo()
        Files.createFile(projectDir.toPath().resolve('foo')).text = 'bar'
        build.text = customBuildFile('''
            gitVersion {
                allowDirty = true
            }
        ''')

        when:
        gradleRunner()
            .withArguments('generateGitVersion')
            .build()

        then:
        inProjectDir('build/generated/version/version.txt').text ==~ /1\.\w{12}/
    }

    def 'should generate with custom version format'() {
        given:
        build.text = customBuildFile("""
            gitVersion {
                versionFormat = 'rc-%d.%s'
                abbrevLength = 10
            }
        """)
        initGitRepo()

        when:
        gradleRunner()
            .withArguments('generateGitVersion')
            .build()

        then:
        inProjectDir('build/generated/version/version.txt').text ==~ /rc-1\.\w{10}/
    }

    def 'should generate with custom git root'() {
        def customGitRoot = Files.createTempDirectory('custom-git-root')
        given:
        build.text = customBuildFile("""
            gitVersion {
                gitRoot = file('${customGitRoot.toAbsolutePath()}')
            }
        """)
        initGitRepo(customGitRoot.toFile())

        when:
        gradleRunner()
            .withArguments('generateGitVersion')
            .build()

        then:
        inProjectDir('build/generated/version/version.txt').text ==~ /1\.\w{12}/
    }

    def initGitRepo(File gitRoot = projectDir) {
        def gitCmd = Git.init()
        gitCmd.directory = gitRoot
        Git git = gitCmd.call()
        Files.createFile(projectDir.toPath().resolve('.gitignore')).text = '.gradle'
        git.add().addFilepattern('.').call()
        def commit = git.commit()
        commit.setCommitter('Anne', 'Onyme')
        commit.sign = false
        commit.message = 'Initial commit'
        commit.call()
    }
}
