package org.jenkinsci.gradle.plugins.jpi.support

import spock.lang.Specification

class SettingsFileSpec extends Specification {
    Indenter indenter = FourSpaceIndenter.create()

    def 'should create with given root name'() {
        expect:
        SettingsFile.builder()
                .withRootProjectName('orange')
                .build()
                .emit(indenter) == '''\
                rootProject.name = 'orange'
                '''.stripIndent()
    }

}
