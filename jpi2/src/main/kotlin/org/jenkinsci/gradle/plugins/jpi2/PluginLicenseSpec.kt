package org.jenkinsci.gradle.plugins.jpi2

import org.gradle.api.Action

/** DSL for adding licenses to the plugin POM. */
fun interface PluginLicenseSpec {
    /** Configures and adds one license. */
    fun license(action: Action<in PluginLicense>)
}