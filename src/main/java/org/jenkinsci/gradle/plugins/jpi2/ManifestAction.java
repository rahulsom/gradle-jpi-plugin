package org.jenkinsci.gradle.plugins.jpi2;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.java.archives.Manifest;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;
import org.gradle.internal.component.local.model.PublishArtifactLocalArtifactMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Action to update the JAR manifest with plugin dependencies and other attributes required in a Jenkins Plugin.
 */
class ManifestAction implements Action<Manifest> {
    private final Project project;
    private final Configuration configuration;
    private final String jenkinsVersion;

    public ManifestAction(Project project, Configuration configuration, String jenkinsVersion) {
        this.project = project;
        this.configuration = configuration;
        this.jenkinsVersion = jenkinsVersion;
    }

    @Override
    public void execute(@NotNull Manifest manifest) {
        var attributes = manifest.getAttributes();
        attributes.put("Implementation-Title", project.getGroup() + "#" + project.getName() + ";" + project.getVersion());
        attributes.put("Implementation-Version", project.getVersion());

        var pluginDependencies = new ArrayList<String>();

        configuration.getResolvedConfiguration().getFirstLevelModuleDependencies().forEach(resolvedDependency ->
                resolvedDependency.getModuleArtifacts().forEach(resolvedArtifact -> {
                    ComponentArtifactIdentifier id = resolvedArtifact.getId();
                    if (id instanceof PublishArtifactLocalArtifactMetadata) {
                        String projectPath = resolvedDependency.getModuleName();
                        var dependencyProject = project.getRootProject()
                                .getAllprojects().stream()
                                .filter(p -> p.getName().equals(projectPath))
                                .findFirst();

                        assert dependencyProject.isPresent();

                        var jpiTaskFromDependencyProject = dependencyProject.get().getTasks().findByName("jpi");
                        if (jpiTaskFromDependencyProject != null) {
                            pluginDependencies.add(resolvedDependency.getModuleName() + ":" + resolvedDependency.getModuleVersion());
                        }
                    } else if (id instanceof ModuleComponentArtifactIdentifier identifier) {
                        var byDots = identifier.getFileName().split("\\.");
                        var extension = byDots[byDots.length - 1];
                        if (HpiMetadataRule.PLUGIN_PACKAGINGS.contains(extension)) {
                            pluginDependencies.add(resolvedDependency.getModuleName() + ":" + resolvedDependency.getModuleVersion());
                        }
                    }
                }));

        if (!pluginDependencies.isEmpty()) {
            attributes.put("Plugin-Dependencies", String.join(",", pluginDependencies));
        }
        attributes.put("Plugin-Version", project.getVersion());
        attributes.put("Short-Name", project.getName());
        attributes.put("Long-Name", Optional.ofNullable(project.getDescription()).orElse(project.getName()));

        attributes.put("Jenkins-Version", jenkinsVersion);
    }
}
