package org.jenkinsci.gradle.plugins.jpi.internal;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.jenkinsci.gradle.plugins.jpi.core.PluginDeveloper;
import org.jenkinsci.gradle.plugins.jpi.core.PluginDeveloperSpec;

import java.util.LinkedList;
import java.util.List;

public class BackwardsCompatiblePluginDevelopers implements PluginDeveloperSpec {
    private final List<PluginDeveloper> developers = new LinkedList<>();
    private final ObjectFactory objects;
    private PluginDeveloper current;

    public BackwardsCompatiblePluginDevelopers(ObjectFactory objects) {
        this.objects = objects;
    }

    @Override
    public void developer(Action<? super PluginDeveloper> action) {
        current = objects.newInstance(PluginDeveloper.class);
        action.execute(current);
        developers.add(current);
        current = null;
    }

    public List<PluginDeveloper> getDevelopers() {
        return developers;
    }

    // this is because of nested closures?
    void setId(String s) {
        current.getId().set(s);
    }

    void id(String s) {
        setId(s);
    }

    void setName(String s) {
        current.getName().set(s);
    }

    void name(String s) {
        setName(s);
    }

    void setEmail(String s) {
        current.getEmail().set(s);
    }

    void email(String s) {
        setEmail(s);
    }
}
