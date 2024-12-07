Releasing the Gradle JPI Plugin
===============================

These are the instructions to release the Gradle JPI Plugin.


Prerequisites
-------------

Ensure you have your signing credentials in `~/.gradle/gradle.properties`:

    signing.keyId=24875D73
    signing.secretKeyRingFile=/Users/me/.gnupg/secring.gpg

You do not need to store your private key password there, the build script will ask for it. See
[The Signing Plugin](https://www.gradle.org/docs/current/userguide/signing_plugin.html) for details.


Steps
-----

This repo uses the [nebula.release plugin][nebula.release] to manage versions automatically.
For a candidate, use the `candidate` task.
For a final release, use the `final` task.

* Ensure you have the latest code: `git checkout master && git pull`
* Update `CHANGELOG.md`, set the release date
* Update the version in `README.md`
* Ensure everything is checked in: `git commit -S -am "releasing 0.6.0"`
* Build the code: `gradlew clean check install`
* Test the plugin with Jenkins plugin projects using it (e.g. https://github.com/jenkinsci/job-dsl-plugin)
* Deploy: `gradlew -Pjenkins.username=<my-username> -Pjenkins.password=<my-password> publishPluginMavenPublicationToJenkinsCommunityRepository`
* Publish to Gradle plugin portal: `gradlew publishPlugins`
* Update `CHANGELOG.md`, add the next version
* Close all resolved issues in JIRA: https://issues.jenkins-ci.org/secure/Dashboard.jspa?selectPageId=15444
* Send an email to jenkinsci-dev@googlegroups.com

[nebula.release]: https://github.com/nebula-plugins/nebula-release-plugin

