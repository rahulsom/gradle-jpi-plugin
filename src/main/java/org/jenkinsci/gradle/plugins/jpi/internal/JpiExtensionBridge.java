package org.jenkinsci.gradle.plugins.jpi.internal;

import org.gradle.api.provider.Property;

public interface JpiExtensionBridge {
    Property<String> getPluginId();
}
