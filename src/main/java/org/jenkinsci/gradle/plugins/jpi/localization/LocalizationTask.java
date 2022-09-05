package org.jenkinsci.gradle.plugins.jpi.localization;

import org.apache.tools.ant.util.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.apache.tools.ant.util.StringUtils.removePrefix;

public abstract class LocalizationTask extends SourceTask {
    public LocalizationTask() {
        include("**/Messages.properties");
    }

    @InputFiles
    public abstract SetProperty<File> getSourceRoots();

    @OutputDirectory
    public abstract Property<File> getOutputDir();

    @Inject
    abstract public WorkerExecutor getWorkerExecutor();

    @TaskAction
    void generate() {
        WorkQueue workQueue = getWorkerExecutor().noIsolation();

        Set<String> roots = new HashSet<>();
        for (File root : getSourceRoots().get()) {
            String absolutePath = StringUtils.removeSuffix(root.getAbsolutePath(), "/");
            roots.add(absolutePath + "/");
        }

        for (File file : getSource()) {
            String absolutePath = file.getAbsolutePath();
            String candidate = "";
            for (String root : roots) {
                if (absolutePath.startsWith(root)) {
                    candidate = removePrefix(absolutePath, root);
                    break;
                }
            }
            if (candidate.isEmpty()) {
                throw new GradleException("Could not determine relative path of " + absolutePath + " from configured roots: " + String.join(",", roots));
            }
            final String relativePath = candidate;
            workQueue.submit(RunGenerator.class, parameters -> {
                parameters.getSourceFile().set(file);
                parameters.getOutputDir().set(getOutputDir());
                parameters.getRelativePath().set(relativePath);
            });
        }
    }
}
