package org.jenkinsci.gradle.plugins.jpi.support;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectFile implements Emitable {
    private final String name;
    private final List<CodeBlock> blocks;

    public ProjectFile(String name, List<CodeBlock> blocks) {
        this.name = name;
        this.blocks = blocks;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    @Override
    public String emit(Indenter indenter) {
        return blocks.stream()
                .sorted()
                .map(b -> b.emit(indenter))
                .collect(Collectors.joining("\n"));
    }

    public static class Builder {
        private String name;
        private final List<CodeBlock> blocks = new LinkedList<>();

        public ProjectFile build() {
            return new ProjectFile(name, blocks);
        }

        public Builder withPlugins(PluginsBlock block) {
            return withBlock(block);
        }

        public Builder withDependencies(DependenciesBlock block) {
            return withBlock(block);
        }

        public Builder withBlock(CodeBlock block) {
            blocks.add(block);
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder clearDependencies() {
            blocks.removeIf(block -> block instanceof DependenciesBlock);
            return this;
        }
    }
}
