@file:JvmName("RepositoryShortcuts")

package org.jenkinsci.gradle.plugins.jpi2

import groovy.lang.Closure
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.ExtensionAware
import java.net.URI

internal const val JENKINS_PUBLIC_REPO_NAME = "jenkins-public"
internal val JENKINS_PUBLIC_REPO_URL: URI = URI("https://repo.jenkins-ci.org/public/")
internal const val JENKINS_INCREMENTALS_REPO_NAME = "jenkins-incrementals"
internal val JENKINS_INCREMENTALS_REPO_URL: URI = URI("https://repo.jenkins-ci.org/incrementals/")
internal const val JENKINS_SNAPSHOTS_REPO_NAME = "jenkins-snapshots"
internal val JENKINS_SNAPSHOTS_REPO_URL: URI = URI("https://repo.jenkins-ci.org/snapshots/")

fun RepositoryHandler.jenkinsPublic(): MavenArtifactRepository =
    maven {
        name = JENKINS_PUBLIC_REPO_NAME
        url = JENKINS_PUBLIC_REPO_URL
    }

fun RepositoryHandler.jenkinsIncrementals(): MavenArtifactRepository =
    maven {
        name = JENKINS_INCREMENTALS_REPO_NAME
        url = JENKINS_INCREMENTALS_REPO_URL
    }

fun RepositoryHandler.jenkinsSnapshots(): MavenArtifactRepository =
    maven {
        name = JENKINS_SNAPSHOTS_REPO_NAME
        url = JENKINS_SNAPSHOTS_REPO_URL
    }

fun registerRepositoryShortcuts(repositories: RepositoryHandler) {
    if (repositories is ExtensionAware) {
        val extensions = repositories.extensions
        if (extensions.findByName("jenkinsPublic") == null) {
            extensions.add("jenkinsPublic", object : Closure<MavenArtifactRepository>(repositories, repositories) {
                @Suppress("unused")
                fun doCall(): MavenArtifactRepository {
                    return repositories.jenkinsPublic()
                }
            })
        }
        if (extensions.findByName("jenkinsIncrementals") == null) {
            extensions.add("jenkinsIncrementals", object : Closure<MavenArtifactRepository>(repositories, repositories) {
                @Suppress("unused")
                fun doCall(): MavenArtifactRepository {
                    return repositories.jenkinsIncrementals()
                }
            })
        }
        if (extensions.findByName("jenkinsSnapshots") == null) {
            extensions.add("jenkinsSnapshots", object : Closure<MavenArtifactRepository>(repositories, repositories) {
                @Suppress("unused")
                fun doCall(): MavenArtifactRepository {
                    return repositories.jenkinsSnapshots()
                }
            })
        }
    }
}
