package org.jenkinsci.gradle.plugins.jpi

import java.util.jar.JarEntry
import java.util.jar.JarInputStream

class JpiPublishingAndConsumptionTest extends IntegrationSpec {
    private File producerBuild
    private File consumerBuild

    def setup() {
        mkDirInProjectDir('producer')
        mkDirInProjectDir('consumer')
        def repo = mkDirInProjectDir('repo')
        touchInProjectDir('producer/settings.gradle') << 'rootProject.name = "producer"'
        touchInProjectDir('consumer/settings.gradle') << 'rootProject.name = "consumer"'
        producerBuild = touchInProjectDir('producer/build.gradle')
        consumerBuild = touchInProjectDir('consumer/build.gradle')
        producerBuild << """\
            plugins {
                id 'org.jenkins-ci.jpi'
                id 'maven-publish'
            }
            group = 'org'
            version = '1.0'
            publishing {
                repositories {
                    maven {
                        name 'test'
                        url '${path(repo)}'
                    }
                }
            }
            """.stripIndent()
        consumerBuild << """\
            plugins {
                id 'org.jenkins-ci.jpi'
            }
            repositories {
                maven {
                    url '${path(repo)}'
                }
            }
            tasks.create('runtime') {
                doLast {
                    configurations.runtimeClasspath.files.each { print "\${it.name}," }
                }
            }
            tasks.create('compile') {
                doLast {
                    configurations.compileClasspath.files.each { print "\${it.name},"  }
                }
            }
            tasks.create('jenkinsRuntime') {
                doLast {
                    configurations.runtimeClasspathJenkins.files.findAll {
                        it.name.endsWith('.jpi') || it.name.endsWith('.hpi') || it.name.endsWith('.war')
                    }.each { print "\${it.name}," }
                }
            }
            tasks.create('jenkinsTestRuntime') {
                doLast {
                    configurations.testRuntimeClasspathJenkins.files.findAll {
                        it.name.endsWith('.jpi') || it.name.endsWith('.hpi') || it.name.startsWith('jenkins-war-')
                    }.each { print "\${it.name}," }
                }
            }
            """.stripIndent()
    }

    def 'publishes compile, runtime and jenkins runtime variant'() {
        given:
        producerBuild << """\
            jenkinsPlugin {
                jenkinsVersion = '${TestSupport.RECENT_JENKINS_VERSION}'
            }
            dependencies {
                api 'org.jenkins-ci.plugins:credentials:1.9.4'
                implementation 'org.jenkins-ci.plugins:git:3.12.1'
                api 'org.apache.commons:commons-lang3:3.9'
                implementation 'org.apache.commons:commons-collections4:4.4'
            }
            """.stripIndent()
        publishProducer()

        when:
        consumerBuild << """
            jenkinsPlugin {
                jenkinsVersion = '1.580.1'
            }
            dependencies {
                implementation 'org:producer:1.0'
            }
        """

        then:
        resolveConsumer('compile') == JENKINS_CORE_DEPS + [
                'producer-1.0.jar',
                'credentials-1.9.4.jar',
                'commons-lang3-3.9.jar',
                'jcip-annotations-1.0.jar',
                'findbugs-annotations-1.3.9-1.jar',
                'jsr305-2.0.1.jar',
        ] as Set

        resolveConsumer('runtime') == [
                'producer-1.0.jar',
                'git-3.12.1.jar',
                'commons-collections4-4.4.jar',
                'git-client-2.7.7.jar',
                'jsch-0.1.54.1.jar',
                'ssh-credentials-1.13.jar',
                'credentials-2.1.18.jar',
                'commons-lang3-3.9.jar',
                'joda-time-2.9.5.jar',
                'scm-api-2.6.3.jar',
                'workflow-scm-step-2.7.jar',
                'workflow-step-api-2.20.jar',
                'structs-1.19.jar',
                'matrix-project-1.7.1.jar',
                'mailer-1.18.jar',
                'antlr4-runtime-4.5.jar',
                'symbol-annotation-1.19.jar',
                'org.eclipse.jgit.http.server-4.5.5.201812240535-r.jar',
                'org.eclipse.jgit.http.apache-4.5.5.201812240535-r.jar',
                'org.eclipse.jgit-4.5.5.201812240535-r.jar',
                'apache-httpcomponents-client-4-api-4.5.3-2.0.jar',
                'display-url-api-0.2.jar',
                'junit-1.3.jar',
                'script-security-1.13.jar',
                'org.abego.treelayout.core-1.0.1.jar',
                'JavaEWAH-0.7.9.jar',
                'slf4j-api-1.7.2.jar',
                'jsch-0.1.54.jar',
                'httpmime-4.5.3.jar',
                'fluent-hc-4.5.3.jar',
                'httpclient-cache-4.5.3.jar',
                'httpclient-4.5.3.jar',
                'groovy-sandbox-1.8.jar',
                'httpcore-4.4.6.jar',
                'commons-logging-1.2.jar',
                'commons-codec-1.9.jar',
        ] as Set

        resolveConsumer('jenkinsRuntime') == [
                'producer-1.0.hpi',
                'git-3.12.1.hpi',
                'git-client-2.7.7.hpi',
                'jsch-0.1.54.1.hpi',
                'ssh-credentials-1.13.hpi',
                'credentials-2.1.18.hpi',
                'scm-api-2.6.3.hpi',
                'workflow-scm-step-2.7.hpi',
                'workflow-step-api-2.20.hpi',
                'structs-1.19.hpi',
                'matrix-project-1.7.1.hpi',
                'mailer-1.18.hpi',
                'apache-httpcomponents-client-4-api-4.5.3-2.0.hpi',
                'display-url-api-0.2.hpi',
                'junit-1.3.hpi',
                'script-security-1.13.hpi',
        ] as Set

        resolveConsumer('jenkinsTestRuntime') == [
                'producer-1.0.hpi',
                'git-3.12.1.hpi',
                'git-client-2.7.7.hpi',
                'jsch-0.1.54.1.hpi',
                'ssh-credentials-1.13.hpi',
                'credentials-2.1.18.hpi',
                'scm-api-2.6.3.hpi',
                'workflow-scm-step-2.7.hpi',
                'workflow-step-api-2.20.hpi',
                'structs-1.19.hpi',
                'matrix-project-1.7.1.hpi',
                'mailer-1.18.hpi',
                'apache-httpcomponents-client-4-api-4.5.3-2.0.hpi',
                'display-url-api-0.2.hpi',
                'junit-1.3.hpi',
                'script-security-1.13.hpi',
                'jenkins-war-1.580.1.war',
        ] as Set
    }

