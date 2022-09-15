package org.jenkinsci.gradle.plugins.jpi

import hudson.util.VersionNumber
import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.model.ObjectFactory

import javax.inject.Inject

@CacheableRule
abstract class JenkinsWarRule implements ComponentMetadataRule {

    static final JENKINS_WAR_COORDINATES = 'org.jenkins-ci.main:jenkins-war'

    @Inject
    abstract ObjectFactory getObjects()

    /**
     * A Jenkins 'war' or 'war-for-test' is required on the Jenkins test classpath. This classpath expects JPI
     * variants. This rule adds such a variant to the Jenkins war module pointing at the right artifact depending
     * on the version of the module.
     */
    @Override
    void execute(ComponentMetadataContext ctx) {
        def id = ctx.details.id
        ctx.details.addVariant('jenkinsTestRuntimeElements', 'runtime') {
            it.attributes {
                it.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        objects.named(LibraryElements, JpiPlugin.JPI))
            }
            it.withDependencies {
                // Dependencies with a classifier point at JARs and can be removed
                // TODO needs public API - https://github.com/gradle/gradle/issues/11975
                it.removeAll { it.originalMetadata?.dependencyDescriptor?.dependencyArtifact?.classifier }
            }
            if (new VersionNumber(id.version).isOlderThan(new VersionNumber('2.64'))) {
                it.withFiles {
                    it.removeAllFiles()
                    it.addFile("${id.name}-${id.version}-war-for-test.jar")
                }
            }
        }
    }
}
