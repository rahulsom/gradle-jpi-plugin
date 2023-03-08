package org.jenkinsci.gradle.plugins.jpi;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;

public abstract class GitVersionExtension {

    private static final int DEFAULT_ABBREV_LENGTH = 12;
    private static final String DEFAULT_VERSION_FORMAT = "%d.%s";

    @Inject
    public GitVersionExtension(ProjectLayout layout) {
        getVersionFormat().convention(DEFAULT_VERSION_FORMAT);
        getAbbrevLength().convention(DEFAULT_ABBREV_LENGTH);
        getAllowDirty().convention(false);
        getGitRoot().convention(layout.getProjectDirectory());
    }

    @Optional
    public abstract Property<String> getVersionFormat();

    @Optional
    public abstract Property<Integer> getAbbrevLength();

    @Optional
    public abstract Property<Boolean> getAllowDirty();

    @Optional
    public abstract DirectoryProperty getGitRoot();
}
