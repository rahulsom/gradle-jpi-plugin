package org.jenkinsci.gradle.plugins.jpi.restricted

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.kohsuke.accmod.impl.Checker

/**
 * This task is modeled on org.kohsuke:access-modifier-checker
 *
 * @see org.kohsuke.accmod.impl.EnforcerMojo
 * @link https://github.com/jenkinsci/gradle-jpi-plugin/issues/160
 */
@CompileStatic
class CheckAccessModifierTask extends DefaultTask {
    public static final String TASK_NAME = 'checkAccessModifier'
    public static final String PROPERTY_PREFIX = TASK_NAME + '.'

    @Classpath
    final Property<Configuration> configuration = project.objects.property(Configuration)

    @InputFiles
    final ListProperty<File> compiledOutput = project.objects.listProperty(File)

    @Internal
    final Provider<List<URL>> scannable = configuration.map { Configuration config ->
        List<File> dependencies = config.resolvedConfiguration.resolvedArtifacts*.file
        List<File> compiled = compiledOutput.get()
        (compiled + dependencies).collect { it.toURI().toURL() }
    }

    @TaskAction
    void check() {
        List<URL> toScan = scannable.get()
        URL[] array = toScan.toArray(new URL[toScan.size()])
        def listener = new InternalErrorListener()
        def loader = new URLClassLoader(array, getClass().classLoader)
        def props = new Properties()
        project.properties.findAll { it.key.startsWith(PROPERTY_PREFIX) }.each {
            String tidyKey = it.key.replace(PROPERTY_PREFIX, '')
            props.put(tidyKey, it.value)
        }
        def checker = new Checker(loader, listener, props, new InternalMavenLoggingBridge())

        for (File f : compiledOutput.get()) {
            checker.check(f)
        }
        if (listener.hasErrors()) {
            logger.error(listener.errorMessage())
            throw new RestrictedApiException()
        }
    }
}
