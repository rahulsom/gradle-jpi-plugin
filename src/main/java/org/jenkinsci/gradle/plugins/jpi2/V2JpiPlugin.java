package org.jenkinsci.gradle.plugins.jpi2;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.bundling.War;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class V2JpiPlugin implements Plugin<Project> {

    public static final String JENKINS_PLUGIN = "jenkinsPlugin";
    public static final String JENKINS_PLUGIN_COMPILE_ONLY = "jenkinsPluginCompileOnly";
    public static final String SERVER_JENKINS_PLUGIN = "serverJenkinsPlugin";
    public static final String SERVER_TASK_CLASSPATH = "serverTaskClasspath";

    @Override
    public void apply(@NotNull Project project) {
        project.getPlugins().apply(JavaLibraryPlugin.class);
        project.getPlugins().apply(WarPlugin.class);

        var jenkinsPlugin = project.getConfigurations().create(JENKINS_PLUGIN);
        var jenkinsPluginCompileOnly = project.getConfigurations().create(JENKINS_PLUGIN_COMPILE_ONLY, new Action<>() {
            @Override
            public void execute(@NotNull Configuration c) {
                c.withDependencies(new Action<>() {
                    @Override
                    public void execute(@NotNull DependencySet dependencies) {
                        addJarDependenciesFromJpis(project, jenkinsPlugin, dependencies);
                        dependencies.add(project.getDependencies()
                                .create("org.jenkins-ci.main:jenkins-core:" + project.getProperties().get("jenkins.version")));
                    }
                });
            }
        });
        project.getConfigurations().getByName("compileOnly").extendsFrom(jenkinsPluginCompileOnly);

        var serverJenkinsPlugin = project.getConfigurations().create(SERVER_JENKINS_PLUGIN, new Action<>() {
            @Override
            public void execute(@NotNull Configuration c) {
                c.extendsFrom(jenkinsPlugin);
                c.withDependencies(new Action<>() {
                    @Override
                    public void execute(@NotNull DependencySet dependencies) {
                        jenkinsPlugin.getDependencies().forEach(it -> {
                            if (it instanceof ExternalModuleDependency) {
                                dependencies.add(project.getDependencies()
                                        .create(it.getGroup() + ":" + it.getName() + ":" + it.getVersion()));
                            } else if (it instanceof ProjectDependency p) {
                                Project rootProject = project.getRootProject();
                                Project pluginProject = rootProject.getChildProjects()
                                        .get(p.getName());
                                Task warTask = pluginProject.getTasks().findByName("war");
                                if (warTask != null) {
                                    dependencies.add(project.getDependencies().create(warTask.getOutputs().getFiles()));
                                    Configuration jenkinsPluginFromDependentProject = pluginProject
                                            .getConfigurations()
                                            .getByName("jenkinsPlugin");
                                    dependencies.add(project.getDependencies().create(jenkinsPluginFromDependentProject));
                                }
                            }
                        });
                    }
                });
            }
        });
        var serverTaskClasspath = project.getConfigurations().create(SERVER_TASK_CLASSPATH, new Action<>() {
            @Override
            public void execute(@NotNull Configuration c) {
                c.setCanBeConsumed(false);
                c.setTransitive(false);
                c.withDependencies(new Action<>() {
                    @Override
                    public void execute(@NotNull DependencySet dependencies) {
                        dependencies.add(project.getDependencies()
                                .create("org.jenkins-ci.main:jenkins-war:" + project.getProperties().get("jenkins.version")));
                    }
                });
            }
        });

        configureWarTask(project, jenkinsPlugin);

        final var projectRoot = project.getLayout().getProjectDirectory().getAsFile().getAbsolutePath();

        final var prepareServer = project.getTasks()
                .register("prepareServer", PrepareServerTask.class, new Action<>() {
                    @Override
                    public void execute(@NotNull PrepareServerTask serverTask) {
                        var war = project.getTasks().getByName("war");
                        serverTask.setProjectRoot(projectRoot);
                        serverTask.setServerPluginsClasspath(serverJenkinsPlugin);

                        serverTask.setJpi(war.getOutputs().getFiles().getSingleFile());

                        serverTask.getInputs().files(serverJenkinsPlugin);
                        serverTask.getInputs().files(war.getOutputs().getFiles());
                        serverTask.getOutputs().dir(new File(projectRoot + "/work/plugins"));
                    }
                });

        project.getTasks().register("server", JavaExec.class, new Action<>() {
            @Override
            public void execute(@NotNull JavaExec spec) {
                spec.classpath(serverTaskClasspath);
                spec.setStandardOutput(System.out);
                spec.setErrorOutput(System.err);
                spec.args(List.of(
                        "--webroot=" + projectRoot + "/build/jenkins/war",
                        "--pluginroot=" + projectRoot + "/build/jenkins/plugins",
                        "--extractedFilesFolder=" + projectRoot + "/build/jenkins/extracted",
                        "--commonLibFolder=" + projectRoot + "/work/lib"
                ));
                spec.environment("JENKINS_HOME", projectRoot + "/work");

                spec.dependsOn(prepareServer);

                spec.getOutputs().upToDateWhen(new Spec<>() {
                    @Override
                    public boolean isSatisfiedBy(Task element) {
                        return false;
                    }
                });
            }
        });
    }

    private void addJarDependenciesFromJpis(@NotNull Project project, Configuration jpiSource, @NotNull DependencySet jarSink) {
        jpiSource.getAllDependencies()
                .forEach(it -> {
                    if (it instanceof ExternalModuleDependency) {
                        jarSink.add(project.getDependencies()
                                .create(it.getGroup() + ":" + it.getName() + ":" + it.getVersion() + "@jar"));
                    } else if (it instanceof ProjectDependency p) {
                        Project rootProject = project.getRootProject();
                        Map<String, Project> childProjects = rootProject.getChildProjects();
                        Task jar = childProjects.get(p.getName()).getTasks().getByName("jar");
                        jarSink.add(project.getDependencies().create(jar.getOutputs().getFiles()));
                    }
                });
    }

    private static void configureWarTask(@NotNull Project project, Configuration jenkinsPlugin/*, Configuration jpi*/) {
        project.getTasks().register("explodedWar", Copy.class, new Action<>() {
            @Override
            public void execute(@NotNull Copy sync) {
                sync.into(project.getLayout().getBuildDirectory().dir("jpi"));
                sync.with((War) project.getTasks().getByName("war"));
            }
        });
        project.getTasks().withType(War.class).configureEach(war -> {
            war.getArchiveExtension().set("jpi");
            configureManifest(project, jenkinsPlugin, war);
            war.from(project.getTasks().named("jar"), new Action<>() {
                @Override
                public void execute(@NotNull CopySpec copySpec) {
                    copySpec.into("WEB-INF/lib");
                }
            });
            var classpath = Optional.ofNullable(war.getClasspath()).map(FileCollection::getFiles).orElse(Set.of());
            classpath.removeIf(it -> !it.getName().endsWith(".jar"));
            war.setClasspath(classpath);
            war.finalizedBy("explodedWar");
        });
    }

    private static void configureManifest(@NotNull Project project, Configuration jenkinsPlugin, War war) {
        war.manifest(new Action<>() {
            @Override
            public void execute(@NotNull Manifest manifest) {
                var pluginDependencies = jenkinsPlugin.getDependencies()
                        .stream()
                        .map(it -> it.getName() + ":" + it.getVersion())
                        .collect(Collectors.joining(","));

                manifest.getAttributes()
                        .put("Implementation-Title", project.getGroup() + "#" + project.getName() + ";" + project.getVersion());
                manifest.getAttributes().put("Implementation-Version", project.getVersion());
                if (!pluginDependencies.isEmpty()) {
                    manifest.getAttributes().put("Plugin-Dependencies", pluginDependencies);
                }
                manifest.getAttributes().put("Plugin-Version", project.getVersion());
                manifest.getAttributes().put("Short-Name", project.getName());
                manifest.getAttributes().put("Long-Name", Optional.ofNullable(project.getDescription()).orElse(project.getName()));

                manifest.getAttributes().put("Jenkins-Version", project.getProperties().get("jenkins.version"));
            }
        });
    }
}
