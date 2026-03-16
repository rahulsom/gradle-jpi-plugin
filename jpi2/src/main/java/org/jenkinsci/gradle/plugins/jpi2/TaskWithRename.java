package org.jenkinsci.gradle.plugins.jpi2;

import org.gradle.api.tasks.TaskProvider;

/**
 * Associates a task provider with the rename to apply when syncing its output.
 */
class TaskWithRename {
    private final TaskProvider<?> taskProvider;
    private final DropVersionTransformer renameTransformer;

    TaskWithRename(TaskProvider<?> taskProvider, DropVersionTransformer renameTransformer) {
        this.taskProvider = taskProvider;
        this.renameTransformer = renameTransformer;
    }

    TaskProvider<?> getTaskProvider() {
        return taskProvider;
    }

    DropVersionTransformer getRenameTransformer() {
        return renameTransformer;
    }
}
