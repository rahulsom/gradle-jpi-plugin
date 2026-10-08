package org.jenkinsci.gradle.plugins.jpi2

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

/** DSL for adding developers to the plugin metadata. */
fun interface PluginDeveloperSpec {
    /** Configures and adds one developer. */
    fun developer(action: Action<in PluginDeveloper>)
}

/** Instantiated through [ObjectFactory] so Gradle decorates it and Groovy closures delegate to [PluginDeveloper]. */
internal abstract class DefaultPluginDeveloperSpec
    @Inject
    constructor(
        private val objects: ObjectFactory,
        private val developers: ListProperty<PluginDeveloper>,
    ) : PluginDeveloperSpec {
        override fun developer(action: Action<in PluginDeveloper>) {
            val dev = objects.newInstance(PluginDeveloper::class.java)
            action.execute(dev)
            developers.add(dev)
        }
    }