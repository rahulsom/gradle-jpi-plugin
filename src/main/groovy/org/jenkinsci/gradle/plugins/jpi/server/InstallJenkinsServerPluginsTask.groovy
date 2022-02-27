package org.jenkinsci.gradle.plugins.jpi.server

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory

class InstallJenkinsServerPluginsTask extends DefaultTask {
    static final String TASK_NAME = 'installJenkinsServerPlugins'

    @Classpath
    final Property<Configuration> pluginsConfiguration = project.objects.property(Configuration)

    @InputFile
    final RegularFileProperty hpl = project.objects.fileProperty()

    @Input
    final Property<File> jenkinsHome = project.objects.property(File)

    @Internal
    final Provider<Map<String, String>> lookup = pluginsConfiguration.map {
        it.resolvedConfiguration
                .resolvedArtifacts
                .findAll { ['hpi', 'jpi'].contains(it.extension) }
                .collectEntries { [(it.file.name): "${it.name}.jpi"] } as Map<String, String>
    }

    @OutputDirectory
    final Provider<Directory> pluginsDir = jenkinsHome.map {
        project.layout.projectDirectory.dir("${it.path}/plugins")
    }

    InstallJenkinsServerPluginsTask() {
        doLast {
            def withoutVersion = lookup.get()
            project.sync {
                into(pluginsDir)
                from(pluginsConfiguration) {
                    include('*.hpi', '*.jpi')
                    rename {
                        withoutVersion[it as String]
                    }
                }
                from(hpl)
            }
        }
    }
}
