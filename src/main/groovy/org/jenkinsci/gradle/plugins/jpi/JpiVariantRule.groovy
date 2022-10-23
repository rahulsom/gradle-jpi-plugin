package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.maven.PomModuleDescriptor
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.internal.component.external.model.ivy.IvyModuleResolveMetadata
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.inject.Inject

@CacheableRule
abstract class JpiVariantRule implements ComponentMetadataRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpiVariantRule)

    public static final Attribute EMPTY_VARIANT = Attribute.of('empty-jpi', Boolean)

    private static final Attribute DESUGARED_LIBRARY_ELEMENTS_ATTRIBUTE =
            Attribute.of(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE.name, String)

    @Inject
    abstract ObjectFactory getObjects()

    @Override
    void execute(ComponentMetadataContext ctx) {
        def id = ctx.details.id
        if (id.module.toString() == JenkinsWarRule.JENKINS_WAR_COORDINATES) {
            skip(id, 'only resolved standalone')
            return
        }
        if (isMissingMetadata(ctx)) {
            skip(id, 'missing metadata')
            return
        }
        if (isIvyResolvedDependency(ctx)) {
            skip(id, 'ivy metadata')
            return
        }
        if (isJenkinsPackaging(ctx)) {
            ctx.details.withVariant('runtime') {
                it.attributes {
                    it.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements, 'jpi'))
                }
                it.withDependencies {
                    // Dependencies with a classifier point at JARs and can be removed
                    it.removeAll { it.artifactSelectors.classifier }
                }
            }
            ctx.details.withVariant('compile') {
                it.withFiles {
                    it.removeAllFiles()
                    it.addFile("${id.name}-${id.version}.jar")
                }
            }
            ctx.details.addVariant('jarRuntimeElements', 'runtime') {
                it.attributes {
                    it.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                            objects.named(LibraryElements, LibraryElements.JAR))
                }
                it.withFiles {
                    it.removeAllFiles()
                    it.addFile("${id.name}-${id.version}.jar")
                }
            }
        } else if (!hasJpiVariant(ctx)) {
            addEmptyJpiVariant(ctx)
        }
    }

    /**
     * Add a variant that we can match if a JPI variant depends on something that
     * does not have a JPI variant. This is the case for all POM-based JPI modules
     * that declare dependencies to JAR modules, because the metadata does not
     * have sufficient separation of JAR/JPI variants.
     */
    private addEmptyJpiVariant(ComponentMetadataContext ctx) {
        ctx.details.addVariant('jpiEmpty') {
            it.attributes {
                it.with {
                    attribute(EMPTY_VARIANT, Boolean.TRUE)
                    attribute(Usage.USAGE_ATTRIBUTE,
                            objects.named(Usage, Usage.JAVA_RUNTIME))
                    attribute(Usage.USAGE_ATTRIBUTE,
                            objects.named(Usage, Usage.JAVA_RUNTIME))
                    attribute(Category.CATEGORY_ATTRIBUTE,
                            objects.named(Category, Category.LIBRARY))
                    attribute(Bundling.BUNDLING_ATTRIBUTE,
                            objects.named(Bundling, Bundling.EXTERNAL))
                    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                            objects.named(LibraryElements, JpiPlugin.JPI))
                }
            }
        }
        // This variant might still resolve to a jar file - https://github.com/gradle/gradle/issues/11974
    }

    private boolean isMissingMetadata(ComponentMetadataContext ctx) {
        ctx.metadata == null
    }

    private boolean isIvyResolvedDependency(ComponentMetadataContext ctx) {
        ctx.metadata instanceof IvyModuleResolveMetadata
    }

    private boolean isJenkinsPackaging(ComponentMetadataContext ctx) {
        String packaging = ctx.getDescriptor(PomModuleDescriptor).packaging
        packaging == 'jpi' ||  packaging == 'hpi'
    }

    private boolean hasJpiVariant(ComponentMetadataContext ctx) {
        // TODO this needs public API - https://github.com/gradle/gradle/issues/12349
        ctx.metadata.variants.any {
            it.attributes.getAttribute(DESUGARED_LIBRARY_ELEMENTS_ATTRIBUTE) == JpiPlugin.JPI
        }
    }

    private static void skip(ModuleVersionIdentifier id, String reason) {
        LOGGER.debug('Skipping {} due to {}', id, reason)
    }
}
