package org.jenkinsci.gradle.plugins.jpi.restricted

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.AbstractCompile
import org.kohsuke.accmod.impl.Checker

/**
 * This task is modeled on org.kohsuke:access-modifier-checker
 *
 * @see org.kohsuke.accmod.impl.EnforcerMojo
 * @link https://github.com/jenkinsci/gradle-jpi-plugin/issues/160
 */
class CheckAccessModifierTask extends DefaultTask {
    public static final TASK_NAME = 'checkAccessModifier'
    public static final PROPERTY_PREFIX = TASK_NAME + '.'

    @Classpath
    Configuration configuration

    @TaskAction
    void act() {
        Iterable<File> compiledOutput = project.tasks.withType(AbstractCompile)*.destinationDir
        Iterable<File> deps = configuration.resolvedConfiguration.resolvedArtifacts*.file
        List<URL> toScan = (compiledOutput + deps).collect { it.toURI().toURL() }
        URL[] array = toScan.toArray(new URL[toScan.size()])
        def listener = new InternalErrorListener()
        def loader = new URLClassLoader(array, getClass().classLoader)
        def props = new Properties()
        project.properties.findAll { it.key.startsWith(PROPERTY_PREFIX) }.each {
            String tidyKey = it.key.replace(PROPERTY_PREFIX, '')
            props.put(tidyKey, it.value)
        }
        def checker = new Checker(loader, listener, props, new InternalMavenLoggingBridge())

        compiledOutput.each {
            checker.check(it)
        }
        if (listener.hasErrors()) {
            logger.error(listener.errorMessage())
            throw new RestrictedApiException()
        }
    }
}
