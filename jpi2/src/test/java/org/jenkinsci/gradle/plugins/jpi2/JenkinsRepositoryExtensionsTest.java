package org.jenkinsci.gradle.plugins.jpi2;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.gradle.kotlin.dsl.JenkinsRepositoryExtensions;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

class JenkinsRepositoryExtensionsTest {

    @Test
    void jenkinsIncrementalsRegistersIncrementalsRepository() {
        var repositories = ProjectBuilder.builder().build().getRepositories();

        var repository = JenkinsRepositoryExtensions.jenkinsIncrementals(repositories);

        assertThat(repository.getName()).isEqualTo("jenkinsIncrementals");
        assertThat(repository.getUrl()).isEqualTo(URI.create("https://repo.jenkins-ci.org/incrementals/"));
        assertThat(repositories.findByName("jenkinsIncrementals")).isSameAs(repository);
    }

    @Test
    void jenkinsSnapshotsRegistersSnapshotsRepository() {
        var repositories = ProjectBuilder.builder().build().getRepositories();

        var repository = JenkinsRepositoryExtensions.jenkinsSnapshots(repositories);

        assertThat(repository.getName()).isEqualTo("jenkinsSnapshots");
        assertThat(repository.getUrl()).isEqualTo(URI.create("https://repo.jenkins-ci.org/snapshots/"));
        assertThat(repositories.findByName("jenkinsSnapshots")).isSameAs(repository);
    }
}
