package org.jenkinsci.gradle.plugins.jpi.core;

import org.gradle.api.Action;

/**
 * This is aligned with MavenPomDeveloperSpec, but not the same
 * because we need every property to be annotated as an Input to
 * serialize as a task input
 */
public interface PluginDeveloperSpec {
    void developer(Action<? super PluginDeveloper> action);
}
