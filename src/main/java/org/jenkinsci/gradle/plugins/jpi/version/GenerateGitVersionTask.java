package org.jenkinsci.gradle.plugins.jpi.version;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jenkinsci.gradle.plugins.jpi.GitVersionExtension;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public abstract class GenerateGitVersionTask extends DefaultTask {

    private final Directory gitRoot;
    private final String versionFormat;
    private final Boolean allowDirty;
    private final Integer abbrevLength;

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @Inject
    public GenerateGitVersionTask(GitVersionExtension gitVersionExtension, ProjectLayout layout) {
        gitRoot = gitVersionExtension.getGitRoot().get();
        versionFormat = gitVersionExtension.getVersionFormat().get();
        allowDirty = gitVersionExtension.getAllowDirty().get();
        // TODO: validate abbrevLength > 2
        abbrevLength = gitVersionExtension.getAbbrevLength().get();
        getOutputFile().convention(layout.getBuildDirectory().file("generated/version/version.txt"));
        getOutputs().doNotCacheIf("Caching would require `.git` to be an input", t -> true);
        getOutputs().upToDateWhen(t -> false);
    }

    @TaskAction
    public void generate() {
        try {
            String version = new GitVersionGenerator(gitRoot.getAsFile().toPath(),
                abbrevLength, versionFormat, allowDirty).generate();
            Files.write(getOutputFile().get().getAsFile().toPath(), version.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
