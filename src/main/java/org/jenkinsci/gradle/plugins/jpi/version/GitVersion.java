package org.jenkinsci.gradle.plugins.jpi.version;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.StreamSupport;

public class GitVersion {
    private static final int DEFAULT_ABBREV_LENGTH = 12;
    private static final String DEFAULT_VERSION_FORMAT = "%d.%s";
    private static final String AMBIGUOUS_COMMIT_MSG = "Found commit '%s' with same abbreviated hash '%s' and depth '%s' as HEAD ('%s')." +
        " Please raise the abbreviated length to get a unique Git based version";

    private final int abbrevLength;
    private final String versionFormat;
    private final boolean allowDirty;
    private final Path gitRoot;

    private static final Logger LOGGER = LoggerFactory.getLogger(GitVersion.class);

    private GitVersion(Path gitRoot, int abbrevLength, String versionFormat, boolean allowDirty) {
        this.gitRoot = gitRoot;
        this.abbrevLength = abbrevLength;
        this.versionFormat = versionFormat;
        this.allowDirty = allowDirty;
    }

    public static GitVersionBuilder builder(Path gitRoot) {
        return new GitVersionBuilder(gitRoot);
    }

    private static class GitVersionBuilder {
        private final Path gitRoot;
        private int abbrevLength = DEFAULT_ABBREV_LENGTH;
        private String versionFormat = DEFAULT_VERSION_FORMAT;
        private boolean allowDirty;

        public GitVersionBuilder(Path gitRoot) {
            this.gitRoot = gitRoot;
        }

        public GitVersionBuilder abbrevLength(int abbrevLength) {
            this.abbrevLength = abbrevLength;
            return this;
        }

        public GitVersionBuilder versionFormat(String versionFormat) {
            this.versionFormat = versionFormat;
            return this;
        }

        public GitVersionBuilder allowDirty(boolean allowDirty) {
            this.allowDirty = allowDirty;
            return this;
        }

        public GitVersion build() {
            return new GitVersion(gitRoot, abbrevLength, versionFormat, allowDirty);
        }
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
            String abbrevHash = head.abbreviate(abbrevLength).name();
            long headDepth = commitDepth(repo, head);
            checkAmbiguousCommits(git, repo, new HeadMeta(head, abbrevHash, headDepth));
            return String.format(versionFormat, headDepth, abbrevHash);
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkAmbiguousCommits(Git git, Repository repository, HeadMeta headMeta) throws IOException, GitAPIException {
        try (RevWalk walk = new RevWalk(repository)) {
            walk.setRetainBody(false);
            RevCommit headRev = walk.parseCommit(headMeta.head);
            Set<ObjectId> alreadyWalked = new HashSet<>();
            for (Ref ref : git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()) {
                RevCommit branchTip = walk.parseCommit(ref.getObjectId());
                LOGGER.debug("Checking branch '{}' for ambiguous commits", ref);
                walk.reset();
                walk.markStart(branchTip);
                for (RevCommit c : walk) {
                    // No need to continue if a commit has already been checked: since a commit hash
                    // depends on its ancestors, that means all its ancestors have also been checked once
                    if (headRev.equals(c) || alreadyWalked.contains(c.getId())) {
                        break;
                    }
                    alreadyWalked.add(c.getId());
                    String abbrevHash = c.getId().abbreviate(abbrevLength).name();
                    if (abbrevHash.equals(headMeta.abbrevHash)) {
                        long depth = commitDepth(repository, c.getId());
                        if (depth != headMeta.headDepth) {
                            LOGGER.debug("Found commit '{}' has same abbreviated hash '{}'  but different depth '{}' from head depth '{}'",
                                c.getId().name(), abbrevHash, depth, headMeta.headDepth);
                        } else {
                            throw new RuntimeException(String.format(AMBIGUOUS_COMMIT_MSG, c.getId().getName(),
                                abbrevHash, depth, headRev.getId().getName()));
                        }
                    }
                }
            }
        }
    }

    private long commitDepth(Repository repository, ObjectId objectId) throws IOException, GitAPIException {
        try (RevWalk walk = new RevWalk(repository)) {
            walk.setRetainBody(false);
            walk.markStart(walk.parseCommit(objectId));
            return StreamSupport.stream(walk.spliterator(), false).count();
        }
    }

    private static final class HeadMeta {
        private final ObjectId head;
        private final String abbrevHash;
        private final long headDepth;

        HeadMeta(ObjectId head, String abbrevHash, long headDepth) {
            this.head = head;
            this.abbrevHash = abbrevHash;
            this.headDepth = headDepth;
        }
    }
}
