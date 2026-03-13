package org.jenkinsci.gradle.plugins.jpi2;

import com.google.common.io.Files;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.assertj.core.groups.Tuple;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.gradle.testkit.runner.GradleRunner;
import org.jenkinsci.gradle.plugins.jpi.IntegrationTestHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnOs(value = OS.WINDOWS, disabledReason = "TempDir doesn't appear to work correctly on Windows")
class ManifestIntegrationTest extends V2IntegrationTestBase {

    @Test
    void manifestContainsVersionWhenUsingForce() throws IOException {
        var ith = new IntegrationTestHelper(tempDir, "8.14");
        initBuild(ith);
        Files.write((getBasePluginConfig() + /* language=kotlin */ """
                configurations.configureEach {
                    resolutionStrategy {
                        force("org.jenkins-ci.plugins:git:5.7.0")
                    }
                }
                dependencies {
                    implementation("org.jenkins-ci.plugins:git")
                }
                """).getBytes(StandardCharsets.UTF_8), ith.inProjectDir("build.gradle.kts"));

        GradleRunner gradleRunner = ith.gradleRunner();

        gradleRunner.withArguments("build").build();

        var manifestData = manifestAttributes(ith);
        assertThat(manifestData).isNotNull().isNotEmpty();
        assertThat(manifestData.getValue("Jenkins-Version")).isEqualTo("2.492.3");
        assertThat(manifestData.getValue("Plugin-Dependencies")).isEqualTo("git:5.7.0");
    }

    @Test
    void manifestContainsVersionWhenUsingBom() throws IOException, XmlPullParserException {
        var ith = new IntegrationTestHelper(tempDir, "8.14");
        initBuild(ith);
        ith.mkDirInProjectDir("src-repo/com/example/bom/bom/1.0.0");
        Files.write(/* language=xml */ """
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example.bom</groupId>
                    <artifactId>bom</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.jenkins-ci.plugins</groupId>
                                <artifactId>git</artifactId>
                                <version>5.7.0</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """.getBytes(StandardCharsets.UTF_8), ith.inProjectDir("src-repo/com/example/bom/bom/1.0.0/bom-1.0.0.pom"));
        Files.write((getBasePluginConfig() + /* language=kotlin */ """
                repositories {
                    maven {
                        url = uri("${rootDir}/src-repo")
                    }
                }
                dependencies {
                    implementation(platform("com.example.bom:bom:1.0.0"))
                    api("org.jenkins-ci.plugins:git")
                }
                """).getBytes(StandardCharsets.UTF_8), ith.inProjectDir("build.gradle.kts"));

        GradleRunner gradleRunner = ith.gradleRunner();

        var result = gradleRunner.withArguments("build", "publish").build();

        assertThat(result.getOutput()).doesNotContain("Dependency resolution rules will not be applied to configuration");

        var manifestData = manifestAttributes(ith);
        assertThat(manifestData).isNotNull().isNotEmpty();
        assertThat(manifestData.getValue("Jenkins-Version")).isEqualTo("2.492.3");
        assertThat(manifestData.getValue("Plugin-Dependencies")).isEqualTo("git:5.7.0");

        var pom = ith.inProjectDir("build/repo/com/example/test-plugin/1.0.0/test-plugin-1.0.0.pom");
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader(pom));
        assertThat(model).isNotNull();

