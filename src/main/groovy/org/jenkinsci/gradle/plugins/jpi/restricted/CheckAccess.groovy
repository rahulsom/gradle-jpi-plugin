package org.jenkinsci.gradle.plugins.jpi.restricted

import org.gradle.workers.WorkAction
import org.kohsuke.accmod.impl.Checker
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class CheckAccess implements WorkAction<CheckAccessWorkParameters> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckAccess)

    @Override
    void execute() {
        List<URL> toScan = parameters.scannableClasspath.files.collect { f ->
            try {
                return f.toURI().toURL()
            } catch (MalformedURLException e) {
                LOGGER.warn('Failed to turn {} into url - will be skipped', f, e)
                return null
            }
        }.findAll { it != null }
        URL[] array = toScan.toArray(new URL[toScan.size()])
        InternalErrorListener listener = new InternalErrorListener()
        ClassLoader loader = new URLClassLoader(array, getClass().classLoader)
        Properties props = new Properties()
        for (Map.Entry<String, String> entry : parameters.accessModifierProperties.get().entrySet()) {
            props.put(entry.key, entry.value)
        }
        Checker checker = new Checker(loader, listener, props, new InternalMavenLoggingBridge())
        checker.check(parameters.compilationDir.asFile.get())
        if (listener.hasErrors()) {
            LOGGER.error(listener.errorMessage())
            throw new RestrictedApiException()
        }
    }
}
