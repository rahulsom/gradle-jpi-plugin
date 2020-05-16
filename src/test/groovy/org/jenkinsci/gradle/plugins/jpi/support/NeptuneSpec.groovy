package org.jenkinsci.gradle.plugins.jpi.support

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class NeptuneSpec extends Specification {
    @Rule
    protected final TemporaryFolder projectDir = new TemporaryFolder()

    def 'should create build and settings'() {
        given:
        def neptune = Neptune.newBuilder()
                .withRootProject(ProjectFile.newBuilder()
                        .withName('avacado')
                        .build())
                .build()

        when:
        neptune.writeTo(projectDir)

        then:
        new File(projectDir.root, 'build.gradle').exists()
        new File(projectDir.root, 'settings.gradle').exists()
    }

    def 'should create subprojects'() {
        given:
        def neptune = Neptune.newBuilder()
                .withRootProject(ProjectFile.newBuilder().build())
                .addSubproject(ProjectFile.newBuilder().withName('a').build())
                .addSubproject(ProjectFile.newBuilder().withName('b').build())
                .build()

        when:
        neptune.writeTo(projectDir)

        then:
        new File(projectDir.root, 'build.gradle').exists()
        new File(projectDir.root, 'a/build.gradle').exists()
        new File(projectDir.root, 'b/build.gradle').exists()
        new File(projectDir.root, 'settings.gradle').text == '''\
            include 'a'
            include 'b'
            '''.stripIndent()
    }
}
