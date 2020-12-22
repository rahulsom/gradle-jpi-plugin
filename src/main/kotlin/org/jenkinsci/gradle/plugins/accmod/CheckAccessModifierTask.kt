package org.jenkinsci.gradle.plugins.accmod

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkerExecutor
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
    val accessModifierClasspath: ConfigurableFileCollection = project.objects.fileCollection()

    @Input
    val accessModifierProperties: MapProperty<String, Any> = project.objects.mapProperty()

    @CompileClasspath
    val compileClasspath: ConfigurableFileCollection = project.objects.fileCollection()

    @InputFiles
    val compilationDirs: ConfigurableFileCollection = project.objects.fileCollection()

    @TaskAction
    fun check() {
        val q = workerExecutor.classLoaderIsolation {
            classpath.from(accessModifierClasspath)
        }
        for (compilationDir in compilationDirs) {
            q.submit(CheckAccess::class) {
                classpathToScan.from(compilationDirs, compileClasspath)
                dirToCheck.set(compilationDir)
                propertiesForAccessModifier.set(accessModifierProperties)
            }
        }
    }
}
