package org.jenkinsci.gradle.plugins.jpi2

import org.gradle.api.Action

/** DSL for adding developers to the plugin metadata. */
fun interface PluginDeveloperSpec {
    /** Configures and adds one developer. */
    fun developer(action: Action<in PluginDeveloper>)
}