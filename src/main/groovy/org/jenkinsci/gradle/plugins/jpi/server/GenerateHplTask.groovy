package org.jenkinsci.gradle.plugins.jpi.server

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jenkinsci.gradle.plugins.jpi.JpiHplManifest

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

    @InputFiles
    final ConfigurableFileCollection upstreamManifests = project.objects.fileCollection()

    @OutputFile
    final Provider<RegularFile> hpl = fileName.flatMap { String name ->
        hplDir.map { Directory d -> d.file(name) }
    }

    @TaskAction
    void generate() {
        def destination = hpl.get().asFile
        destination.parentFile.mkdirs()
        def manifest = new JpiHplManifest(project)
        upstreamManifests.each {
            it.withInputStream {
                manifest.read(it)
            }
        }
        destination.withOutputStream {
            manifest.write(it)
        }
    }
}
