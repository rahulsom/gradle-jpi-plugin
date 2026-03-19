# Legacy JPI plugin

This document preserves the `org.jenkins-ci.jpi` documentation for older Gradle builds.
For Gradle 8 and newer, use `org.jenkins-ci.jpi2` instead.
If you are moving an existing build forward, see [migrating-to-jpi2.md](migrating-to-jpi2.md).

## Compatibility with Gradle versions

The latest version of the legacy JPI plugin requires Gradle 7.1 or later.
For Gradle versions 6.3 through 6.9.x, use version `0.50.0`.
For Gradle versions 6.0 through 6.2.1, use version `0.46.0`.
For Gradle versions 4.x or 5.x, use version `0.38.0`.
Parts of the legacy plugin do not work on Gradle 8+.

## Configuration

Add the following to your `build.gradle`.

```groovy
plugins {
  id 'org.jenkins-ci.jpi' version '<current-version>'
}

group = 'org.jenkins-ci.plugins'
version = '1.2.0-SNAPSHOT'
description = 'A description of your plugin'

jenkinsPlugin {
    // version of Jenkins core this plugin depends on, must be 1.420 or later
    jenkinsVersion = '1.420'

    // ID of the plugin, defaults to the project name without trailing '-plugin'
    shortName = 'hello-world'

    // human-readable name of plugin
    displayName = 'Hello World plugin built with Gradle'

    // URL for plugin on Jenkins wiki or elsewhere
    url = 'http://wiki.jenkins-ci.org/display/JENKINS/SomePluginPage'

    // plugin URL on GitHub, optional
    gitHubUrl = 'https://github.com/jenkinsci/some-plugin'

    // scm tag eventually set in the published pom, optional
    scmTag = 'v1.0.0'

    // use the plugin class loader before the core class loader, defaults to false
    pluginFirstClassLoader = true

    // optional list of package prefixes that your plugin doesn't want to see from core
    maskClasses = 'groovy.grape org.apache.commons.codec'

    // optional version number from which this plugin release is configuration-compatible
    compatibleSinceVersion = '1.1.0'

    // set the directory from which the development server will run, defaults to 'work'
    workDir = file('/tmp/jenkins')

    // URL used to deploy the plugin, defaults to the value shown
    // the system property 'jpi.repoUrl' can be used to override this option
    repoUrl = 'https://repo.jenkins-ci.org/releases'

    // URL used to deploy snapshots of the plugin, defaults to the value shown
    // the system property 'jpi.snapshotRepoUrl' can be used to override this option
    snapshotRepoUrl = 'https://repo.jenkins-ci.org/snapshots'

    // enable injection of additional tests for checking the syntax of Jelly and other things
    disabledTestInjection = false

    // the output directory for the localizer task relative to the project root, defaults to the value shown
    localizerOutputDir = "${project.buildDir}/generated-src/localizer"

    // disable configuration of Maven Central, the local Maven cache and the Jenkins Maven repository, defaults to true
    configureRepositories = false

    // skip configuration of publications and repositories for the Maven Publishing plugin, defaults to true
    configurePublishing = false

    // plugin file extension, either 'jpi' or 'hpi', defaults to 'hpi'
    fileExtension = 'hpi'

    // the developers section is optional, and corresponds to the POM developers section
    developers {
        developer {
            id 'abayer'
            name 'Andrew Bayer'
            email 'andrew.bayer@gmail.com'
        }
    }

    // the licenses section is optional, and corresponds to the POM licenses section
    licenses {
        license {
            name 'Apache License, Version 2.0'
            url 'https://www.apache.org/licenses/LICENSE-2.0.txt'
            distribution 'repo'
            comments 'A business-friendly OSS license'
        }
    }

    // Git based version generation is optional
    gitVersion {
        // Don't fail if changes are not committed (default: false)
        allowDirty = true
        // Customize version format (default: %d.%s where %d is the commit depth, %s the abbreviated sha)
        versionFormat = 'rc-%d.%s'
        // Sanitize the hash according to Jenkins requirements (default: false)
        sanitize = true
        // Customize abbreviated sha length (default: 12)
        abbrevLength = 10
        // Customize git root (default: project directory)
        gitRoot = file('/some/external/git/repo')
    }

    // "Incrementals" custom repository (default: https://repo.jenkins-ci.org/incrementals)
    incrementalsRepoUrl = 'https://custom'

    // Enable quality check plugins
    enableSpotBugs()
    enableCheckstyle()
    enableJacoco()
}
```

Add the `jenkinsPlugin { ... }` section before any additional repositories are defined in your build script.

## Dependencies on other Jenkins plugins

If your plugin depends on other Jenkins plugins, you can use the same configurations as in Gradle's `java-library` plugin.
See the Gradle documentation for details on the difference between `api` and `implementation`.
For optional dependencies, you can use Gradle feature variants.
The legacy JPI plugin figures out whether each dependency is a Jenkins plugin or a plain Java library and processes it accordingly.
Plain Java libraries are packaged into the resulting `hpi` or `jpi` archive.
The additional `jenkinsServer` configuration can be used to install extra plugins for the `server` task.

