package org.jenkinsci.gradle.plugins.jpi2

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

/** DSL for adding licenses to the plugin POM. */
fun interface PluginLicenseSpec {
    /** Configures and adds one license. */
    fun license(action: Action<in PluginLicense>)
}

/** Instantiated through [ObjectFactory] so Gradle decorates it and Groovy closures delegate to [PluginLicense]. */
internal abstract class DefaultPluginLicenseSpec
    @Inject
    constructor(
        private val objects: ObjectFactory,
        private val licenses: ListProperty<PluginLicense>,
    ) : PluginLicenseSpec {
        override fun license(action: Action<in PluginLicense>) {
            val lic = objects.newInstance(PluginLicense::class.java)
            action.execute(lic)
            licenses.add(lic)
        }
    }