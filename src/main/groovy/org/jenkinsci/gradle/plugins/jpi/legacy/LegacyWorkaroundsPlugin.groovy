package org.jenkinsci.gradle.plugins.jpi.legacy

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Delete
import org.gradle.util.GradleVersion
import org.jenkinsci.gradle.plugins.jpi.JpiExtension

@CompileStatic
class LegacyWorkaroundsPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        // workarounds for JENKINS-26331
        project.tasks.named('test').configure { Task t ->
            t.doFirst {
                project.file('target').mkdirs()
            }.onlyIf {
                def ext = project.extensions.getByType(JpiExtension)
                def jenkinsVersion = ext.validatedJenkinsVersion
                isBetween(jenkinsVersion.get(), '1.545', '1.592')
            }
        }
        project.tasks.named('clean', Delete).configure { Delete t ->
            t.doFirst {
                t.delete('target')
            }.onlyIf {
                def ext = project.extensions.getByType(JpiExtension)
                def jenkinsVersion = ext.validatedJenkinsVersion
                isOlderThan(jenkinsVersion.get(), '1.598')
            }
        }
    }

    private static boolean isBetween(String subject, String lowerBoundInclusive, String upperExclusive) {
        def current = GradleVersion.version(subject)
        def lower = GradleVersion.version(lowerBoundInclusive)
        current >= lower && isOlderThan(subject, upperExclusive)
    }

    private static boolean isOlderThan(String subject, String upperExclusive) {
        def current = GradleVersion.version(subject)
        def upper = GradleVersion.version(upperExclusive)
        current < upper
    }
}
