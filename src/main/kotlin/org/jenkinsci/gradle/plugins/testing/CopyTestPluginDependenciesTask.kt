package org.jenkinsci.gradle.plugins.testing

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.mapProperty
import javax.inject.Inject

open class CopyTestPluginDependenciesTask @Inject constructor(private val fileSystemOperations: FileSystemOperations) : DefaultTask() {
    @Internal
    val files: ConfigurableFileCollection = project.objects.fileCollection()

    @InputFiles
    val plugins: FileCollection = files.filter { it.path.endsWith(".hpi") || it.path.endsWith(".jpi") }

    @Internal
    val versionlessPluginLookup: MapProperty<String, String> = project.objects.mapProperty()

    @OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @Internal
    val index: Provider<RegularFile> = outputDir.file("index")

    @TaskAction
    fun go() {
        val lookup = versionlessPluginLookup.get()
        fileSystemOperations.copy {
            from(plugins)
            into(outputDir)
            rename {
                lookup[it] ?: it
            }
        }
        index.get().asFile.bufferedWriter().use { w ->
            plugins.mapNotNull { lookup[it.name]?.substringBeforeLast(".jpi") }.forEach {
                w.write(it)
                w.write("\n")
            }
        }
    }
}
