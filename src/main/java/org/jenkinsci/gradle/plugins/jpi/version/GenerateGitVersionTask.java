package org.jenkinsci.gradle.plugins.jpi.version;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;
import org.jenkinsci.gradle.plugins.jpi.GitVersionExtension;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class GenerateGitVersionTask extends DefaultTask {

    public static final String TASK_NAME = "generateGitVersion";

    private final GitVersionExtension gitVersionExtension;

    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    @OutputFile
    public RegularFileProperty getOutputFile() {
        return gitVersionExtension.getOutputFile();
    }

    @Inject
    abstract public WorkerExecutor getWorkerExecutor();

    @Inject
    abstract public ProjectLayout getProjectLayout();

    @Inject
    public GenerateGitVersionTask(GitVersionExtension gitVersionExtension) {
        this.gitVersionExtension = gitVersionExtension;
        getOutputs().doNotCacheIf("Caching would require `.git` to be an input", t -> true);
        getOutputs().upToDateWhen(t -> false);
    }

    @TaskAction
    public void generate() {
        WorkQueue queue = getWorkerExecutor().classLoaderIsolation(classLoaderWorkerSpec -> {
            classLoaderWorkerSpec.getClasspath().from(getClasspath());
        });
        queue.submit(GenerateGitVersion.class, p -> {
            p.getGitRoot().set(gitVersionExtension.getGitRoot());
            p.getAbbrevLength().set(gitVersionExtension.getAbbrevLength());
            p.getVersionPrefix().set(gitVersionExtension.getVersionPrefix());
            p.getVersionFormat().set(gitVersionExtension.getVersionFormat());
            p.getSanitize().set(gitVersionExtension.getSanitize());
            p.getAllowDirty().set(gitVersionExtension.getAllowDirty());
            p.getOutputFile().set(getOutputFile());
        });
    }

    public interface GenerateGitVersionParameters extends WorkParameters {

        DirectoryProperty getGitRoot();

        Property<String> getVersionPrefix();

        Property<String> getVersionFormat();

        Property<Boolean> getSanitize();

        Property<Boolean> getAllowDirty();

        Property<Integer> getAbbrevLength();

        RegularFileProperty getOutputFile();
    }

    public abstract static class GenerateGitVersion implements WorkAction<GenerateGitVersionParameters> {
        @Override
        public void execute() {
            GenerateGitVersionParameters p = getParameters();
            Path outputFile = p.getOutputFile().get().getAsFile().toPath();
            try {
                GitVersionGenerator.GitVersion version = new GitVersionGenerator(
                        p.getGitRoot().get().getAsFile().toPath(),
                        p.getAbbrevLength().get(),
                        p.getVersionPrefix().get(),
                        p.getVersionFormat().get(),
                        p.getAllowDirty().get(),
                        p.getSanitize().get()).generate();
                Files.write(outputFile, version.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException("Fail to write version file at " + outputFile, e);
            }
        }
    }

}
