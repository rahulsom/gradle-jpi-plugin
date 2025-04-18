package org.jenkinsci.gradle.plugins.jpi2;

import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class PrepareServerTask extends DefaultTask {
    private Configuration serverPluginsClasspath;
    private File jpi;
    private String projectRoot;

    public void setServerPluginsClasspath(Configuration serverPluginsClasspath) {
        this.serverPluginsClasspath = serverPluginsClasspath;
    }

    public void setJpi(File jpi) {
        this.jpi = jpi;
    }

    public void setProjectRoot(String projectRoot) {
        this.projectRoot = projectRoot;
    }

    @TaskAction
    public void run() throws IOException {
        FileUtils.deleteDirectory(new File(projectRoot + "/work/plugins"));
        new File(projectRoot + "/work/plugins/").mkdirs();
        serverPluginsClasspath.resolve().stream()
                .filter(it -> it.getName().endsWith(".jpi") || it.getName().endsWith(".hpi"))
                .forEach(it -> {
                    try {
                        Files.copy(it.toPath(), new File(projectRoot + "/work/plugins/" + it.getName()).toPath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        Files.copy(jpi.toPath(), new File(projectRoot + "/work/plugins/" + jpi.getName()).toPath());
    }
}
