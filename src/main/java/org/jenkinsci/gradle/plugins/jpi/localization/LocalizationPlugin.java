package org.jenkinsci.gradle.plugins.jpi.localization;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

public class LocalizationPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        JavaPluginConvention javaConvention = target.getConvention().getPlugin(JavaPluginConvention.class);
        SourceSetContainer sourceSets = javaConvention.getSourceSets();
        SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        SourceDirectorySet mainResources = main.getResources();

        target.getTasks().register("localizeMessages", LocalizationTask.class, new Action<LocalizationTask>() {
            @Override
            public void execute(LocalizationTask t) {
                t.setSource(mainResources);
            }
        });
    }
}
