package internal

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

open class DependenciesComparisonPlugin : Plugin<Project> {
    override fun apply(p0: Project) {
        val latestTestHarness = p0.dependencies.create("org.jenkins-ci.main:jenkins-test-harness:latest.release")
        val detachedConfiguration = p0.configurations.detachedConfiguration(latestTestHarness)
        val latestTestHarnessVersionProvider = p0.provider {
            detachedConfiguration
                    .resolvedConfiguration
                    .firstLevelModuleDependencies
                    .single()
                    .moduleVersion
        }
        val file = p0.layout.buildDirectory.file("version-comparisons/test-harness.txt")
        p0.tasks.register<DependenciesComparisonTask>("compareTestHarness") {
            current.set(p0.providers.gradleProperty("deps.jenkinsTestHarness"))
            latest.set(latestTestHarnessVersionProvider)
            output.set(file)
        }
    }
}
