buildscript {
    dependencies {
        classpath(libs.nebula.release.plugin)
    }
}
plugins {
    alias(libs.plugins.distribution.sha)
    alias(libs.plugins.test.logger) apply false
    alias(libs.plugins.test.retry) apply false
    alias(libs.plugins.spotless)
}

allprojects {
    group = "org.jenkins-ci.tools"
    plugins.apply("nebula.release")
}

repositories {
    mavenCentral()
}

subprojects {
    repositories {
        maven {
            url = uri("https://repo.jenkins-ci.org/public")
            mavenContent {
                excludeGroup("commons-io")
                excludeGroup("org.apache.commons")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }

    plugins.withId("java") {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
        plugins.apply("com.adarshr.test-logger")
        plugins.apply("org.gradle.test-retry")
        tasks.withType<Test>().configureEach {
            retry {
                // Integration tests launch nested Gradle builds and Jenkins JVMs, which occasionally
                // trip over transient Gradle-internal races on CI runners. Locally, failures should
                // surface immediately so they get investigated rather than hidden.
                if (providers.environmentVariable("CI").isPresent) {
                    maxRetries.set(2)
                    maxFailures.set(5)
                }
            }
        }
    }
}

spotless {
    java {
        palantirJavaFormat()
        target("**/*.java")
    }
    kotlin {
        ktlint()
        target("**/*.kt")
    }
    kotlinGradle {
        ktlint()
        target("**/*.gradle.kts")
    }
    yaml {
        prettier()
        target("**/*.yaml", "**/*.yml")
    }
    json {
        prettier()
        target("**/*.json")
    }
}