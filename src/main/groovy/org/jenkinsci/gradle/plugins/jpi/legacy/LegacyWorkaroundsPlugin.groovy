package org.jenkinsci.gradle.plugins.jpi.legacy

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.util.GradleVersion
import org.jenkinsci.gradle.plugins.jpi.JpiExtension

@CompileStatic
class LegacyWorkaroundsPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        // workarounds for JENKINS-26331
        project.tasks.named('test').configure { Task t ->
            def ext = project.extensions.getByType(JpiExtension)
            if (isBetween(ext.coreVersion, '1.545', '1.592')) {
                project.file('target').mkdirs()
            }
        }
    }

    private static boolean isBetween(String subject, String lowerBoundInclusive, String upperExclusive) {
        def current = GradleVersion.version(subject)
        def lower = GradleVersion.version(lowerBoundInclusive)
        def upper = GradleVersion.version(upperExclusive)
        current >= lower && current < upper
    }
}
