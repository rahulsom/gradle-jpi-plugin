package org.jenkinsci.gradle.plugins.jpi.server

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.gradle.process.JavaExecSpec
import org.gradle.util.GradleVersion

import java.util.jar.JarFile

@CompileStatic
class JenkinsServerTask extends DefaultTask {
    public static final String TASK_NAME = 'server'
    private static final Set<String> DEFAULTED_PROPERTIES = [
            'stapler.trace',
            'stapler.jelly.noCache',
            'debug.YUI',
            'hudson.Main.development',
    ] as Set
    private final List<Action<JavaExecSpec>> execSpecActions = []

    @Classpath
    final Property<Configuration> jenkinsServerRuntime = project.objects.property(Configuration)

    @Input
    final Provider<File> jenkinsHome = project.objects.property(File)

    @Input
    @Option(option = 'port', description = 'Port to start Jenkins on (default: 8080)')
    final Property<String> port = project.objects.property(String)
            .convention('8080')

    @Input
    @Option(option = 'debug-jvm', description = 'Start Jenkins suspended and listening on debug port (default: 5005)')
    final Property<Boolean> debug = project.objects.property(Boolean)
            .convention(false)

    @Internal
    final Provider<String> extractedMainClass = jenkinsServerRuntime.map {
        def war = it.resolvedConfiguration
                .firstLevelModuleDependencies
                .find { it.moduleGroup == 'org.jenkins-ci.main' && it.moduleName == 'jenkins-war' }
                .moduleArtifacts
                .find { it.extension == 'war' }
                .file
        new JarFile(war).manifest.mainAttributes.getValue('Main-Class')
    }

    JenkinsServerTask() {
        doLast {
            project.javaexec { JavaExecSpec s ->
                s.classpath(jenkinsServerRuntime.get())
                if (GradleVersion.current() < GradleVersion.version('6.4')) {
                    s.main = extractedMainClass.get()
                } else {
                    s.mainClass.set(extractedMainClass)
                }
                s.args("--httpPort=${port.get()}")
                s.systemProperty('JENKINS_HOME', jenkinsHome.get())
                for (String prop : DEFAULTED_PROPERTIES) {
                    s.systemProperty(prop, 'true')
                }
                passThroughForBackwardsCompatibility(s)
                s.debug = debug.get()
                execSpecActions.each { a -> a.execute(s) }
            }
        }
    }

    void execSpec(Action<JavaExecSpec> action) {
        execSpecActions.add(action)
    }

    /**
     * Discovers system properties set in gradle and passes them through to the task.
     *
     * Implemented for backwards-compatibility. Will be removed in 1.0.0.
     *
     * @param spec - to be mutated
     */
    void passThroughForBackwardsCompatibility(JavaExecSpec spec) {
        boolean anyDefined = false
        for (String prop : DEFAULTED_PROPERTIES) {
            def defined = System.getProperty(prop)
            if (defined) {
                anyDefined = true
                logger.warn('Passing through system property {} to server is deprecated', prop)
                spec.systemProperty(prop, defined)
            }
        }
        if (anyDefined) {
            logger.warn('Please configure server task with system properties directly')
            logger.warn('Passing through will be removed in 1.0.0')
        }
    }
}
