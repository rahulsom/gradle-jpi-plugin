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
}