        assertThat(model.getGroupId()).isEqualTo("com.example");
        assertThat(model.getArtifactId()).isEqualTo("test-plugin");
        assertThat(model.getVersion()).isEqualTo("1.0.0");
        assertThat(model.getPackaging()).isEqualTo("jpi");
        var dependencies = model.getDependencies();
        assertThat(dependencies)
                .extracting(Dependency::getGroupId, Dependency::getArtifactId, Dependency::getVersion, Dependency::getScope)
                .containsExactlyInAnyOrder(
                        new Tuple("org.jenkins-ci.plugins", "git", "5.7.0", "compile")
                );
    }

    @Test
    void supportDynamicLoadingDefaultsToTrueWhenNoExtensionsExist() throws IOException {
        var ith = new IntegrationTestHelper(tempDir, "8.14");
        initBuild(ith);
        Files.write(getBasePluginConfig().getBytes(StandardCharsets.UTF_8), ith.inProjectDir("build.gradle.kts"));

        ith.gradleRunner().withArguments("build").build();

        assertThat(manifestAttributes(ith).getValue("Support-Dynamic-Loading")).isEqualTo("true");
    }

    @Test
    void supportDynamicLoadingIsTrueWhenAllExtensionsAreYes() throws IOException {
        var ith = new IntegrationTestHelper(tempDir, "8.14");
        initBuild(ith);
        Files.write(getBasePluginConfig().getBytes(StandardCharsets.UTF_8), ith.inProjectDir("build.gradle.kts"));
        writeJavaSource(ith, "com/example/plugin/AlwaysReloadable.java", """
                package com.example.plugin;

                @hudson.Extension(dynamicLoadable = jenkins.YesNoMaybe.YES)
                public class AlwaysReloadable {
                }
                """);

        ith.gradleRunner().withArguments("build").build();

        assertThat(manifestAttributes(ith).getValue("Support-Dynamic-Loading")).isEqualTo("true");
    }

    @Test
    void supportDynamicLoadingIsOmittedWhenAnyExtensionIsMaybe() throws IOException {
        var ith = new IntegrationTestHelper(tempDir, "8.14");
        initBuild(ith);
        Files.write(getBasePluginConfig().getBytes(StandardCharsets.UTF_8), ith.inProjectDir("build.gradle.kts"));
        writeJavaSource(ith, "com/example/plugin/MaybeReloadable.java", """
                package com.example.plugin;

                @hudson.Extension(dynamicLoadable = jenkins.YesNoMaybe.MAYBE)
                public class MaybeReloadable {
                }
                """);
        writeJavaSource(ith, "com/example/plugin/AlwaysReloadable.java", """
                package com.example.plugin;

                @hudson.Extension(dynamicLoadable = jenkins.YesNoMaybe.YES)
                public class AlwaysReloadable {
                }
                """);

        ith.gradleRunner().withArguments("build").build();

        assertThat(manifestAttributes(ith).getValue("Support-Dynamic-Loading")).isNull();
    }

    @Test
    void supportDynamicLoadingIsFalseWhenAnyExtensionIsNotDynamicLoadable() throws IOException {
        var ith = new IntegrationTestHelper(tempDir, "8.14");
        initBuild(ith);
        Files.write(getBasePluginConfig().getBytes(StandardCharsets.UTF_8), ith.inProjectDir("build.gradle.kts"));
        writeJavaSource(ith, "com/example/plugin/NeverReloadable.java", """
                package com.example.plugin;

                @hudson.Extension(dynamicLoadable = jenkins.YesNoMaybe.NO)
                public class NeverReloadable {
                }
                """);
        writeJavaSource(ith, "com/example/plugin/AlwaysReloadable.java", """
                package com.example.plugin;

                @hudson.Extension(dynamicLoadable = jenkins.YesNoMaybe.YES)
                public class AlwaysReloadable {
                }
                """);

        ith.gradleRunner().withArguments("build").build();

        assertThat(manifestAttributes(ith).getValue("Support-Dynamic-Loading")).isEqualTo("false");
    }

    private static Attributes manifestAttributes(IntegrationTestHelper ith) throws IOException {
        File manifest = ith.inProjectDir("build/jpi/META-INF/MANIFEST.MF");
        assertThat(manifest).exists();
        return new Manifest(manifest.toURI().toURL().openStream()).getMainAttributes();
    }

    private static void writeJavaSource(IntegrationTestHelper ith, String relativePath, String source) throws IOException {
        var parent = relativePath.substring(0, relativePath.lastIndexOf('/'));
        ith.mkDirInProjectDir("src/main/java/" + parent);
        Files.write(source.getBytes(StandardCharsets.UTF_8), ith.inProjectDir("src/main/java/" + relativePath));
    }
}
