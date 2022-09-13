package org.jenkinsci.gradle.plugins.jpi.internal;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;

import java.util.function.Function;

public class DependenciesPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        DependencyLookup lookup = new DependencyLookup();
        for (String config : lookup.configurations()) {
            project.getConfigurations().getByName(config, new Action<Configuration>() {
                @Override
                public void execute(Configuration c) {
                    c.withDependencies(new Action<DependencySet>() {
                        @Override
                        public void execute(DependencySet deps) {
                            JpiExtensionBridge ext = project.getExtensions().getByType(JpiExtensionBridge.class);
                            String jenkinsVersion = ext.getJenkinsCoreVersion().get();
                            lookup.find(c.getName(), jenkinsVersion).stream().map(new Function<String, Dependency>() {
                                @Override
                                public Dependency apply(String s) {
                                    Dependency dependency = project.getDependencies().create(s);
                                    dependency.because("Added by org.jenkins-ci.jpi plugin");
                                    return dependency;
                                }
                            }).forEach(deps::add);
                        }
                    });
                }
            });
        }
    }
}
