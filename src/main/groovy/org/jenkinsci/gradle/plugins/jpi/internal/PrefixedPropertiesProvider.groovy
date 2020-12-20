package org.jenkinsci.gradle.plugins.jpi.internal

import groovy.transform.CompileStatic
import org.gradle.api.Project

import java.util.concurrent.Callable

@CompileStatic
class PrefixedPropertiesProvider implements Callable<Map<String, ?>> {
    private final Project project
    private final String prefix

    PrefixedPropertiesProvider(Project project, String prefix) {
        this.project = project
        this.prefix = prefix
    }

    @Override
    Map<String, ?> call() throws Exception {
        Map<String, ?> tidy = [:]
        for (Map.Entry<String, ?> entry : project.properties) {
            if (entry.key.startsWith(prefix)) {
                String key = entry.key.replace(prefix, '')
                tidy[key] = entry.value
            }
        }
        tidy
    }
}
