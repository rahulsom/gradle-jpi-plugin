package org.jenkinsci.gradle.plugins.jpi.restricted

class RestrictedApiException extends RuntimeException {
    RestrictedApiException() {
        super('Restricted APIs were detected - see https://tiny.cc/jenkins-restricted')
    }
}
