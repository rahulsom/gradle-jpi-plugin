package org.jenkinsci.gradle.plugins.jpi.localization;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;

public abstract class LocalizationTask extends SourceTask {
    public LocalizationTask() {
        include("**/Messages.properties");
    }

    @OutputDirectory
    public abstract Property<File> getOutputDir();

    @Inject
    abstract public WorkerExecutor getWorkerExecutor();

    @TaskAction
    void generate() {
        WorkQueue workQueue = getWorkerExecutor().noIsolation();

        for (File file : getSource()) {
            workQueue.submit(RunGenerator.class, parameters -> {
                parameters.getSourceFile().set(file);
                parameters.getOutputDir().set(getOutputDir());
            });
        }
    }
}
