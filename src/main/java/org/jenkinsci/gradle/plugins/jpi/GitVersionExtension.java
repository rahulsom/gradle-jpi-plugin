package org.jenkinsci.gradle.plugins.jpi;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;

public abstract class GitVersionExtension {

    private static final int DEFAULT_ABBREV_LENGTH = 12;
    private static final String DEFAULT_VERSION_FORMAT = "%d.%s";

    @Inject
    public GitVersionExtension(ProjectLayout layout, ProviderFactory providers) {
        getVersionFormat().convention(
                providers.gradleProperty("gitVersionFormat").orElse(DEFAULT_VERSION_FORMAT));
        getAbbrevLength().convention(DEFAULT_ABBREV_LENGTH);
        getAllowDirty().convention(false);
        getGitRoot().convention(layout.getProjectDirectory());
        getSanitize().convention(providers.gradleProperty("gitVersionSanitize").map(p -> true).orElse(false));
        getOutputFile().convention(
                providers.gradleProperty("gitVersionFile")
                        .map(p-> layout.getProjectDirectory().file(p))
                        .orElse(layout.getBuildDirectory().file("generated/version/version.txt")));
    }

    @Optional
    public abstract Property<String> getVersionFormat();

    @Optional
    public abstract Property<Boolean> getSanitize();

    @Optional
    public abstract Property<Integer> getAbbrevLength();

    @Optional
    public abstract Property<Boolean> getAllowDirty();

    @Optional
    public abstract DirectoryProperty getGitRoot();

    @Optional
    public abstract RegularFileProperty getOutputFile();


}
