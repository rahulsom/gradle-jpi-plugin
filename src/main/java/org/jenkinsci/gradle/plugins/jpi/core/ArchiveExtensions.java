package org.jenkinsci.gradle.plugins.jpi.core;

import org.gradle.api.artifacts.ResolvedArtifact;

import java.util.HashSet;
import java.util.Set;

public class ArchiveExtensions {
    public static final String HPI = "hpi";
    public static final String JPI = "jpi";
    private static Set<String> ALL_EXTENSIONS;
    private static Set<String> ALL_PATHS;

    private ArchiveExtensions() {
    }

    public static String nameWithJpi(ResolvedArtifact artifact) {
        return artifact.getName() + "." + JPI;
    }

    public static Set<String> allExtensions() {
        if (ALL_EXTENSIONS == null) {
            Set<String> extensions = new HashSet<>();
            extensions.add(JPI);
            extensions.add(HPI);
            ALL_EXTENSIONS = extensions;
        }
        return ALL_EXTENSIONS;
    }

    public static Set<String> allPathPatterns() {
        if (ALL_PATHS == null) {
            Set<String> paths = new HashSet<>();
            Set<String> allExtensions = allExtensions();
            for (String extension : allExtensions) {
                paths.add("*." + extension);
            }
            ALL_PATHS = paths;
        }
        return ALL_PATHS;
    }
}
