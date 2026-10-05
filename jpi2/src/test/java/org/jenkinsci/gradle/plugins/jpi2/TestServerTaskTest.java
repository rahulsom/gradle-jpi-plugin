package org.jenkinsci.gradle.plugins.jpi2;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TestServerTaskTest {

    @Test
    void recognizesSuccessfulStartup() {
        assertThat(TestServerTask.verdictForLine("INFO Jenkins is fully up and running"))
                .isEqualTo(new TestServerTask.LaunchResult(TestServerTask.Status.SUCCESS, 0, null));
    }

    @Test
    void recognizesKnownStartupFailures() {
        for (var message :
                new String[] {"Failed Loading plugin", "Jenkins stopped", "java.io.IOException: Failed to load"}) {
            var line = "ERROR " + message;
            assertThat(TestServerTask.verdictForLine(line))
                    .isEqualTo(new TestServerTask.LaunchResult(TestServerTask.Status.CRASH, -1, line));
        }
    }

    @Test
    void ignoresOutputWithoutAStartupVerdict() {
        assertThat(TestServerTask.verdictForLine("INFO Jenkins is starting")).isNull();
    }
}
