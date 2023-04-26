package org.jenkinsci.gradle.plugins.jpi.version;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.StreamSupport;

public class GitVersionGenerator {
    private final int abbrevLength;
    private final String versionFormat;
    private final boolean sanitize;
    private final boolean allowDirty;
    private final Path gitRoot;

    public GitVersionGenerator(Path gitRoot, int abbrevLength, String versionFormat, boolean allowDirty, boolean sanitize) {
        this.gitRoot = gitRoot;
        // TODO abbrevLength should be 2 minimum
        this.abbrevLength = abbrevLength;
        this.versionFormat = versionFormat;
        this.sanitize = sanitize;
        this.allowDirty = allowDirty;
    }

    public GitVersion generate() {
        try (Git git = Git.open(gitRoot.toFile())) {
            Repository repo = git.getRepository();
            Status status = git.status().call();
            checkGitStatus(status);
            ObjectId head = repo.resolve("HEAD");
            if (head == null) {
                throw new RuntimeException("Cannot resolve HEAD for repository '" + gitRoot + "'");
            }
            try (ObjectReader reader = repo.newObjectReader()) {
                String abbrevHash = reader.abbreviate(head, abbrevLength).name();
                long headDepth = commitDepth(repo, head);
                if (sanitize) {
                    abbrevHash = sanitize(abbrevHash);
                }
                return new GitVersion(head.getName(), String.format(versionFormat, headDepth, abbrevHash));
            }
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String sanitize(String hash) {
        return hash.replaceAll("[ab]", "$0_");
    }

    private void checkGitStatus(Status status) {
        if (!status.isClean() && !allowDirty) {
            StringBuilder str = new StringBuilder("Repository '" + gitRoot + "' has some pending changes:");
            if (!status.getUntracked().isEmpty()) {
                str.append("\n- untracked files: ").append(status.getUntracked());
            }
            if (!status.getUncommittedChanges().isEmpty()) {
                str.append("\n- uncommitted changes: ").append(status.getUncommittedChanges());
            }
            throw new RuntimeException(str.toString());
        }
    }

    private long commitDepth(Repository repository, ObjectId objectId) throws IOException, GitAPIException {
        try (RevWalk walk = new RevWalk(repository)) {
            walk.setRetainBody(false);
            walk.markStart(walk.parseCommit(objectId));
            return StreamSupport.stream(walk.spliterator(), false).count();
        }
    }

    static class GitVersion {
        private final String fullHash;
        private final String abbreviatedHash;

        public GitVersion(String fullHash, String abbreviatedHash) {
            this.fullHash = fullHash;
            this.abbreviatedHash = abbreviatedHash;
        }

        @Override
        public String toString() {
            return abbreviatedHash + "\n" + fullHash;
        }

        public String getAbbreviatedHash() {
            return abbreviatedHash;
        }

        public String getFullHash() {
            return fullHash;
        }
    }

}
