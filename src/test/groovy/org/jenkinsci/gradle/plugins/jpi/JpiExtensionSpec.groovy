package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class JpiExtensionSpec extends Specification {
    def 'work directory defaults to work if not set'() {
        when:
        Project project = ProjectBuilder.builder().build()
        JpiExtension jpiExtension = new JpiExtension(project)
        jpiExtension.workDir = null

        then:
        jpiExtension.workDir == new File(project.projectDir, 'work')
    }

    def 'work directory defaults to work in child project the extension is applied to if not set'() {
        when:
        Project parent = ProjectBuilder.builder().build()
        Project project = ProjectBuilder.builder().withParent(parent).build()
        JpiExtension jpiExtension = new JpiExtension(project)
        jpiExtension.workDir = null

        then:
        jpiExtension.workDir == new File(project.projectDir, 'work')
    }

    def 'work directory is used when set'() {
        when:
        Project project = ProjectBuilder.builder().build()
        JpiExtension jpiExtension = new JpiExtension(project)
        File dir = new File('/tmp/foo')
        jpiExtension.workDir = dir

        then:
        jpiExtension.workDir == dir
    }
}
