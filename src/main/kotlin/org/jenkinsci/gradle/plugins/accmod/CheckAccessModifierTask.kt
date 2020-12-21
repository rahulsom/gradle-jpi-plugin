package org.jenkinsci.gradle.plugins.accmod

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

/**
 * This task is modeled on org.kohsuke:access-modifier-checker
 *
 * @see org.kohsuke.accmod.impl.EnforcerMojo
 */
open class CheckAccessModifierTask @Inject constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {
    companion object {
        const val NAME = "checkAccessModifier"
        const val PREFIX = "$NAME."
    }

    @Classpath
    val accessModifierClasspath: Property<Configuration> = project.objects.property()

    @Input
    val accessModifierProperties: MapProperty<String, Any> = project.objects.mapProperty()

    @CompileClasspath
    val compileClasspath: Property<Configuration> = project.objects.property()

    @InputFiles
    val compilationDirs: ListProperty<File> = project.objects.listProperty()

    @Internal
    val presentCompilationDirs: Provider<List<File>> = compilationDirs.map { it.filter { f -> f.exists() } }

    @Internal
    val classpathToScan: Provider<FileCollection> = compileClasspath.map {
        val dependencies = it.resolvedConfiguration.resolvedArtifacts.map { a -> a.file }
        val compiled = presentCompilationDirs.get()
        project.layout.files(compiled, dependencies)
    }

    @TaskAction
    fun check() {
        val q = workerExecutor.classLoaderIsolation {
            classpath.from(accessModifierClasspath)
        }
        for (compilationDir in presentCompilationDirs.get()) {
            q.submit(CheckAccess::class) {
                getAccessModifierProperties().set(accessModifierProperties)
                getCompilationDir().set(compilationDir)
                getScannableClasspath().from(classpathToScan)
            }
        }
    }
}
