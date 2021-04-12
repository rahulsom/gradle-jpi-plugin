package internal

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion

abstract class DependenciesComparisonTask : DefaultTask() {
    @get:Input
    abstract val current: Property<String>

    @get:InputFiles
    abstract val latest: Property<String>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun compare() {
        val currentVersion = current.get()
        val latestVersion = latest.get()
        output.asFile.get().bufferedWriter().use { w ->
            w.append("current=${currentVersion}")
            w.newLine()
            w.append("latest=${latestVersion}")
            w.newLine()
        }
        if (currentVersion.toComparable() < latestVersion.toComparable()) {
            throw GradleException("Latest version is $latestVersion - using $currentVersion")
        }
    }

    private fun String.toComparable(): Int {
        return substringBefore('.').toInt()
    }
}
