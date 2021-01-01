package org.jenkinsci.gradle.plugins.legacy

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

open class HudsonPluginDiscoveryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.register<DiscoverHudsonPluginsTask>(DiscoverHudsonPluginsTask.NAME) {
            group = "Build"
            description = "Finds sole hudson.Plugin implementation for Manifest"
            val dirs = project.extensions.getByType<SourceSetContainer>()["main"].output.classesDirs
            classesDirs.from(dirs)
            outputFile.set(project.layout.buildDirectory.file("hudson/plugin.mf"))
        }
    }
}
