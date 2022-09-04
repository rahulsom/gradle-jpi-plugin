package org.jenkinsci.gradle.plugins.jpi.localization;

import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.BuildException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.jvnet.localizer.ClassGenerator;
import org.jvnet.localizer.Generator;
import org.jvnet.localizer.GeneratorConfig;
import org.jvnet.localizer.Reporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public abstract class LocalizationTask extends SourceTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalizationTask.class);
    
    public LocalizationTask() {
        include("**/Messages.properties");
    }
    
    @OutputDirectory
    public abstract Property<File> getOutputDir();
    
    @TaskAction
    void generate() {
        for (File file : getSource()) {
            GeneratorConfig config = GeneratorConfig.of(getOutputDir().get(), null, new Reporter() {
                public void debug(String msg) {
                    LOGGER.info(msg);
                }
            }, null, false);
            ClassGenerator g = new Generator(config);

            try {
                String relPath = StringUtils.substringAfter(file.getAbsolutePath(), "src/main/resources/");
                g.generate(file, relPath);
                g.build();
            } catch (IOException e) {
                throw new BuildException(e);
            }
        }
    }
}