    def 'publishes feature variant with compile, runtime and jenkins runtime variant'() {
        given:
        producerBuild << """\
            jenkinsPlugin {
                jenkinsVersion = '1.580.1'
            }
            java {
                registerFeature('credentials') {
                    usingSourceSet(sourceSets.main)
                }
            }
            dependencies {
                implementation 'org.jenkins-ci.plugins:ant:1.2'
                implementation 'org.apache.commons:commons-lang3:3.9'
                credentialsImplementation 'org.jenkins-ci.plugins:credentials:1.9.4'
                credentialsImplementation 'org.apache.commons:commons-collections4:4.4'
            }
            """.stripIndent()
        publishProducer()

        when:
        consumerBuild << """
            jenkinsPlugin {
                jenkinsVersion = '1.580.1'
            }
            dependencies {
                implementation 'org:producer:1.0'
            }
        """

        then:
        resolveConsumer('compile') == JENKINS_CORE_DEPS + [ 'producer-1.0.jar' ] as Set
        resolveConsumer('runtime') == [
                'producer-1.0.jar',
                'ant-1.2.jar',
                'commons-lang3-3.9.jar' ] as Set
        resolveConsumer('jenkinsRuntime') == [ 'producer-1.0.hpi', 'ant-1.2.hpi' ] as Set
        resolveConsumer('jenkinsTestRuntime') == [
                'producer-1.0.hpi',
                'ant-1.2.hpi',
                'jenkins-war-1.580.1.war',
        ] as Set

        when:
        consumerBuild << """
            dependencies {
                implementation('org:producer:1.0') {
                    capabilities { requireCapability('org:producer-credentials') }
                }
            }
        """

        then:
        resolveConsumer('compile') == JENKINS_CORE_DEPS + [ 'producer-1.0.jar' ] as Set
        resolveConsumer('runtime') == [
                'producer-1.0.jar',
                'ant-1.2.jar',
                'commons-lang3-3.9.jar',
                'commons-collections4-4.4.jar',
                'credentials-1.9.4.jar',
                'jcip-annotations-1.0.jar',
                'findbugs-annotations-1.3.9-1.jar',
                'jsr305-1.3.9.jar' ] as Set
        resolveConsumer('jenkinsRuntime') ==
                [ 'producer-1.0.hpi', 'ant-1.2.hpi', 'credentials-1.9.4.hpi' ] as Set
        resolveConsumer('jenkinsTestRuntime') ==
                [ 'producer-1.0.hpi',
                  'ant-1.2.hpi',
                  'credentials-1.9.4.hpi',
                  'jenkins-war-1.580.1.war',
                ] as Set

        manifestEntry('consumer', 'Plugin-Dependencies') ==
                'producer:1.0,credentials:1.9.4'
        manifestEntry('producer', 'Plugin-Dependencies') ==
                'credentials:1.9.4;resolution:=optional,ant:1.2'
        packagedJars('consumer') ==
                [ 'consumer.jar' ] as Set
        packagedJars('producer') ==
                [ 'producer-1.0.jar', 'commons-lang3-3.9.jar', 'commons-collections4-4.4.jar' ] as Set
    }

