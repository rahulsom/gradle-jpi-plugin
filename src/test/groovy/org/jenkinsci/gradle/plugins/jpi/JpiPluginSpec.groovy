package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class JpiPluginSpec extends Specification {
    Project project = ProjectBuilder.builder().build()

    def 'test plugin id'(String pluginId) {
        when:
        project.apply plugin: pluginId

        then:
        project.plugins[pluginId] instanceof JpiPlugin

        where:
        pluginId << ['jpi', 'org.jenkins-ci.jpi']
    }

    def 'tasks exist'(String taskName, Class taskClass) {
        when:
        project.apply plugin: 'jpi'

        then:
        taskClass.isInstance(project.tasks[taskName])

        where:
        taskName     | taskClass
        'jpi'        | Jpi
        'server'     | ServerTask
        'localizer'  | LocalizerTask
        'insertTest' | TestInsertionTask
    }

    def 'publishing has been setup'(String projectVersion, String repositoryUrl, String extension) {
        when:
        project.with {
            apply plugin: 'jpi'
            group = 'org.example'
            version = projectVersion
            jenkinsPlugin {
                shortName = 'foo'
                fileExtension = extension
            }
        }
        (project as ProjectInternal).evaluate()

        then:
        PublishingExtension publishingExtension = project.extensions.getByType(PublishingExtension)
        publishingExtension.publications.size() == 1
        publishingExtension.publications.getByName('mavenJpi') instanceof MavenPublication
        MavenPublication mavenPublication = publishingExtension.publications.getByName('mavenJpi') as MavenPublication
        mavenPublication.groupId == 'org.example'
        mavenPublication.artifactId == 'foo'
        mavenPublication.version == projectVersion
        mavenPublication.pom.packaging == extension
        mavenPublication.artifacts.size() == 4
        publishingExtension.repositories.size() == 1
        publishingExtension.repositories.get(0).name == 'jenkins'
        publishingExtension.repositories.get(0) instanceof MavenArtifactRepository
        (publishingExtension.repositories.get(0) as MavenArtifactRepository).url == URI.create(repositoryUrl)
        project.components.size() == 3

        where:
        projectVersion   | repositoryUrl                           | extension
        '1.0.0'          | 'https://repo.jenkins-ci.org/releases'  | 'jpi'
        '1.0.0-SNAPSHOT' | 'https://repo.jenkins-ci.org/snapshots' | 'jpi'
        '1.0.0'          | 'https://repo.jenkins-ci.org/releases'  | 'hpi'
        '1.0.0-SNAPSHOT' | 'https://repo.jenkins-ci.org/snapshots' | 'hpi'
    }

    def 'jpi task has been setup'() {
        when:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                shortName = name
                fileExtension = extension
            }
        }
        (project as ProjectInternal).evaluate()

        then:
        Jpi task = project.tasks[Jpi.TASK_NAME] as Jpi
        task != null
        task.description != null
        task.group == BasePlugin.BUILD_GROUP
        task.archiveName == archiveName
        task.extension == extension

        where:
        name  | extension || archiveName
        ''    | 'jpi'     || 'test.jpi'
        null  | 'jpi'     || 'test.jpi'
        'foo' | 'jpi'     || 'foo.jpi'
        ''    | 'hpi'     || 'test.hpi'
        null  | 'hpi'     || 'test.hpi'
        'foo' | 'hpi'     || 'foo.hpi'
    }

    def 'publishing configuration has been skipped'() {
        when:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                configurePublishing = false
            }
        }
        (project as ProjectInternal).evaluate()

        then:
        PublishingExtension publishingExtension = project.extensions.getByType(PublishingExtension)
        publishingExtension.publications.size() == 0
        publishingExtension.repositories.size() == 0
        project.components.size() == 3
    }

    def 'localizer task has been setup'(Object outputDir, String expectedOutputDir) {
        when:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                localizerOutputDir = outputDir
            }
        }

        then:
        JavaPluginConvention javaConvention = project.convention.getPlugin(JavaPluginConvention)
        LocalizerTask localizerTask = project.tasks.findByName('localizer') as LocalizerTask
        localizerTask != null
        localizerTask.destinationDir == new File(project.rootDir, expectedOutputDir)
        localizerTask.sourceDirs == javaConvention.sourceSets.main.resources.srcDirs
        project.tasks.compileJava.dependsOn.contains(localizerTask)
        javaConvention.sourceSets.main.java.srcDirs.contains(localizerTask.destinationDir)

        where:
        outputDir       | expectedOutputDir
        null            | 'build/generated-src/localizer'
        'foo'           | 'foo'
        new File('bar') | 'bar'
    }

    def 'repositories have been setup'() {
        when:
        project.with {
            apply plugin: 'jpi'
        }
        (project as ProjectInternal).evaluate()

        then:
        project.repositories.size() == 3
        project.repositories.get(0) instanceof MavenArtifactRepository
        project.repositories.get(0).name == 'MavenRepo'
        project.repositories.get(1) instanceof MavenArtifactRepository
        project.repositories.get(1).name == 'MavenLocal'
        project.repositories.get(2) instanceof MavenArtifactRepository
        project.repositories.get(2).name == 'jenkins'
        (project.repositories.get(2) as MavenArtifactRepository).url ==
                URI.create('https://repo.jenkins-ci.org/public/')
    }

    def 'repositories have not been setup'() {
        when:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                configureRepositories = false
            }
        }
        (project as ProjectInternal).evaluate()

        then:
        project.repositories.size() == 0
    }

    def 'testCompile configuration contains plugin JAR dependencies'() {
        setup:
        Project project = ProjectBuilder.builder().build()

        when:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                coreVersion = '1.554.2'
            }
        }
        (project as ProjectInternal).evaluate()

        then:
        def dependencies = collectDependencies(project, 'testCompile')
        'org.jenkins-ci.main:maven-plugin:2.1@jar' in dependencies
        'org.jenkins-ci.plugins:ant:1.2@jar' in dependencies
        'org.jenkins-ci.plugins:antisamy-markup-formatter:1.0@jar' in dependencies
        'org.jenkins-ci.plugins:javadoc:1.0@jar' in dependencies
        'org.jenkins-ci.plugins:mailer:1.8@jar' in dependencies
        'org.jenkins-ci.plugins:matrix-auth:1.0.2@jar' in dependencies
        'org.jenkins-ci.plugins:subversion:1.45@jar' in dependencies
    }

    def 'tasks are wired to lifecycle'() {
        setup:
        Project project = ProjectBuilder.builder().build()

        when:
        project.with {
            apply plugin: 'jpi'
        }
        (project as ProjectInternal).evaluate()

        then:
        project.tasks.assemble.dependsOn.contains(project.tasks.jpi)
    }

    private static List<String> collectDependencies(Project project, String configuration) {
        project.configurations.getByName(configuration).resolvedConfiguration.resolvedArtifacts.collect {
            "${it.moduleVersion.id}@${it.extension}".toString()
        }
    }
}
