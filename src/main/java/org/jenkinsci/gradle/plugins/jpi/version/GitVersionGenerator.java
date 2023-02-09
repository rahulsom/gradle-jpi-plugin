package org.jenkinsci.gradle.plugins.jpi.version;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.StreamSupport;

public class GitVersionGenerator {
    private final int abbrevLength;
    private final String versionFormat;
    private final boolean allowDirty;
    private final Path gitRoot;

    public GitVersionGenerator(Path gitRoot, int abbrevLength, String versionFormat, boolean allowDirty) {
        this.gitRoot = gitRoot;
        // TODO abbrevLength should be 2 minimum
        this.abbrevLength = abbrevLength;
        this.versionFormat = versionFormat;
        this.allowDirty = allowDirty;
    }

    public String generate() {
        try (Git git = Git.open(gitRoot.toFile())) {
            Repository repo = git.getRepository();
            Status status = git.status().call();
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
            ObjectId head = repo.resolve("HEAD");
            if (head == null) {
                throw new RuntimeException("Cannot resolve HEAD for repository '" + gitRoot + "'");
            }
            String abbrevHash = repo.newObjectReader().abbreviate(head, abbrevLength).name();
            long headDepth = commitDepth(repo, head);
            return String.format(versionFormat, headDepth, abbrevHash);
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private long commitDepth(Repository repository, ObjectId objectId) throws IOException, GitAPIException {
        try (RevWalk walk = new RevWalk(repository)) {
            walk.setRetainBody(false);
            walk.markStart(walk.parseCommit(objectId));
            return StreamSupport.stream(walk.spliterator(), false).count();
        }
    }
}
