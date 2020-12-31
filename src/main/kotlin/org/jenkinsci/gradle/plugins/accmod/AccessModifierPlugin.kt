package org.jenkinsci.gradle.plugins.accmod

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

open class AccessModifierPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply<JavaLibraryPlugin>()
            val library = dependencies.create("org.kohsuke:access-modifier-checker:1.21")
            val jenkinsAccessModifier = configurations.create("jenkinsAccessModifier") {
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                }
                isVisible = false
                isCanBeConsumed = false
                isCanBeResolved = true
                withDependencies {
                    add(library)
                }
            }

            val propertyProvider = provider(PrefixedPropertiesProvider(this, CheckAccessModifierTask.PREFIX))
            val checkAccessModifier = tasks.register<CheckAccessModifierTask>(CheckAccessModifierTask.NAME) {
                val dirs = project.extensions.getByType<SourceSetContainer>()["main"].output.classesDirs
                accessModifierClasspath.from(jenkinsAccessModifier)
                accessModifierProperties.set(propertyProvider)
                compileClasspath.from(target.configurations.getByName("compileClasspath"))
                compilationDirs.from(dirs)
                outputDirectory.set(project.layout.buildDirectory.dir("access-modifier"))
            }
            tasks.named("check").configure {
                dependsOn(checkAccessModifier)
            }
        }
    }
}
