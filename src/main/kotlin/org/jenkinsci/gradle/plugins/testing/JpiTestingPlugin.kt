package org.jenkinsci.gradle.plugins.testing

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withDependencies
import org.jenkinsci.gradle.plugins.jpi.internal.JpiExtensionBridge

open class JpiTestingPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val javapoet = target.dependencies.create("com.squareup:javapoet:1.13.0")
        val jenkinsTestHarness = target.dependencies.create("org.jenkins-ci.main:jenkins-test-harness:2.60")
        val jenkinsTestGeneration = target.configurations.create("jenkinsTestGeneration") {
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, target.objects.named(Usage.JAVA_RUNTIME))
            }
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = true
            withDependencies {
                add(javapoet)
                add(jenkinsTestHarness)
            }
        }
        val generatedTestsDir = target.layout.buildDirectory.dir("inject-tests")
        val generateJenkinsTests = target.tasks.register<GenerateTestTask>("generateJenkinsTests") {
            group = "Verification"
            description = "Generates a test class that runs org.jvnet.hudson.test.PluginAutomaticTestBuilder"
            val ext = project.extensions.getByType<JpiExtensionBridge>()
            onlyIf { ext.generateTests.get() }
            generatorClasspath.setFrom(jenkinsTestGeneration)
            testName.set(ext.generatedTestClassName)
            pluginId.set(ext.pluginId)
            baseDir.set(project.projectDir)
            requireEscapeByDefaultInJelly.set(ext.requireEscapeByDefaultInJelly)
            mainResourcesOutputDir.set(project.extensions.getByType<SourceSetContainer>()["main"].output.resourcesDir)
            outputDir.set(generatedTestsDir)
        }
        val sourceSet = target.extensions.getByType<SourceSetContainer>().create("generatedJenkinsTest") {
            java {
                setSrcDirs(listOf(generatedTestsDir.get()))
            }
        }
        target.configurations.named(sourceSet.implementationConfigurationName).configure {
            withDependencies {
                add(jenkinsTestHarness)
            }
        }
        target.tasks.named(sourceSet.compileJavaTaskName).configure {
            dependsOn(generateJenkinsTests)
        }
    }
}
