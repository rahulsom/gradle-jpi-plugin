package org.jenkinsci.gradle.plugins.jpi2;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.file.CopySpec;
import org.gradle.api.tasks.bundling.War;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Action to configure the JPI task for a Jenkins plugin.
 */
@SuppressWarnings({
        "Convert2Lambda", // Gradle doesn't like lambdas
})
class ConfigureJpiAction implements Action<War> {
    private final Project project;
    private final Configuration runtimeClasspath;
    private final String jenkinsVersion;

    public ConfigureJpiAction(Project project, Configuration runtimeClasspath, String jenkinsVersion) {
        this.project = project;
        this.runtimeClasspath = runtimeClasspath;
        this.jenkinsVersion = jenkinsVersion;
    }

    @Override
    public void execute(@NotNull War jpi) {
        jpi.getArchiveExtension().set("jpi");
        jpi.manifest(new ManifestAction(project, runtimeClasspath, jenkinsVersion));
        jpi.from(project.getTasks().named("jar"), new Action<>() {
            @Override
            public void execute(@NotNull CopySpec copySpec) {
                copySpec.into("WEB-INF/lib");
            }
        });

        var directJarDependencies = getDirectJarDependencies();
        var detachedConfiguration = project.getConfigurations().detachedConfiguration(directJarDependencies.toArray(new Dependency[0]));
        detachedConfiguration.shouldResolveConsistentlyWith(runtimeClasspath);

        jpi.setClasspath(detachedConfiguration);
        jpi.finalizedBy(V2JpiPlugin.EXPLODED_JPI_TASK);
    }

    @NotNull
    private ArrayList<Dependency> getDirectJarDependencies() {
        var directJarDependencies = new ArrayList<Dependency>();

        var requestedDependencies = runtimeClasspath.getAllDependencies();
        var resolvedDependencies = runtimeClasspath.getResolvedConfiguration().getFirstLevelModuleDependencies();

        resolvedDependencies.forEach(dependency -> {
            dependency.getModuleArtifacts().forEach(artifact -> {
                if (artifact.getExtension().equals("jar")) {
                    var requestedDependency = requestedDependencies.stream()
                            .filter(reqDep -> matches(dependency, reqDep))
                            .findFirst();

                    requestedDependency.ifPresent(directJarDependencies::add);
                }
            });
        });
        return directJarDependencies;
    }

    private boolean matches(ResolvedDependency dependency, Dependency reqDep) {
        if (reqDep instanceof ProjectDependency projectDependency) {
            var projectPath = projectDependency.getDependencyProject().getPath();
            Project dependencyProject = project.getRootProject().getChildProjects().values().stream()
                    .filter(it -> it.getPath().equals(projectPath)).findFirst()
                    .get();
            if (dependencyProject.getTasks().findByName("jpi") == null) {
                return Objects.equals(reqDep.getGroup(), dependency.getModuleGroup()) &&
                        reqDep.getName().equals(dependency.getModuleName());
            }
            return false;
        } else if (reqDep instanceof ModuleDependency moduleDependency) {
            return Objects.equals(moduleDependency.getGroup(), dependency.getModuleGroup()) &&
                    Objects.equals(moduleDependency.getName(), dependency.getModuleName());
        } else {
            return false;
        }
    }
}
