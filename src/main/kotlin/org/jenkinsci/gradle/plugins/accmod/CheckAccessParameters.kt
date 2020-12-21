package org.jenkinsci.gradle.plugins.accmod

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.workers.WorkParameters

interface CheckAccessParameters : WorkParameters {
    fun getCompilationDir(): DirectoryProperty
    fun getScannableClasspath(): ConfigurableFileCollection
    fun getAccessModifierProperties(): MapProperty<String, Any>
}
