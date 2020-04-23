package org.jenkinsci.gradle.plugins.jpi.restricted

import groovy.transform.CompileStatic
import org.apache.maven.plugin.logging.Log
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@CompileStatic
class InternalMavenLoggingBridge implements Log {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalMavenLoggingBridge)
    public static final String EXCEPTION_MESSAGE = 'An error occurred'

    @Override
    boolean isDebugEnabled() {
        LOGGER.isDebugEnabled()
    }

    @Override
    void debug(CharSequence content) {
        LOGGER.debug(content as String)
    }

    @Override
    void debug(CharSequence content, Throwable error) {
        LOGGER.debug(content as String, error)
    }

    @Override
    void debug(Throwable error) {
        LOGGER.debug(EXCEPTION_MESSAGE, error)
    }

    @Override
    boolean isInfoEnabled() {
        LOGGER.isInfoEnabled()
    }

    @Override
    void info(CharSequence content) {
        LOGGER.info(content as String)
    }

    @Override
    void info(CharSequence content, Throwable error) {
        LOGGER.info(content as String, error)
    }

    @Override
    void info(Throwable error) {
        LOGGER.info(EXCEPTION_MESSAGE, error)
    }

    @Override
    boolean isWarnEnabled() {
        LOGGER.isWarnEnabled()
    }

    @Override
    void warn(CharSequence content) {
        LOGGER.warn(content as String)
    }

    @Override
    void warn(CharSequence content, Throwable error) {
        LOGGER.warn(content as String, error)
    }

    @Override
    void warn(Throwable error) {
        LOGGER.warn(EXCEPTION_MESSAGE, error)
    }

    @Override
    boolean isErrorEnabled() {
        LOGGER.isErrorEnabled()
    }

    @Override
    void error(CharSequence content) {
        LOGGER.error(content as String)
    }

    @Override
    void error(CharSequence content, Throwable error) {
        LOGGER.error(content as String, error)
    }

    @Override
    void error(Throwable error) {
        LOGGER.error(EXCEPTION_MESSAGE, error)
    }
}