```groovy
java {
    registerFeature('ant') {
        usingSourceSet(sourceSets.main)
    }
}

dependencies {
    implementation 'org.jenkinsci.plugins:git:1.1.15'
    api 'org.jenkins-ci.plugins:credentials:1.9.4'

    antImplementation 'org.jenkins-ci.plugins:ant:1.2'
    testImplementation 'org.jenkins-ci.main:maven-plugin:1.480'
    jenkinsServer 'org.jenkins-ci.plugins:ant:1.2'
}
```

## Usage

- `gradle jpi` builds the Jenkins plugin archive in the build directory.
- `gradle publishToMavenLocal` builds the plugin and installs it into the local Maven repository.
- `gradle publish` deploys the plugin to the Jenkins Maven repository.
- `gradle server` starts a local Jenkins instance with the plugin preinstalled for testing and debugging.

## Running Jenkins locally

The `server` task creates an HPL file, installs plugins, and starts Jenkins on port `8080`.
The server runs in the foreground.
Plugins added to the `api`, `implementation`, `runtimeOnly`, and `jenkinsServer` configurations are installed to `${jenkinsPlugin.workDir}/plugins`.

### Default system properties

Jenkins starts with these system properties set to `true`.

- `stapler.trace`
- `stapler.jelly.noCache`
- `debug.YUI`
- `hudson.Main.development`

### Customizing port

Jenkins starts on port `8080` by default.
Change the port with `--port` or with the task property.

```shell
./gradlew server --port=7000
```

```groovy
tasks.named('server').configure {
    it.port.set(7000)
}
```

### Customizing further

The `server` task accepts a `JavaExecSpec`, which allows extensive customization.

```groovy
tasks.named('server').configure {
    execSpec {
        systemProperty 'some.property', 'true'
        environment 'SOME_ENV_VAR', 'HelloWorld'
        maxHeapSize = '2g'
    }
}
```

### Debugging

Add `--debug-jvm` to start Jenkins suspended with a debug port of `5005`.
Customize those debug options through the `execSpec` block.

```shell
./gradlew server --debug-jvm
```

```groovy
tasks.named('server').configure {
    execSpec {
        debugOptions {
            port.set(6000)
            suspend.set(false)
        }
    }
}
```

### Additional server dependencies

Add additional dependencies for the `server` task classpath to the `jenkinsServerRuntimeOnly` configuration.
This is useful for alternative logging implementations.

## Checking for restricted APIs

Starting with `0.41.0`, the plugin checks for use of `@Restricted` types, methods, and fields.
This behavior warns by default.
Set `ignoreFailures` to `false` if you want the build to fail instead.

```groovy
tasks.named('checkAccessModifier').configure {
    ignoreFailures.set(false)
}
```

## Disabling timestamped `-SNAPSHOT` versions

By default, `generateJenkinsManifest` appends the current timestamp and username to `-SNAPSHOT` plugin versions.
Disable that behavior if you need reproducible outputs and better task cacheability.

```groovy
tasks.named('generateJenkinsManifest').configure {
    dynamicSnapshotVersion.set(false)
}
```

## Using Git based versioning

The plugin registers a `generateGitVersion` task that writes a Git-based version to a text file.
This is commonly used on `ci.jenkins.io` by generating the version first and then building with `-Pversion=${versionFile.readLines()[0]}`.
See the configuration example above to customize the generation.

## Using the Jenkins incrementals repository

The plugin defines `https://repo.jenkins-ci.org/incrementals/` for local builds.
The `publish` task does not publish there by default.
Call the `publish*PublicationToJenkinsIncrementalsRepository` tasks separately when you want to publish incrementals.
Use `incrementalsRepoUrl` to point at a different repository.

## Enabling quality checks

The legacy plugin can enable SpotBugs, Checkstyle, and JaCoCo with helper methods.
When enabled, the plugin configures those tools with Jenkins-friendly defaults.

```groovy
jenkinsPlugin {
    enableSpotBugs()
    enableCheckstyle()
    enableJacoco()
}
```

## Disabling SHA256 and SHA512 checksums when releasing

Gradle may try to upload SHA-256 and SHA-512 checksum files during `gradle publish`.
The public Jenkins artifact repository does not currently support those uploads.
Pass `-Dorg.gradle.internal.publish.checksums.insecure` or set `org.gradle.internal.publish.checksums.insecure=true` in `gradle.properties` to disable them.

## Gradle 4+

Gradle 4 changed the default layout of compiled classes directories.
That change breaks how SezPoz discovers `@Extension` annotations when Java and Groovy sources are split across separate output folders.
If you combine Java and Groovy code and both provide Jenkins extensions, either use joint compilation or force Gradle to use the older shared classes directory layout.
