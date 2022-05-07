package org.jenkinsci.gradle.plugins.jpi.deployment;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public abstract class CreateVersionlessLookupTask extends DefaultTask {
    @InputFiles
    public abstract ConfigurableFileCollection getAllResolvedPlugins();

    @Internal
    public abstract MapProperty<String, String> getModuleVersionToModule();

    @OutputFile
    public abstract RegularFileProperty getLookupDestination();

    @TaskAction
    void create() {
        Path output = getLookupDestination().getAsFile().get().toPath();
        Map<String, String> toVersionless = getModuleVersionToModule().get();
        try (BufferedWriter w = Files.newBufferedWriter(output, UTF_8, CREATE, TRUNCATE_EXISTING)) {
            for (File plugin : getAllResolvedPlugins()) {
                String filename = plugin.getName();
                if (filename.endsWith(".hpi") || filename.endsWith(".jpi")) {
                    String noVersion = toVersionless.getOrDefault(filename, filename);
                    w.write(String.join("\t", filename, noVersion));
                    w.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
