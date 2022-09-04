package org.jenkinsci.gradle.plugins.jpi.localization;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;

public class LocalizationPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        JavaPluginConvention javaConvention = target.getConvention().getPlugin(JavaPluginConvention.class);
        SourceSetContainer sourceSets = javaConvention.getSourceSets();
        SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        SourceDirectorySet mainResources = main.getResources();
        TaskContainer tasks = target.getTasks();
        
        TaskProvider<LocalizationTask> localizeMessages = tasks.register("localizeMessages", 
                LocalizationTask.class, 
                new RegistrationActions(mainResources));

        tasks.named(main.getCompileJavaTaskName(), JavaCompile.class)
                .configure(new IncludeAdditionalSource(localizeMessages));
    }
    
    private static class RegistrationActions implements Action<LocalizationTask> {
        private static final String DESCRIPTION = "Generates Java source files for **/Messages.properties";
        private final SourceDirectorySet source;

        private RegistrationActions(SourceDirectorySet source) {
            this.source = source;
        }

        @Override
        public void execute(LocalizationTask t) {
            t.setDescription(DESCRIPTION);
            t.setGroup(BasePlugin.BUILD_GROUP);
            t.setSource(source);
        }
    }
    
    private static class IncludeAdditionalSource implements Action<JavaCompile> {
        private final TaskProvider<?> provider;

        private IncludeAdditionalSource(TaskProvider<?> provider) {
            this.provider = provider;
        }

        @Override
        public void execute(JavaCompile javaCompile) {
            javaCompile.source(provider);
        }
    }
}
