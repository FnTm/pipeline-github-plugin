package org.jenkinsci.plugins.pipeline.github;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.GroovyObjectSupport;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedCommitComment;
import org.jenkinsci.plugins.pipeline.github.client.ExtendedCommitService;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

/**
 * Groovy wrapper over a {@link RepositoryCommit}.
 *
 * Provides useful properties that allow one to iterate over a commits:
 * - Comments
 * - Files
 * - Statuses
 *
 * And a few methods to create:
 * - Review comments
 * - Statuses
 *
 * @author Aaron Whiteside
 * @see RepositoryCommit
 */
@SuppressFBWarnings("SE_BAD_FIELD")
public class CommitGroovyObject extends GroovyObjectSupport implements Serializable {
    private static final long serialVersionUID = 1L;

    private final RepositoryCommit commit;
    private final ExtendedCommitService commitService;
    private final RepositoryId base;

    CommitGroovyObject(final RepositoryCommit commit,
                       final ExtendedCommitService commitService,
                       final RepositoryId base) {
        Objects.requireNonNull(commit, "commit cannot be null");
        Objects.requireNonNull(commitService, "commitService cannot be null");
        Objects.requireNonNull(base, "base cannot be null");

        this.commit = commit;
        this.commitService = commitService;
        this.base = base;
    }

    @Whitelisted
    public String getSha() {
        return commit.getSha();
    }

    @Whitelisted
    public String getUrl() {
        return commit.getUrl();
    }

    @Whitelisted
    public String getAuthor() {
        return Optional.ofNullable(commit.getAuthor())
                .map(User::getLogin)
                .orElse(null);
    }

    @Whitelisted
    public String getCommitter() {
        return Optional.ofNullable(commit.getCommitter())
                .map(User::getLogin)
                .orElse(null);
    }

    @Whitelisted
    public String getMessage() {
        return commit.getCommit().getMessage();
    }

    @Whitelisted
    public int getCommentCount() {
        return commit.getCommit().getCommentCount();
    }

    @Whitelisted
    public int getAdditions() {
        return commit.getStats().getAdditions();
    }

    @Whitelisted
    public int getDeletions() {
        return commit.getStats().getDeletions();
    }

    @Whitelisted
    public int getTotalChanges() {
        return commit.getStats().getTotal();
    }

    @Whitelisted
    public Iterable<ReviewCommentGroovyObject> getComments() {
        Stream<ReviewCommentGroovyObject> stream = StreamSupport.stream(
                commitService.pageComments2(base, commit.getSha()).spliterator(), false)
                .flatMap(Collection::stream)
                .map(c -> new ReviewCommentGroovyObject(c, base, commitService));
        return stream::iterator;
    }

    @Whitelisted
    public Iterable<CommitStatusGroovyObject> getStatuses() {
        try {
            return commitService.getStatuses(base, commit.getSha())
                    .stream()
                    .map(CommitStatusGroovyObject::new)
                    .collect(toList());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public Iterable<String> getParents() {
        return commit.getParents()
                .stream()
                .map(Commit::getSha)
                .collect(toList());
    }

    @Whitelisted
    public Iterable<CommitFileGroovyObject> getFiles() {
        return commit.getFiles()
                .stream()
                .map(CommitFileGroovyObject::new)
                .collect(toList());
    }

    @Whitelisted
    public ReviewCommentGroovyObject comment(final Map<String, Object> params) {
        Objects.requireNonNull(params.get("body"), "body is a required argument");

        return comment(params.get("body").toString(),
                       params.get("path") != null ? params.get("path").toString() : null,
                       (Integer)params.get("position"));
    }

    @Whitelisted
    public ReviewCommentGroovyObject comment(final String body,
                                             final String path,
                                             final Integer position) {
        Objects.requireNonNull(body, "body is a required argument");

        ExtendedCommitComment comment = new ExtendedCommitComment();
        comment.setBody(body);
        comment.setPath(path);
        comment.setPosition(position);
        try {
            return new ReviewCommentGroovyObject(
                    commitService.addComment(base, commit.getSha(), comment),
                    base,
                    commitService);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Whitelisted
    public CommitStatusGroovyObject createStatus(final Map<String, Object> params) {
        Objects.requireNonNull(params.get("status"), "status is a required argument");

        return createStatus(params.get("status").toString(),
                            params.get("context") != null ? params.get("context").toString() : null,
                            params.get("description") != null ? params.get("description").toString() : null,
                            params.get("targetUrl") != null ? params.get("targetUrl").toString() : null);
    }

    @Whitelisted
    public CommitStatusGroovyObject createStatus(final String status,
                                                 final String context,
                                                 final String description,
                                                 final String targetUrl) {
        Objects.requireNonNull(status, "status is a required argument");

        CommitStatus commitStatus = new CommitStatus();
        commitStatus.setState(status);
        commitStatus.setContext(context);
        commitStatus.setDescription(description);
        commitStatus.setTargetUrl(targetUrl);
        try {
            return new CommitStatusGroovyObject(
                    commitService.createStatus(base, commit.getSha(), commitStatus));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
