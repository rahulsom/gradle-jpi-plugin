package org.jenkinsci.gradle.plugins.legacy

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.jar.Attributes
import java.util.jar.Attributes.Name.MANIFEST_VERSION
import java.util.jar.Manifest

open class DiscoverHudsonPluginsTask : DefaultTask() {
    companion object {
        const val NAME: String = "discoverHudsonPlugins"
        private const val FILEPATH: String = "META-INF/services/hudson.Plugin"
        private val logger: Logger = LoggerFactory.getLogger(DiscoverHudsonPluginsTask::class.java)
    }

    @InputFiles
    val classesDirs: ConfigurableFileCollection = project.objects.fileCollection()

    @OutputFile
    val outputFile: RegularFileProperty = project.objects.fileProperty()

    @TaskAction
    fun discover() {
        val implementations = classesDirs.asSequence()
                .map { File(it, FILEPATH) }
                .filter { it.exists() }
                .flatMap { it.readLines().asSequence() }
                .toList()

        if (implementations.size > 1) {
            logger.error("Must not have more than 1 legacy hudson.Plugin subclass")
            logger.error("Found {} subclasses:", implementations.size)
            for (implementation in implementations) {
                logger.error("\t- {}", implementation)
            }
            logger.error("")
            logger.error("Prefer using @Extension: https://www.jenkins.io/doc/developer/extensions")
            throw GradleException("Multiple legacy hudson.Plugin subclasses")
        } else {
            val manifest = Manifest()
            manifest.mainAttributes[MANIFEST_VERSION] = "1.0"
            if (implementations.size == 1) {
                manifest.mainAttributes.putValue("Plugin-Class", implementations.single())
            }
            outputFile.asFile.get().outputStream().use {
                manifest.write(it)
            }
        }
    }
}
