package org.jenkinsci.gradle.plugins.testing

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class CopyTestPluginDependenciesTask @Inject constructor(private val fileSystemOperations: FileSystemOperations) : DefaultTask() {
    @Internal
    val files: ConfigurableFileCollection = project.objects.fileCollection()

    @InputFiles
    val plugins: FileCollection = files.filter { it.path.endsWith(".hpi") || it.path.endsWith(".jpi") }

    @OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @Internal
    val index: Provider<RegularFile> = outputDir.file("index")

    @TaskAction
    fun go() {
        fileSystemOperations.copy {
            from(plugins)
            into(outputDir)
            rename {
                val name = it.substringBeforeLast('-')
                "$name.jpi"
            }
        }
        index.get().asFile.bufferedWriter().use { w ->
            plugins.map { it.name.substringBeforeLast('-') }.forEach {
                w.write(it)
                w.newLine()
            }
        }
    }
}
