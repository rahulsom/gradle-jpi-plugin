package org.jenkinsci.gradle.plugins.manifest

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.jenkinsci.gradle.plugins.jpi.internal.JpiExtensionBridge

open class JenkinsManifestPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val pluginClass = target.tasks.register<GeneratePluginClassManifestTask>(GeneratePluginClassManifestTask.NAME) {
            group = "Build"
            description = "Finds sole hudson.Plugin subclass for Manifest"
            val dirs = project.extensions.getByType<SourceSetContainer>()["main"].output.classesDirs
            classesDirs.from(dirs)
            outputFile.set(project.layout.buildDirectory.file("jenkins-manifests/plugin-class.mf"))
        }

        val dynamicSupport = target.tasks.register<GenerateSupportDynamicLoadingManifestTask>(GenerateSupportDynamicLoadingManifestTask.NAME) {
            group = "Build"
            description = "Aggregates dynamic loading values of @Extensions"
            val dirs = project.extensions.getByType<SourceSetContainer>()["main"].output.classesDirs
            classesDirs.from(dirs)
            outputFile.set(project.layout.buildDirectory.file("jenkins-manifests/support-dynamic-loading.mf"))
        }

        target.tasks.register<GenerateJenkinsManifestTask>(GenerateJenkinsManifestTask.NAME) {
            group = "Build"
            description = "Generate manifest for Jenkins plugin"
            upstreamManifests.from(pluginClass, dynamicSupport)
            groupId.set(project.provider { project.group as String })
            minimumJavaVersion.set(project.provider {
                project.extensions.getByType<JavaPluginExtension>().targetCompatibility.toString()
            })
            val ext = project.extensions.getByType<JpiExtensionBridge>()
            pluginId.set(ext.pluginId)
            humanReadableName.set(ext.humanReadableName)
            homePage.set(ext.homePage)
            jenkinsVersion.set(ext.jenkinsCoreVersion)
            minimumJenkinsVersion.set(ext.minimumJenkinsCoreVersion)
            sandboxed.set(ext.sandboxed)
            outputFile.set(project.layout.buildDirectory.file("jenkins-manifests/jenkins.mf"))
        }
    }
}
