package org.jenkinsci.gradle.plugins.jpi.restricted

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.kohsuke.accmod.impl.ErrorListener
import org.kohsuke.accmod.impl.Location
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@CompileStatic
class InternalErrorListener implements ErrorListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalErrorListener)
    private final Map<String, Set<CallSite>> errors = [:]

    boolean hasErrors() {
        !errors.isEmpty()
    }

    String errorMessage() {
        errors.toSorted { a, b -> b.value.size() <=> a.value.size() }
                .inject('') { msg, err ->
            def lines = [
                    '',
                    '',
                    err.key,
                    "\tbut was used on ${pluralizeLines(err.value.size())}:",
            ]
            lines.addAll(err.value.toSorted { a, b -> a.className <=> b.className ?: a.line <=> b.line }
                    .collect { "\t\t- ${it.className}:${it.line}" as String })
            msg += lines.join(String.format('%n'))
            msg
        }
    }

    private static pluralizeLines(int count) {
        def suffix = count == 1 ? 'line' : 'lines'
        count + ' ' + suffix
    }

    /**
     * Accesses of Restricted APIs invoke this method.
     *
     * Rather than log invalid accesses right away, we aggregate them in order
     * to do some processing and present in a more consumable way.
     *
     * @param t throwable - always seems to be null
     * @param loc callsite
     * @param msg restricted class and error text
     */
    @Override
    void onError(Throwable t, Location loc, String msg) {
        def e = errors.computeIfAbsent(msg) { unused -> [] as Set }
        e.add(new CallSite(loc?.className, loc?.lineNumber))
        errors.put(msg, e)
    }

    /**
     * This never seems to be called.
     *
     * If this changes in the future, it's better to propagate this forward
     * to Gradle's logger rather than drop these messages.
     *
     * @param t throwable
     * @param loc callsite
     * @param msg warning message
     */
    @Override
    void onWarning(Throwable t, Location loc, String msg) {
        LOGGER.warn(loc?.toString() + ' ' + msg, t)
    }

    @Immutable
    static class CallSite {
        String className
        int line
    }
}
