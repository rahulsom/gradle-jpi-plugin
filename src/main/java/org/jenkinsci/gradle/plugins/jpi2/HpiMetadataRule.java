package org.jenkinsci.gradle.plugins.jpi2;

import org.gradle.api.Action;
import org.gradle.api.artifacts.ComponentMetadataContext;
import org.gradle.api.artifacts.ComponentMetadataDetails;
import org.gradle.api.artifacts.ComponentMetadataRule;
import org.gradle.api.artifacts.MutableVariantFilesMetadata;
import org.gradle.api.artifacts.VariantMetadata;
import org.gradle.api.artifacts.maven.PomModuleDescriptor;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Usage;
import org.gradle.api.model.ObjectFactory;
import org.gradle.internal.component.model.VariantResolveMetadata;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

/**
 * Rule to make compile configurations use jar instead of hpi/jpi.
 */
@SuppressWarnings({
        "Convert2Lambda", // Gradle doesn't like lambdas
})
abstract class HpiMetadataRule implements ComponentMetadataRule {

    public static final Set<String> PLUGIN_PACKAGINGS = Set.of("hpi", "jpi");
    public static final String DEFAULT_USAGE = "default";
    public static final String DEFAULT_VARIANT = "defaultFromJpi";

    @Inject
    public HpiMetadataRule() {
    }

    @Inject
    public abstract ObjectFactory getObjectFactory();

    public interface HasVariants {
        List<VariantResolveMetadata> getVariants();
    }

    @Override
    public void execute(@NotNull ComponentMetadataContext componentMetadataContext) {
        PomModuleDescriptor pom = componentMetadataContext.getDescriptor(PomModuleDescriptor.class);
        if (pom == null) {
            return;
        }
        var details = componentMetadataContext.getDetails();
        if (PLUGIN_PACKAGINGS.contains(pom.getPackaging())) {
            details.withVariant("compile", new DefaultSelectionAction(details, "jar", getObjectFactory().named(Usage.class, Usage.JAVA_API)));
            details.withVariant("runtime", new DefaultSelectionAction(details, "jar", getObjectFactory().named(Usage.class, Usage.JAVA_API)));
        }
        details.addVariant(DEFAULT_VARIANT, "default", new DefaultSelectionAction(details, pom.getPackaging(), getObjectFactory().named(Usage.class, DEFAULT_USAGE)));
    }

    private static class DefaultSelectionAction implements Action<VariantMetadata> {
        private final ComponentMetadataDetails details;
        private final String extension;
        private final Usage usage;

        public DefaultSelectionAction(ComponentMetadataDetails details, String extension, Usage usage) {
            this.details = details;
            this.extension = extension;
            this.usage = usage;
        }

        @Override
        public void execute(@NotNull VariantMetadata variantMetadata) {
            variantMetadata.withFiles(new Action<>() {
                @Override
                public void execute(@NotNull MutableVariantFilesMetadata mutableVariantFilesMetadata) {
                    mutableVariantFilesMetadata.removeAllFiles();
                    String fileName = details.getId().getName() + "-" + details.getId().getVersion() + "." + extension;
                    mutableVariantFilesMetadata.addFile(fileName);
                }
            });
            variantMetadata.attributes(new Action<>() {
                @Override
                public void execute(@NotNull AttributeContainer attributeContainer) {
                    attributeContainer.attribute(Usage.USAGE_ATTRIBUTE, usage);
                }
            });
        }
    }
}
