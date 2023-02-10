import okio.buffer
import okio.sink
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import java.nio.file.StandardOpenOption

buildscript {
    dependencies {
        classpath("com.squareup.okhttp3:okhttp:4.10.0")
    }
}
plugins {
    groovy
    `maven-publish`
    `kotlin-dsl`
    signing
    codenarc
    id("com.gradle.plugin-publish") version "1.0.0"
    `java-gradle-plugin`
    id("com.github.sghill.distribution-sha") version "0.4.0"
}
plugins.apply(internal.DependenciesComparisonPlugin::class.java)

repositories {
    maven {
        url = uri("https://repo.jenkins-ci.org/public")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

val sezpoz = "net.java.sezpoz:sezpoz:1.13"

dependencies {
    compileOnly("org.kohsuke:access-modifier-checker:1.21")
    annotationProcessor(sezpoz)
    implementation(gradleApi())
    compileOnly("org.eclipse.jgit:org.eclipse.jgit:5.13.1.202206130422-r")
    testImplementation("org.eclipse.jgit:org.eclipse.jgit:5.13.1.202206130422-r")
    compileOnly("com.squareup:javapoet:1.13.0") {
        because("used for GenerateTestTask")
    }
    compileOnly("org.jenkins-ci.main:jenkins-test-harness:${stringProp("deps.jenkinsTestHarness")}") {
        because("used for GenerateTestTask")
        isTransitive = false
    }
    compileOnly("org.jvnet.localizer:maven-localizer-plugin:1.24")
    implementation(sezpoz)
    implementation(localGroovy())
    testAnnotationProcessor(sezpoz)
    testCompileOnly("junit:junit:4.13") {
        because("used for generated tests with javapoet")
    }
    testImplementation("org.spockframework:spock-core:2.1-groovy-2.5")
    testImplementation("org.xmlunit:xmlunit-core:2.8.3")
    testImplementation("org.apache.commons:commons-text:1.10.0")
    testImplementation("com.squareup:javapoet:1.13.0")
    testImplementation("org.kohsuke:access-modifier-checker:1.21")
    testImplementation("org.jenkins-ci.main:jenkins-core:2.263.3") {
        exclude(module = "groovy-all")
    }
    testImplementation(platform("org.junit:junit-bom:5.8.1"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.assertj:assertj-core:3.21.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            pom {
                name.set("Gradle JPI Plugin")
                description.set("The Gradle JPI plugin is a Gradle plugin for building Jenkins plugins")
                url.set("http://github.com/jenkinsci/gradle-jpi-plugin")
                scm {
                    url.set("https://github.com/jenkinsci/gradle-jpi-plugin")
                }
                licenses {
                    license {
                        name.set("Apache 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("abayer")
                        name.set("Andrew Bayer")
                    }
                    developer {
                        id.set("kohsuke")
                        name.set("Kohsuke Kawaguchi")
                    }
                    developer {
                        id.set("daspilker")
                        name.set("Daniel Spilker")
                    }
                    developer {
                        id.set("sghill")
                        name.set("Steve Hill")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            val path = if (version.toString().endsWith("SNAPSHOT")) "snapshots" else "releases"
            name = "JenkinsCommunity"
            url = uri("https://repo.jenkins-ci.org/${path}")
            credentials {
                username = project.stringProp("jenkins.username")
                password = project.stringProp("jenkins.password")
            }
        }
    }
}

signing {
    useGpgCmd()
    setRequired { setOf("jenkins.username", "jenkins.password").all { project.hasProperty(it) } }
}

tasks.addRule("Pattern: testGradle<ID>") {
    val taskName = this
    if (!taskName.startsWith("testGradle")) return@addRule
    val task = tasks.register(taskName)
    for (javaVersion in listOf(8, 11)) {
        val javaSpecificTask = tasks.register<Test>("${taskName}onJava${javaVersion}") {
            val gradleVersion = taskName.substringAfter("testGradle")
            systemProperty("gradle.under.test", gradleVersion)
            setTestNameIncludePatterns(listOf("*IntegrationSpec"))
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(javaVersion))
            })
        }
        task.configure {
            dependsOn(javaSpecificTask)
        }
    }
}

val integrationTestOnJava11 = tasks.register<Test>("integrationTestOnJava11") {
    systemProperty("gradle.under.test", "7.5.1")
    setTestNameIncludePatterns(listOf("*IntegrationSpec"))
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(11))
    })
}
tasks.check {
    dependsOn(integrationTestOnJava11)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = FULL
    }
}

codenarc {
    toolVersion = "1.1"
    configFile = file("config/codenarc/rules.groovy")
}

tasks.codenarcTest {
    configFile = file("config/codenarc/rules-test.groovy")
}

group = "org.jenkins-ci.tools"
description = "Gradle plugin for building and packaging Jenkins plugins"

gradlePlugin {
    plugins {
        create("pluginMaven") {
            id = "org.jenkins-ci.jpi"
            implementationClass = "org.jenkinsci.gradle.plugins.jpi.JpiPlugin"
            displayName = "A plugin for building Jenkins plugins"
        }
    }
}

pluginBundle {
    website = "https://wiki.jenkins-ci.org/display/JENKINS/Gradle+JPI+Plugin"
    vcsUrl = "https://github.com/jenkinsci/gradle-jpi-plugin"
    description = "A plugin for building Jenkins plugins"
    tags = listOf("jenkins")
}

fun Project.stringProp(named: String): String? = findProperty(named) as String?

tasks.register("shadeLatestVersionNumber") {
    doLast {
        val response = okhttp3.OkHttpClient().newCall(okhttp3.Request.Builder().get()
                .url("https://raw.githubusercontent.com/jenkinsci/lib-version-number/master/src/main/java/hudson/util/VersionNumber.java")
                .build())
                .execute()
        if (!response.isSuccessful) {
            throw GradleException("${response.code} attempting to fetch latest hudson.util.VersionNumber")
        }
        val convention = project.convention.getPlugin(JavaPluginConvention::class)
        val main = convention.sourceSets.getByName("main")
        val srcMainJava = main.java.srcDirs.single().toPath()
        val dest = srcMainJava.resolve("shaded/hudson/util/VersionNumber.java")
        val sink = dest.sink(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).buffer()
        response.body?.use {
            it.source().use { input ->
                sink.use { output ->
                    output.writeAll(input)
                }
            }
        }
    }
}
