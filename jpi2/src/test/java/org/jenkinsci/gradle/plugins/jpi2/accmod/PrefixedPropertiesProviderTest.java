package org.jenkinsci.gradle.plugins.jpi2.accmod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.gradle.api.Project;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

class PrefixedPropertiesProviderTest {

    @Test
    void stripsPrefixAndPreservesValues() {
        var result = propertiesFor(Map.of(
                "checkAccessModifier.ignoreFailures", "true",
                "checkAccessModifier.someKey", "some value"));

        assertThat(result)
                .containsExactlyInAnyOrderEntriesOf(Map.of("ignoreFailures", "true", "someKey", "some value"));
    }

    @Test
    void skipsNullValues() {
        var properties = new HashMap<String, String>();
        properties.put("checkAccessModifier.enabled", "yes");
        properties.put("checkAccessModifier.unset", null);

        assertThat(propertiesFor(properties)).containsOnlyKeys("enabled").containsEntry("enabled", "yes");
    }

    private static Map<String, String> propertiesFor(Map<String, String> properties) {
        var project = mock(Project.class);
        var providers = mock(ProviderFactory.class);
        var realProject = ProjectBuilder.builder().build();
        when(project.getProviders()).thenReturn(providers);
        when(providers.gradlePropertiesPrefixedBy(CheckAccessModifierTask.PREFIX))
                .thenReturn(realProject.provider(() -> properties));

        return PrefixedPropertiesProvider.gradlePropertiesPrefixedBy(project, CheckAccessModifierTask.PREFIX)
                .get();
    }
}