    def 'has Jenkins core dependencies if a Jenkins version is configured'() {
        given:
        producerBuild << """
            jenkinsPlugin {
                jenkinsVersion = '${TestSupport.RECENT_JENKINS_VERSION}'
            }
            """.stripIndent()
        publishProducer()

        when:
        consumerBuild << """
            jenkinsPlugin {
                jenkinsVersion = '1.580.1'
            }
            dependencies {
                implementation 'org:producer:1.0'
            }
        """

        then:
        resolveConsumer('compile') == JENKINS_CORE_DEPS + [ 'producer-1.0.jar' ] as Set
        resolveConsumer('runtime') == [ 'producer-1.0.jar' ] as Set
        resolveConsumer('jenkinsRuntime') == [ 'producer-1.0.hpi' ] as Set
        resolveConsumer('jenkinsTestRuntime') == [ 'producer-1.0.hpi', 'jenkins-war-1.580.1.war' ] as Set
    }

    private void publishProducer() {
        gradleRunner().withProjectDir(producerBuild.parentFile).forwardOutput().
                withArguments('publishMavenJpiPublicationToTestRepository', '-s').build()
    }

    private Set<String> resolveConsumer(String resolveTask) {
        def result = gradleRunner().withProjectDir(consumerBuild.parentFile).forwardOutput().
                withArguments(resolveTask, '-q').build()
        result.output.split(',').findAll { !it.isBlank() }
    }

    private String manifestEntry(String jpiName, String key) {
        new JarInputStream(jpi(jpiName).newInputStream()).manifest
                .mainAttributes.find { it.key.toString() == key }.value.toString()
    }

    private Set<String> packagedJars(String jpiName) {
        def jpi = new JarInputStream(jpi(jpiName).newInputStream())
        Set<String> jars = []
        JarEntry entry
        while ((entry = jpi.nextJarEntry) != null) {
            def path = entry.name
            if (path.endsWith('.jar')) {
                jars.add(path.split('/')[2])
            }
        }
        jars
    }

    private File jpi(String name) {
        def buildRoot = inProjectDir(name)
        def jpi = new File(buildRoot, "build/libs/${name}.hpi")
        if (!existsRelativeToProjectDir("${name}/build/libs/${name}.hpi")) {
            gradleRunner().withProjectDir(buildRoot).forwardOutput().
                    withArguments('jpi').build()
        }
        jpi
    }

    private static String path(File file) {
        file.absolutePath.replaceAll('\\\\', '/')
    }

