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
        val declaredJenkinsWar = target.configurations.create("declaredJenkinsWar") {
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = true
        }
        val javapoet = target.dependencies.create("com.squareup:javapoet:1.13.0")
        val jenkinsTestHarness = target.dependencies.create("org.jenkins-ci.main:jenkins-test-harness:2.71")
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
            runtimeClasspath += mainSourceSet.runtimeClasspath
        }
        val generatedJenkinsPluginsDir = target.layout.buildDirectory.dir("jpi-plugin/plugins-for-generatedJenkinsTest/test-dependencies")
        val copyPluginsForGeneratedJenkinsTest = target.tasks.register<CopyTestPluginDependenciesTask>("copyGeneratedJenkinsTestPluginDependencies") {
            group = "Verification"
            description = "Copies plugins on runtimeClasspath into directory for jenkins-test-harness to load in generatedJenkinsTest"
            files.from(project.configurations.findByName("runtimeClasspathJenkins"))
            outputDir.set(generatedJenkinsPluginsDir)
        }
        target.tasks.named(generatedSourceSet.compileJavaTaskName).configure {
            dependsOn(generateJenkinsTests)
        }
        val generatedJenkinsTest = target.tasks.register<Test>("generatedJenkinsTest") {
            inputs.files(copyPluginsForGeneratedJenkinsTest)
            group = "Verification"
            description = "Runs tests from org.jvnet.hudson.test.PluginAutomaticTestBuilder"
            testClassesDirs = generatedSourceSet.output.classesDirs
            classpath = project.files(generatedJenkinsPluginsDir.get().asFile.parentFile) + generatedSourceSet.runtimeClasspath
            // set build directory for Jenkins test harness, JENKINS-26331
            systemProperty("buildDirectory", project.layout.buildDirectory.asFile.get().absolutePath)
            systemProperty("jth.jenkins-war.path", declaredJenkinsWar.resolvedConfiguration.resolvedArtifacts.single().file.absolutePath)
        }
        target.tasks.named("check").configure {
            dependsOn(generatedJenkinsTest)
        }

        // configure test task
        val testPluginsDir = target.layout.buildDirectory.dir("jpi-plugin/plugins-for-test/test-dependencies")
        val copyPluginsForTest = target.tasks.register<CopyTestPluginDependenciesTask>("copyTestPluginDependencies") {
            group = "Verification"
            description = "Copies plugins on testRuntimeClasspath into directory for jenkins-test-harness to load in test"
            files.from(project.configurations.findByName("testRuntimeClasspathJenkins"))
            outputDir.set(testPluginsDir)
        }
        target.tasks.named<Test>("test").configure {
            inputs.files(copyPluginsForTest)
            classpath += project.files(testPluginsDir.get().asFile.parentFile)
            systemProperty("jth.jenkins-war.path", declaredJenkinsWar.resolvedConfiguration.resolvedArtifacts.single().file.absolutePath)
        }
    }
}
