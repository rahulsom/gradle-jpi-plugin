package org.jenkinsci.gradle.plugins.jpi2;

import org.gradle.api.Action;
import org.gradle.api.artifacts.ComponentMetadataContext;
import org.gradle.api.artifacts.ComponentMetadataDetails;
import org.gradle.api.artifacts.ComponentMetadataRule;
import org.gradle.api.artifacts.MutableVariantFilesMetadata;
import org.gradle.api.artifacts.VariantMetadata;
import org.gradle.api.artifacts.maven.PomModuleDescriptor;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Set;

/**
 * Rule to make compile and runtime configurations use jar instead of hpi/jpi.
 */
@SuppressWarnings({
        "Convert2Lambda", // Gradle doesn't like lambdas
})
abstract class HpiMetadataRule implements ComponentMetadataRule {

    public static final Set<String> PLUGIN_PACKAGINGS = Set.of("hpi", "jpi");
    /**
     * Attribute to mark a component as a Jenkins plugin.
     */
    public static final Attribute<Boolean> IS_JENKINS_PLUGIN = Attribute.of("org.jenkinsci.gradle.plugins.jpi2.isJenkinsPlugin", Boolean.class);

    @Inject
    public HpiMetadataRule() {
    }

    @Override
    public void execute(@NotNull ComponentMetadataContext componentMetadataContext) {
        PomModuleDescriptor pom = componentMetadataContext.getDescriptor(PomModuleDescriptor.class);
        if (pom == null) {
            return;
        }
        var details = componentMetadataContext.getDetails();
        if (PLUGIN_PACKAGINGS.contains(pom.getPackaging())) {
            details.withVariant("compile", new DefaultSelectionAction(details, "jar", false));
            details.withVariant("runtime", new DefaultSelectionAction(details, "jar", false));
            details.withVariant("runtime", new DefaultSelectionAction(details, pom.getPackaging(), true));
        }
    }

    private static class DefaultSelectionAction implements Action<VariantMetadata> {
        private final ComponentMetadataDetails details;
        private final String extension;
        private final boolean isJenkinsPlugin;

        public DefaultSelectionAction(ComponentMetadataDetails details, String extension, boolean isJenkinsPlugin) {
            this.details = details;
            this.extension = extension;
            this.isJenkinsPlugin = isJenkinsPlugin;
        }

        @Override
        public void execute(@NotNull VariantMetadata variantMetadata) {
            variantMetadata.withFiles(new Action<>() {
                @Override
                public void execute(@NotNull MutableVariantFilesMetadata mutableVariantFilesMetadata) {
                    mutableVariantFilesMetadata.removeAllFiles();
                    mutableVariantFilesMetadata.addFile(details.getId().getName() + "-" + details.getId().getVersion() + "." + extension);
                }
            });
            variantMetadata.getAttributes().attribute(IS_JENKINS_PLUGIN, isJenkinsPlugin);
        }
    }
}
