package org.jenkinsci.gradle.plugins.jpi.server

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.util.jar.Manifest

@CompileStatic
class GenerateHplTask extends DefaultTask {
    static final String TASK_NAME = 'generateJenkinsServerHpl'
    @Input
    final Property<String> fileName = project.objects.property(String)

    // this approach taken from
    // https://github.com/gradle/gradle/issues/12351#issuecomment-591408300
    @Internal
    final DirectoryProperty hplDir = project.objects.directoryProperty()

    @Input
    String getHplDirPath() {
        hplDir.asFile.get().absolutePath
    }

    @Input
    final Property<File> resourcePath = project.objects.property(File)

    @Classpath
    final ConfigurableFileCollection libraries = project.objects.fileCollection()

    @InputFile
    final RegularFileProperty upstreamManifest = project.objects.fileProperty()

    @OutputFile
    final Provider<RegularFile> hpl = fileName.flatMap { String name ->
        hplDir.map { Directory d -> d.file(name) }
    }

    @TaskAction
    void generate() {
        def destination = hpl.get().asFile
        destination.parentFile.mkdirs()
        def manifest = new Manifest()
        upstreamManifest.asFile.get().withInputStream {
            manifest.read(it)
        }
        manifest.mainAttributes.putValue('Resource-Path', resourcePath.get().absolutePath)
        manifest.mainAttributes.putValue('Libraries', libraries.filter { File f -> f.exists() }.join(','))
        destination.withOutputStream {
            manifest.write(it)
        }
    }
}
