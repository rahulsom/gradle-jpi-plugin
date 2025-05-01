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

/**
 * Rule to make compile configurations use jar instead of hpi/jpi.
 */
@SuppressWarnings({
        "Convert2Lambda", // Gradle doesn't like lambdas
})
abstract class HpiMetadataRule implements ComponentMetadataRule {
    @Inject
    public HpiMetadataRule() {
    }

    @Override
    public void execute(@NotNull ComponentMetadataContext componentMetadataContext) {
        PomModuleDescriptor pom = componentMetadataContext.getDescriptor(PomModuleDescriptor.class);
        if (pom == null) {
            return;
        }
        if ("hpi".equals(pom.getPackaging()) || "jpi".equals(pom.getPackaging())) {
            var details = componentMetadataContext.getDetails();
            details.withVariant("compile", new JarSelectionAction(details));
        }
    }

    private static class JarSelectionAction implements Action<VariantMetadata> {
        private final ComponentMetadataDetails details;

        public JarSelectionAction(ComponentMetadataDetails details) {
            this.details = details;
        }

        @Override
        public void execute(@NotNull VariantMetadata variantMetadata) {
            variantMetadata.withFiles(new Action<>() {
                @Override
                public void execute(@NotNull MutableVariantFilesMetadata mutableVariantFilesMetadata) {
                    mutableVariantFilesMetadata.removeAllFiles();
                    mutableVariantFilesMetadata.addFile(details.getId().getName() + "-" + details.getId().getVersion() + ".jar");
                }
            });
        }
    }
}
