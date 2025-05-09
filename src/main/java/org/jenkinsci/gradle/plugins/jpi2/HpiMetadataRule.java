package org.jenkinsci.gradle.plugins.jpi2;

import org.gradle.api.Action;
import org.gradle.api.artifacts.ComponentMetadataContext;
import org.gradle.api.artifacts.ComponentMetadataDetails;
import org.gradle.api.artifacts.ComponentMetadataRule;
import org.gradle.api.artifacts.MutableVariantFilesMetadata;
import org.gradle.api.artifacts.VariantMetadata;
import org.gradle.api.artifacts.maven.PomModuleDescriptor;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Set;

/**
 * Rule to make compile configurations use jar instead of hpi/jpi.
 */
@SuppressWarnings({
        "Convert2Lambda", // Gradle doesn't like lambdas
})
abstract class HpiMetadataRule implements ComponentMetadataRule {

    public static final Set<String> PLUGIN_PACKAGINGS = Set.of("hpi", "jpi");

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
            details.withVariant("compile", new DefaultSelectionAction(details, "jar"));
        }
    }

    private static class DefaultSelectionAction implements Action<VariantMetadata> {
        private final ComponentMetadataDetails details;
        private final String extension;

        public DefaultSelectionAction(ComponentMetadataDetails details, String extension) {
            this.details = details;
            this.extension = extension;
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
        }
    }
}
