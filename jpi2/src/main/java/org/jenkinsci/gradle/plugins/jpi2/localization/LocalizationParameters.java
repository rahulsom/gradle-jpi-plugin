package org.jenkinsci.gradle.plugins.jpi2.localization;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkParameters;

/**
 * Work parameters for the localization generation task.
 */
public interface LocalizationParameters extends WorkParameters {
    /** @return the source Messages.properties file */
    RegularFileProperty getSourceFile();

    /** @return the output directory for generated files */
    DirectoryProperty getOutputDir();

    /** @return the relative path of the source file within the source root */
    Property<String> getRelativePath();
}
