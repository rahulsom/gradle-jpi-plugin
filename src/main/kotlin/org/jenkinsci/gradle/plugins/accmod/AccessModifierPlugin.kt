package org.jenkinsci.gradle.plugins.accmod

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

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
                dependsOn("classes")
                val dirs = tasks.withType<AbstractCompile>().map { it.destinationDir }
                accessModifierClasspath.set(jenkinsAccessModifier)
                accessModifierProperties.set(propertyProvider)
                compileClasspath.set(target.configurations.getByName("compileClasspath"))
                compilationDirs.set(dirs)
            }
            tasks.named("check").configure {
                dependsOn(checkAccessModifier)
            }
        }
    }
}
