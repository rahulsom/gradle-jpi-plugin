package org.jenkinsci.gradle.plugins.jpi.support;

import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Neptune {
    private static final Logger LOGGER = LoggerFactory.getLogger(Neptune.class);
    private final ProjectFile root;
    private final SettingsFile settings;
    private final Indenter indenter;

    public Neptune(ProjectFile root, SettingsFile settings, Indenter indenter) {
        this.root = root;
        this.settings = settings;
        this.indenter = indenter;
    }

    public void writeTo(TemporaryFolder directory) {
        writeTo(directory.getRoot().toPath());
    }

    public void writeTo(Path directory) {
        write(directory.resolve("build.gradle"), root.emit(indenter));
        write(directory.resolve("settings.gradle"), settings.emit(indenter));
    }

    private static void write(Path path, String contents) {
        try {
            Files.write(path, contents.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.error("Failed to write to " + path, e);
        }
    }

    public static Neptune.Builder newBuilder() {
        return new Neptune.Builder();
    }

    public static Neptune.Builder newBuilder(ProjectFile root) {
        return new Neptune.Builder()
                .withRootProject(root);
    }

    public static class Builder {
        private ProjectFile root;
        private SettingsFile settings;
        private final Indenter indenter = FourSpaceIndenter.create();

        private Builder() {
        }

        public Builder withRootProject(ProjectFile root) {
            this.root = root;
            this.settings = SettingsFile.builder()
                    .withRootProjectName(root.getName())
                    .build();
            return this;
        }

        public Neptune build() {
            return new Neptune(root, settings, indenter);
        }
    }
}
