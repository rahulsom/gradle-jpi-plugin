package org.jenkinsci.gradle.plugins.jpi.internal;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

public class DependencyLicenseValidator {
    private static final String HEADER = "Could not resolve license(s) via POM for %d %s:%n";
    private static final String DEPENDENCY = "\t- %s%n";
    private static final String FOOTER = "The above will be missing from %s%n";
    
    static Result validate(Set<String> requested, Set<String> resolved, Path destination) {
        Set<String> unresolvable = new HashSet<>();
        for (String req : requested) {
            if (!resolved.contains(req)) {
                unresolvable.add(req);
            }
        }
        StringBuilder sb = new StringBuilder();
        if (!unresolvable.isEmpty()) {
            String pluralized = unresolvable.size() == 1 ? "dependency" : "dependencies";
            sb.append(String.format(HEADER, unresolvable.size(), pluralized));
            unresolvable.stream().sorted().map(r -> String.format(DEPENDENCY, r)).forEach(sb::append);
            sb.append(String.format(FOOTER, destination));
        }
        return new Result(unresolvable.size() > 0, sb.toString());
    }
    
    public static class Result {
        private final boolean unresolved;
        private final String message;

        public Result(boolean unresolved, String message) {
            this.unresolved = unresolved;
            this.message = message;
        }

        public boolean isUnresolved() {
            return unresolved;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Result result = (Result) o;
            return unresolved == result.unresolved && Objects.equals(message, result.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(unresolved, message);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Result.class.getSimpleName() + "[", "]")
                    .add("unresolved=" + unresolved)
                    .add("message='" + message + "'")
                    .toString();
        }
    }
}
