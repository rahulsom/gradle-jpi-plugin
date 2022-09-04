package org.jenkinsci.gradle.plugins.jpi.localization;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.workers.WorkAction;
import org.jvnet.localizer.ClassGenerator;
import org.jvnet.localizer.Generator;
import org.jvnet.localizer.GeneratorConfig;
import org.jvnet.localizer.Reporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public abstract class RunGenerator implements WorkAction<LocalizationParameters> {
    private static final String EXPECTED_ROOT = "src/main/resources/";

    @Override
    public void execute() {
        File file = getParameters().getSourceFile().get().getAsFile();
        File outputDir = getParameters().getOutputDir().get();
        GeneratorConfig config = GeneratorConfig.of(outputDir, null, new InfoReporter(), null, false);
        ClassGenerator g = new Generator(config);

        try {
            String relPath = StringUtils.substringAfter(file.getAbsolutePath(), EXPECTED_ROOT);
            g.generate(file, relPath);
            g.build();
        } catch (IOException e) {
            throw new GradleException("Failed to generate Java source files", e);
        }
    }

    private static class InfoReporter implements Reporter {
        private static final Logger LOGGER = LoggerFactory.getLogger(InfoReporter.class);

        @Override
        public void debug(String msg) {
            LOGGER.info(msg);
        }
    }
}
