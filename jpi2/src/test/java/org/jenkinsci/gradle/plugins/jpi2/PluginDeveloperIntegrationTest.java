package org.jenkinsci.gradle.plugins.jpi2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jenkinsci.gradle.plugins.jpi.IntegrationTestHelper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class PluginDeveloperIntegrationTest extends V2IntegrationTestBase {

    @Test
    void kotlinDslWritesAllDeveloperFieldsToPomAndManifest() throws IOException, XmlPullParserException {
        var ith = new IntegrationTestHelper(tempDir, "8.14");
        initBuild(ith);
        Files.writeString(
                ith.inProjectDir("build.gradle.kts").toPath(), getBasePluginConfig() + /* language=kotlin */ """
                jenkinsPlugin {
                    developers {
                        developer {
                            id.set("alice")
                            name.set("Alice Dev")
                            email.set("alice@example.com")
                            url.set("https://example.com/alice")
                            organization.set("Example Org")
                            organizationUrl.set("https://example.com")
                            timezone.set("America/Los_Angeles")
                            roles.addAll("developer", "maintainer")
                            properties.put("chat", "alice-chat")
                        }
                    }
                }
                """);

        ith.gradleRunner().withArguments("build", "publish").build();

        assertThat(manifestAttributes(ith).getValue("Plugin-Developers"))
                .isEqualTo("Alice Dev:alice:alice@example.com");
        assertThat(pom(ith).getDevelopers()).singleElement().satisfies(dev -> {
            assertThat(dev.getId()).isEqualTo("alice");
            assertThat(dev.getName()).isEqualTo("Alice Dev");
            assertThat(dev.getEmail()).isEqualTo("alice@example.com");
            assertThat(dev.getUrl()).isEqualTo("https://example.com/alice");
            assertThat(dev.getOrganization()).isEqualTo("Example Org");
            assertThat(dev.getOrganizationUrl()).isEqualTo("https://example.com");
            assertThat(dev.getTimezone()).isEqualTo("America/Los_Angeles");
            assertThat(dev.getRoles()).containsExactlyInAnyOrder("developer", "maintainer");
            assertThat(dev.getProperties()).containsEntry("chat", "alice-chat");
        });
    }

    @Test
    @Disabled(
            "PluginDeveloperSpec is a plain Kotlin lambda, so Groovy closures passed to developer {} are not delegated to PluginDeveloper")
    void groovyDslWritesDevelopersToPomAndManifest() throws IOException, XmlPullParserException {
        var ith = new IntegrationTestHelper(tempDir, "8.14");
        initBuild(ith);
        Files.writeString(ith.inProjectDir("build.gradle").toPath(), /* language=groovy */ """
                plugins {
                    id "org.jenkins-ci.jpi2"
                }
                repositories {
                    mavenCentral()
                    jenkinsPublic()
                }
                group = "com.example"
                version = "1.0.0"
                publishing {
                    repositories {
                        maven {
                            name = "local"
                            url = uri("${rootDir}/build/repo")
                        }
                    }
                }
                jenkinsPlugin {
                    developers {
                        developer {
                            id = "alice"
                            name = "Alice Dev"
                            email = "alice@example.com"
                        }
                        developer {
                            id = "bob"
                            name = "Bob Dev"
                            email = "bob@example.com"
                        }
                    }
                }
                """);

        ith.gradleRunner().withArguments("build", "publish").build();

        assertThat(manifestAttributes(ith).getValue("Plugin-Developers"))
                .isEqualTo("Alice Dev:alice:alice@example.com,Bob Dev:bob:bob@example.com");
        assertThat(pom(ith).getDevelopers())
                .extracting(Developer::getId, Developer::getName, Developer::getEmail)
                .containsExactly(
                        tuple("alice", "Alice Dev", "alice@example.com"), tuple("bob", "Bob Dev", "bob@example.com"));
    }

    @Test
    void partiallyConfiguredDeveloperLeavesMissingFieldsBlank() throws IOException, XmlPullParserException {
        var ith = new IntegrationTestHelper(tempDir, "8.14");
        initBuild(ith);
        Files.writeString(
                ith.inProjectDir("build.gradle.kts").toPath(), getBasePluginConfig() + /* language=kotlin */ """
                jenkinsPlugin {
                    developers {
                        developer {
                            id.set("alice")
                        }
                        developer {
                            name.set("Bob Dev")
                        }
                    }
                }
                """);

        ith.gradleRunner().withArguments("build", "publish").build();

        assertThat(manifestAttributes(ith).getValue("Plugin-Developers")).isEqualTo(":alice:,Bob Dev::");
        assertThat(pom(ith).getDevelopers())
                .extracting(Developer::getId, Developer::getName, Developer::getEmail, Developer::getRoles)
                .containsExactly(tuple("alice", null, null, List.of()), tuple(null, "Bob Dev", null, List.of()));
    }

    private static Attributes manifestAttributes(IntegrationTestHelper ith) throws IOException {
        try (var stream = ith.inProjectDir("build/jpi/META-INF/MANIFEST.MF")
                .toURI()
                .toURL()
                .openStream()) {
            return new Manifest(stream).getMainAttributes();
        }
    }

    private static Model pom(IntegrationTestHelper ith) throws IOException, XmlPullParserException {
        var pom = ith.inProjectDir("build/repo/com/example/test-plugin/1.0.0/test-plugin-1.0.0.pom");
        try (var reader = new FileReader(pom)) {
            return new MavenXpp3Reader().read(reader);
        }
    }
}
