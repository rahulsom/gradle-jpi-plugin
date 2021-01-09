package org.jenkinsci.gradle.plugins.jpi.internal;

import org.gradle.api.provider.Property;

import java.net.URI;

public interface JpiExtensionBridge {
    Property<String> getPluginId();
    Property<String> getHumanReadableName();
    Property<URI> getHomePage();
}
