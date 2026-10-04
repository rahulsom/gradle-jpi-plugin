package org.jenkinsci.gradle.plugins.jpi2;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import org.gradle.testkit.runner.TaskOutcome;
import org.jenkinsci.gradle.plugins.jpi.IntegrationTestHelper;
import org.junit.jupiter.api.Test;

class JenkinsRepositoryExtensionsIntegrationTest extends V2IntegrationTestBase {

    @Test
    void kotlinDslRegistersIncrementalsAndSnapshotsRepositories() throws IOException {
        var ith = new IntegrationTestHelper(tempDir, "8.14");
        initBuild(ith);
        Files.writeString(ith.inProjectDir("build.gradle.kts").toPath(), /* language=kotlin */ """
                import java.net.URI
                import org.gradle.kotlin.dsl.jenkinsIncrementals
                import org.gradle.kotlin.dsl.jenkinsSnapshots

                plugins {
                    id("org.jenkins-ci.jpi2")
                }

                repositories {
                    val incrementals = jenkinsIncrementals()
                    check(incrementals.name == "jenkinsIncrementals")
                    check(incrementals.url == URI("https://repo.jenkins-ci.org/incrementals/"))
                    check(findByName("jenkinsIncrementals") === incrementals)

                    val snapshots = jenkinsSnapshots()
                    check(snapshots.name == "jenkinsSnapshots")
                    check(snapshots.url == URI("https://repo.jenkins-ci.org/snapshots/"))
                    check(findByName("jenkinsSnapshots") === snapshots)
                }
                """);

        var result = ith.gradleRunner().withArguments("help").build();

        var task = result.task(":help");
        assertThat(task).isNotNull();
        assertThat(task.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    }
}
