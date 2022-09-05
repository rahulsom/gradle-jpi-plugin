package org.jenkinsci.gradle.plugins.jpi;

public class DependencyAnalysisResult {
    private final String manifestPluginDependencies;

    public DependencyAnalysisResult(String manifestPluginDependencies) {
        this.manifestPluginDependencies = manifestPluginDependencies;
    }

    public String getManifestPluginDependencies() {
        return manifestPluginDependencies;
    }
}
