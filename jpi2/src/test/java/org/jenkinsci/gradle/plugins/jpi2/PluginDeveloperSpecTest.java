package org.jenkinsci.gradle.plugins.jpi2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Map;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PluginDeveloperSpecTest {
    private JenkinsPluginExtension extension;

    @BeforeEach
    void setUp() {
        var project = ProjectBuilder.builder().build();
        extension = project.getObjects().newInstance(JenkinsPluginExtension.class, project);
    }

    @Test
    void developerAddsConfiguredDeveloper() {
        extension.developers(spec -> spec.developer(dev -> {
            dev.getId().set("alice");
            dev.getName().set("Alice Dev");
            dev.getEmail().set("alice@example.com");
            dev.getUrl().set("https://example.com/alice");
            dev.getOrganization().set("Example Org");
            dev.getOrganizationUrl().set("https://example.com");
            dev.getTimezone().set("America/Los_Angeles");
            dev.getRoles().addAll("developer", "maintainer");
            dev.getProperties().put("chat", "alice-chat");
        }));

        assertThat(extension.getPluginDevelopers().get()).singleElement().satisfies(dev -> {
            assertThat(dev.getId().get()).isEqualTo("alice");
            assertThat(dev.getName().get()).isEqualTo("Alice Dev");
            assertThat(dev.getEmail().get()).isEqualTo("alice@example.com");
            assertThat(dev.getUrl().get()).isEqualTo("https://example.com/alice");
            assertThat(dev.getOrganization().get()).isEqualTo("Example Org");
            assertThat(dev.getOrganizationUrl().get()).isEqualTo("https://example.com");
            assertThat(dev.getTimezone().get()).isEqualTo("America/Los_Angeles");
            assertThat(dev.getRoles().get()).containsExactlyInAnyOrder("developer", "maintainer");
            assertThat(dev.getProperties().get()).containsExactlyEntriesOf(Map.of("chat", "alice-chat"));
        });
    }

    @Test
    void developerCreatesDistinctInstancePerCallInDeclarationOrder() {
        extension.developers(spec -> {
            spec.developer(dev -> dev.getId().set("alice"));
            spec.developer(dev -> dev.getId().set("bob"));
        });

        var developers = extension.getPluginDevelopers().get();
        assertThat(developers).extracting(dev -> dev.getId().get()).containsExactly("alice", "bob");
        assertThat(developers.get(0)).isNotSameAs(developers.get(1));
    }

    @Test
    void developersBlocksAccumulate() {
        extension.developers(spec -> spec.developer(dev -> dev.getId().set("alice")));
        extension.developers(spec -> spec.developer(dev -> dev.getId().set("bob")));

        assertThat(extension.getPluginDevelopers().get())
                .extracting(dev -> dev.getId().get())
                .containsExactly("alice", "bob");
    }

    @Test
    void developerAppendsToDevelopersAddedDirectly() {
        var existing = ProjectBuilder.builder().build().getObjects().newInstance(PluginDeveloper.class);
        existing.getId().set("existing");
        extension.getPluginDevelopers().add(existing);

        extension.developers(spec -> spec.developer(dev -> dev.getId().set("alice")));

        assertThat(extension.getPluginDevelopers().get())
                .extracting(dev -> dev.getId().get())
                .containsExactly("existing", "alice");
    }

    @Test
    void developerWithEmptyActionAddsUnconfiguredDeveloper() {
        extension.developers(spec -> spec.developer(dev -> {}));

        assertThat(extension.getPluginDevelopers().get()).singleElement().satisfies(dev -> {
            assertThat(dev.getId().isPresent()).isFalse();
            assertThat(dev.getName().isPresent()).isFalse();
            assertThat(dev.getEmail().isPresent()).isFalse();
            assertThat(dev.getRoles().get()).isEmpty();
            assertThat(dev.getProperties().get()).isEmpty();
        });
    }

    @Test
    void developerRunsActionImmediately() {
        var seen = new ArrayList<String>();

        extension.developers(spec -> {
            spec.developer(dev -> seen.add("first"));
            seen.add("between");
            spec.developer(dev -> seen.add("second"));
        });

        assertThat(seen).containsExactly("first", "between", "second");
    }

    @Test
    void developersWithoutDeveloperCallsAddsNothing() {
        extension.developers(spec -> {});

        assertThat(extension.getPluginDevelopers().get()).isEmpty();
    }

    @Test
    void developerValuesCanBeLazilyProvided() {
        var project = ProjectBuilder.builder().build();
        var lazy = project.getObjects().property(String.class);
        extension.developers(spec -> spec.developer(dev -> dev.getId().set(lazy)));

        lazy.set("late");

        assertThat(extension.getPluginDevelopers().get())
                .extracting(dev -> dev.getId().get(), dev -> dev.getName().getOrNull())
                .containsExactly(tuple("late", null));
    }
}
