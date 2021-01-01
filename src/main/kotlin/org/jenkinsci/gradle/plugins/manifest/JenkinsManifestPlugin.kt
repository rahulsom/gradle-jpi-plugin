package org.jenkinsci.gradle.plugins.manifest

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

open class JenkinsManifestPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.register<GeneratePluginClassManifestTask>(GeneratePluginClassManifestTask.NAME) {
            group = "Build"
            description = "Finds sole hudson.Plugin subclass for Manifest"
            val dirs = project.extensions.getByType<SourceSetContainer>()["main"].output.classesDirs
            classesDirs.from(dirs)
            outputFile.set(project.layout.buildDirectory.file("jenkins-manifests/plugin-class.mf"))
        }

        target.tasks.register<GenerateSupportDynamicLoadingManifestTask>(GenerateSupportDynamicLoadingManifestTask.NAME) {
            group = "Build"
            description = "Aggregates dynamic loading values of @Extensions"
            val dirs = project.extensions.getByType<SourceSetContainer>()["main"].output.classesDirs
            classesDirs.from(dirs)
            outputFile.set(project.layout.buildDirectory.file("jenkins-manifests/support-dynamic-loading.mf"))
        }
    }
}