    private static final JENKINS_CORE_DEPS = [
        'jenkins-core-1.580.1.jar',
        'annotations-1.0.0.jar',
        'servlet-api-2.4.jar',
        'icon-set-1.0.3.jar',
        'cli-1.580.1.jar',
        'remoting-2.47.jar',
        'version-number-1.1.jar',
        'crypto-util-1.1.jar',
        'jtidy-4aug2000r7-dev-hudson-1.jar',
        'guice-4.0-beta-no_aop.jar',
        'jna-posix-1.0.3.jar',
        'jnr-posix-3.0.1.jar',
        'trilead-putty-extension-1.2.jar',
        'trilead-ssh2-build217-jenkins-5.jar',
        'stapler-groovy-1.231.jar',
        'stapler-jrebel-1.231.jar',
        'windows-package-checker-1.0.jar',
        'stapler-adjunct-zeroclipboard-1.3.5-1.jar',
        'stapler-adjunct-timeline-1.4.jar',
        'stapler-adjunct-codemirror-1.3.jar',
        'bridge-method-annotation-1.13.jar',
        'stapler-jelly-1.231.jar',
        'stapler-1.231.jar',
        'json-lib-2.4-jenkins-2.jar',
        'commons-httpclient-3.1.jar',
        'args4j-2.0.23.jar',
        'bytecode-compatibility-transformer-1.5.jar',
        'access-modifier-annotation-1.4.jar',
        'annotation-indexer-1.7.jar',
        'task-reactor-1.4.jar',
        'localizer-1.10.jar',
        'antlr-2.7.6.jar',
        'xstream-1.4.7-jenkins-1.jar',
        'jfreechart-1.0.9.jar',
        'ant-1.8.3.jar',
        'commons-fileupload-1.3.1-jenkins-1.jar',
        'commons-io-2.4.jar',
        'acegi-security-1.0.7.jar',
        'ezmorph-1.0.6.jar',
        'commons-lang-2.6.jar',
        'commons-digester-2.1.jar',
        'commons-jelly-tags-xml-1.1.jar',
        'commons-jelly-1.1-jenkins-20120928.jar',
        'commons-beanutils-1.8.3.jar',
        'mail-1.4.4.jar',
        'activation-1.1.1-hudson-1.jar',
        'jaxen-1.1-beta-11.jar',
        'commons-jelly-tags-fmt-1.0.jar',
        'commons-jelly-tags-define-1.0.1-hudson-20071021.jar',
        'commons-jexl-1.1-jenkins-20111212.jar',
        'groovy-all-1.8.9.jar',
        'jline-0.9.94.jar',
        'jansi-1.9.jar',
        'spring-webmvc-2.5.6.SEC03.jar',
        'spring-aop-2.5.6.SEC03.jar',
        'spring-jdbc-1.2.9.jar',
        'spring-context-support-2.5.6.SEC03.jar',
        'spring-web-2.5.6.SEC03.jar',
        'spring-dao-1.2.9.jar',
        'spring-context-2.5.6.SEC03.jar',
        'spring-beans-2.5.6.SEC03.jar',
        'spring-core-2.5.6.SEC03.jar',
        'xpp3-1.1.4c.jar',
        'jstl-1.1.0.jar',
        'commons-discovery-0.4.jar',
        'commons-logging-1.1.3.jar',
        'txw2-20110809.jar',
        'commons-collections-3.2.1.jar',
        'winp-1.22.jar',
        'memory-monitor-1.8.jar',
        'wstx-asl-3.2.9.jar',
        'jmdns-3.4.0-jenkins-3.jar',
        'akuma-1.9.jar',
        'libpam4j-1.6.jar',
        'libzfs-0.5.jar',
        'jna-3.4.0.jar',
        'embedded_su4j-1.1.jar',
        'sezpoz-1.9.jar',
        'j-interop-2.0.6-kohsuke-1.jar',
        'robust-http-client-1.2.jar',
        'commons-codec-1.8.jar',
        'jbcrypt-0.3m.jar',
        'guava-14.0.jar',
        'jzlib-1.1.3.jar',
        'constant-pool-scanner-1.2.jar',
        'javax.inject-1.jar',
        'aopalliance-1.0.jar',
        'cglib-3.0.jar',
        'jnr-ffi-1.0.7.jar',
        'asm-util-4.0.jar',
        'jnr-constants-0.8.5.jar',
        'asm5-5.0.1.jar',
        'jcommon-1.0.12.jar',
        'ant-launcher-1.8.3.jar',
        'oro-2.0.8.jar',
        'junit-3.8.1.jar',
        'stax-api-1.0-2.jar',
        'relaxngDatatype-20020414.jar',
        'stax-api-1.0.1.jar',
        'j-interopdeps-2.0.6-kohsuke-1.jar',
        'jsr305-2.0.1.jar',
        'asm-commons-4.0.jar',
        'asm-analysis-4.0.jar',
        'asm-tree-4.0.jar',
        'asm-4.0.jar',
        'jffi-1.2.7.jar',
        'jnr-x86asm-1.0.2.jar',
        'dom4j-1.6.1-jenkins-4.jar',
        'javax.annotation-api-1.2.jar',
        'tiger-types-1.3.jar',
        'jdom-1.0.jar',
        'xom-1.0b3.jar',
        'jcifs-1.2.19.jar',
        'xmlParserAPIs-2.6.1.jar',
        'icu4j-2.6.1.jar',
        'xalan-2.6.0.jar',
        'tagsoup-0.9.7.jar',
    ] as Set
}
