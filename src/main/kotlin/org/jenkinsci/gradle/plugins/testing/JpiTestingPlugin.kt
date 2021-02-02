package org.jenkinsci.gradle.plugins.testing

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jenkinsci.gradle.plugins.jpi.internal.JpiExtensionBridge
import java.io.File

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
        val mainSourceSet = target.extensions.getByType<SourceSetContainer>()["main"]
        val generatedSourceSet = target.extensions.getByType<SourceSetContainer>().create("generatedJenkinsTest") {
            java {
                setSrcDirs(listOf(generatedTestsDir.get()))
            }
            resources {
                setSrcDirs(listOf<File>())
            }
            runtimeClasspath += mainSourceSet.output
        }
        target.tasks.named(generatedSourceSet.compileJavaTaskName).configure {
            dependsOn(generateJenkinsTests)
        }
        val testTask = target.tasks.register<Test>("generatedJenkinsTest") {
            group = "Verification"
            description = "Runs tests from org.jvnet.hudson.test.PluginAutomaticTestBuilder"
            testClassesDirs = generatedSourceSet.output.classesDirs
            classpath = generatedSourceSet.runtimeClasspath
            // set build directory for Jenkins test harness, JENKINS-26331
            systemProperty("buildDirectory", project.layout.buildDirectory.asFile.get().absolutePath)
        }
        target.tasks.named("check").configure {
            dependsOn(testTask)
        }
    }
}
