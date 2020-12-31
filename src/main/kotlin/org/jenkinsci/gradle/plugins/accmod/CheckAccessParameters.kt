package org.jenkinsci.gradle.plugins.accmod

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.workers.WorkParameters

interface CheckAccessParameters : WorkParameters {
    val propertiesForAccessModifier: MapProperty<String, Any>
    val classpathToScan: ConfigurableFileCollection
    val dirToCheck: DirectoryProperty
    val outputFile: RegularFileProperty
}
