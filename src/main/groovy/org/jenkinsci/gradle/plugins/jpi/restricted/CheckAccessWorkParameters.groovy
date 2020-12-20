package org.jenkinsci.gradle.plugins.jpi.restricted

import groovy.transform.CompileStatic
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.workers.WorkParameters

@CompileStatic
interface CheckAccessWorkParameters extends WorkParameters {
    DirectoryProperty getCompilationDir()
    ConfigurableFileCollection getScannableClasspath()
    MapProperty<String, String> getAccessModifierProperties()
}
