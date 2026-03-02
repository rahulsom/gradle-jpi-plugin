buildscript {
    dependencies {
        classpath(libs.nebula.release.plugin)
    }
}
plugins {
    alias(libs.plugins.distribution.sha)
}

allprojects {
    group = "org.jenkins-ci.tools"
    apply(plugin = "nebula.release")
}

subprojects {
    repositories {
        maven {
            url = uri("https://repo.jenkins-ci.org/public")
        }
        gradlePluginPortal()
    }

    plugins.withId("java") {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
    }
}
