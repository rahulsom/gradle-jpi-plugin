package org.jenkinsci.gradle.plugins.manifest

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import java.net.URI
import java.util.jar.Attributes.Name.MANIFEST_VERSION
import java.util.jar.Manifest

open class GenerateJenkinsManifestTask : DefaultTask() {
    companion object {
        const val NAME: String = "generateJenkinsManifest"
    }

    @InputFiles
    val upstreamManifests: ConfigurableFileCollection = project.objects.fileCollection()

    @Input
    val groupId: Property<String> = project.objects.property()

    @Input
    val minimumJavaVersion: Property<String> = project.objects.property()

    @Input
    val pluginId: Property<String> = project.objects.property()

    @Input
    val humanReadableName: Property<String> = project.objects.property()

    @Input
    @Optional
    val homePage: Property<URI> = project.objects.property()

    @OutputFile
    val outputFile: RegularFileProperty = project.objects.fileProperty()

    @TaskAction
    fun discover() {
        val manifest = Manifest()
        manifest.mainAttributes[MANIFEST_VERSION] = "1.0"
        for (upstream in upstreamManifests) {
            upstream.inputStream().use {
                manifest.read(it)
            }
        }
        groupId.getOrElse("").apply {
            if (isNotEmpty()) {
                manifest.mainAttributes.putValue("Group-Id", groupId.get())
            }
        }
        manifest.mainAttributes.putValue("Minimum-Java-Version", minimumJavaVersion.get())
        manifest.mainAttributes.putValue("Short-Name", pluginId.get())
        manifest.mainAttributes.putValue("Extension-Name", pluginId.get())
        manifest.mainAttributes.putValue("Long-Name", humanReadableName.get())
        homePage.orNull?.apply {
            manifest.mainAttributes.putValue("Url", toASCIIString())
        }
        outputFile.asFile.get().outputStream().use {
            manifest.write(it)
        }
    }
}
