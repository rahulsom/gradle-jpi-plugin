package org.jenkinsci.gradle.plugins.jpi.restricted

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

/**
 * This task is modeled on org.kohsuke:access-modifier-checker
 *
 * @see org.kohsuke.accmod.impl.EnforcerMojo
 */
@CompileStatic
abstract class CheckAccessModifierTask extends DefaultTask {
    public static final String TASK_NAME = 'checkAccessModifier'
    public static final String PROPERTY_PREFIX = TASK_NAME + '.'

    @Classpath
    final Property<Configuration> accessModifierClasspath = project.objects.property(Configuration)

    @CompileClasspath
    final Property<Configuration> compileClasspath = project.objects.property(Configuration)

    @InputFiles
    final ListProperty<File> compilationDirs = project.objects.listProperty(File)

    @Input
    final MapProperty<String, String> accessModifierProperties = project.objects.mapProperty(String, String)

    @Internal
    final Provider<List<File>> presentCompilationDirs = compilationDirs.map { List<File> files ->
        files.findAll { File f -> f.exists() }
    }

    @Internal
    final Provider<FileCollection> scannableClasspath = compileClasspath.map { config ->
        List<File> dependencies = config.resolvedConfiguration.resolvedArtifacts*.file
        List<File> compiled = presentCompilationDirs.get()
        project.layout.files(compiled, dependencies)
    }

    @Inject
    abstract WorkerExecutor getWorkerExecutor()

    @TaskAction
    void check() {
        WorkQueue q = workerExecutor.classLoaderIsolation {
            it.classpath.from(accessModifierClasspath.get())
        }
        for (File compilationDir : presentCompilationDirs.get()) {
            q.submit(CheckAccess) { params ->
                params.compilationDir.set(compilationDir)
                params.scannableClasspath.from(scannableClasspath)
                params.accessModifierProperties.set(accessModifierProperties)
            }
        }
    }
}
