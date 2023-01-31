package org.jenkinsci.gradle.plugins.jpi.version

import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class GitVersionSpec extends Specification {

    @Shared
    Map<String, Path> gitSamples = [:]

    def setupSpec() {
        def tmp = Files.createTempDirectory('git-repos')
        ['sample-git-repo', 'sample-git-repo-collision'].each {
            def projectRoot = tmp.resolve(it)
            FileUtils.copyDirectory(sampleGit(it).toFile(), projectRoot.toFile())
            gitSamples[it] = restoreDotGit(projectRoot)
        }
    }

    def 'generate version from git abbreviated hash'() {
        given:
        def gitRoot = gitSamples['sample-git-repo']

        when:
        def version = GitVersion.builder(gitRoot).build().generate()

        then:
        version == '2.fee4afaecf76'
    }

    def 'generate version from git abbreviated hash with a custom format'() {
        given:
        def gitRoot = gitSamples['sample-git-repo']

        when:
        def version = GitVersion.builder(gitRoot).versionFormat('%d-%s').build().generate()

        then:
        version == '2-fee4afaecf76'
    }

    def 'generate version despite ambiguous commit (different depth)'() {
        given:
        def gitRoot = gitSamples['sample-git-repo']

        when:
        def version = GitVersion.builder(gitRoot).abbrevLength(1).build().generate()

        then:
        version == '2.f'
    }

    /**
     * Here's how the sample-git-repo-collision repo looks like:
     * <pre>
     *     * f3832ef (HEAD -> main) Ahead
     *     | * f84eff7 (collision) Try to collide with other commits
     *     |/
     *     fee4afa Another commit
     *     fb9d8e1 Initial commit
     * </pre>
     * Head commit f3832ef and commit f84eff7 collide (both have a depth of 3).
     */
    def 'cannot generate version because of ambiguous commit (same depth)'() {
        given:
        def gitRoot = gitSamples['sample-git-repo-collision']

        when:
        GitVersion.builder(gitRoot).abbrevLength(1).build().generate()

        then:
        def exception = thrown(RuntimeException)
        exception.message == "Found commit 'f84eff769f8009584353f35bafa4252ece54fc26' with same abbreviated hash 'f' " +
            "and depth '3' as HEAD ('f3832ef5776b5c3fb47ea7b8227f4bc6c6503e71'). " +
            'Please raise the abbreviated length to get a unique Git based version'
    }

    def 'cannot generate version from git because of untracked files'() {
        given:
        def gitRoot = generateGitRepo()
        Files.createFile(gitRoot.resolve('untracked')).text = 'bar'

        when:
        GitVersion.builder(gitRoot).abbrevLength(1).build().generate()

        then:
        def exception = thrown(RuntimeException)
        exception.message == "Repository '${gitRoot}' has some pending changes:\n- untracked files: [untracked]"
    }

    def 'cannot generate version from git because of uncommitted changes'() {
        given:
        def gitRoot = generateGitRepo()
        gitRoot.resolve('somefile').text = 'foo!'

        when:
        GitVersion.builder(gitRoot).abbrevLength(1).build().generate()

        then:
        def exception = thrown(RuntimeException)
        exception.message == "Repository '${gitRoot}' has some pending changes:\n- uncommitted changes: [somefile]"
    }

    def 'cannot generate version from git because of untracked files and uncommitted changes'() {
        given:
        def gitRoot = generateGitRepo()
        Files.createFile(gitRoot.resolve('untracked')).text = 'bar'
        gitRoot.resolve('somefile').text = 'foo!'

        when:
        GitVersion.builder(gitRoot).abbrevLength(1).build().generate()

        then:
        def exception = thrown(RuntimeException)
        exception.message == """\
            Repository '${gitRoot}' has some pending changes:
            - untracked files: [untracked]
            - uncommitted changes: [somefile]""".stripIndent()
    }

    def 'can generate version from git with pending changes'() {
        given:
        def gitRoot = generateGitRepo()
        Files.createFile(gitRoot.resolve('untracked')).text = 'bar'

        when:
        def version = GitVersion.builder(gitRoot).abbrevLength(1).allowDirty(true).build().generate()

        then:
        version =~ /1\.\w{1}/
    }

    @Ignore('enable to try locally on significant git repository, like postgres')
    def 'check on significant repo'() {
        given:
        def gitRoot = Paths.get('/tmp/postgres')

        when:
        def version = GitVersion.builder(gitRoot).abbrevLength(3).build().generate()

        then:
        version != null
    }

    Path generateGitRepo() {
        def gitRoot = Files.createTempDirectory('git-repo')
        def gitCmd = Git.init()
        gitCmd.directory = gitRoot.toFile()
        def git = gitCmd.call()
        Files.createFile(gitRoot.resolve('somefile')).text = 'foo'
        git.add().addFilepattern('somefile').call()
        def commit = git.commit()
        commit.setCommitter('Anne', 'Onyme')
        commit.sign = false
        commit.message = 'First commit'
        commit.call()
        gitRoot
    }

    Path sampleGit(String folder) {
        Paths.get(GitVersionSpec.getResource(folder).toURI())
    }

    Path restoreDotGit(Path rootProject) {
        Files.move(rootProject.resolve('dot_git'), rootProject.resolve('.git'))
        rootProject
    }

}
