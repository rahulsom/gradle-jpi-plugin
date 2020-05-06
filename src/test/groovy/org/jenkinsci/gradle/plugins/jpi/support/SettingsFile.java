package org.jenkinsci.gradle.plugins.jpi.support;

import java.util.LinkedList;
import java.util.List;

public class SettingsFile implements Emitable {
    private final List<Statement> statements;

    public SettingsFile(List<Statement> statements) {
        this.statements = statements;
    }

    @Override
    public String emit(Indenter indenter) {
        StringBuilder sb = new StringBuilder();
        for (Statement statement : statements) {
            sb.append(indenter.indent()).append(statement.emit(indenter));
        }
        return sb.append('\n').toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;

        public Builder withRootProjectName(String name) {
            this.name = name;
            return this;
        }

        public SettingsFile build() {
            List<Statement> statements = new LinkedList<>();
            statements.add(Statement.create("rootProject.name = $S", name));
            return new SettingsFile(statements);
        }
    }
}
