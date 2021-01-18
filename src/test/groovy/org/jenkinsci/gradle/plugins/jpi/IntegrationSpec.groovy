package org.jenkinsci.gradle.plugins.jpi

import groovy.transform.CompileStatic
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.Rule
import org.junit.experimental.categories.Category
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

@CompileStatic
@Category(UsesGradleTestKit)
class IntegrationSpec extends Specification {
    @Rule
    protected final TemporaryFolder projectDir = new TemporaryFolder()

    protected GradleRunner gradleRunner() {
        def gradleProperties = new File(projectDir.root, 'gradle.properties')
        if (!gradleProperties.exists()) {
            def props = new Properties()
            props.setProperty('org.gradle.warning.mode', 'fail')
            gradleProperties.withOutputStream {
                props.store(it, 'IntegrationSpec default generated values')
            }
        }
        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir.root)
        def gradleVersion = gradleVersionForTest
        if (gradleVersion != GradleVersion.current()) {
            return runner.withGradleVersion(gradleVersion.version)
        }
        runner
    }

    static GradleVersion getGradleVersionForTest() {
        System.getProperty('gradle.under.test')?.with { GradleVersion.version(delegate) } ?: GradleVersion.current()
    }

    static boolean isBeforeConfigurationCache() {
        gradleVersionForTest < GradleVersion.version('6.6')
    }
}
