package org.jenkinsci.gradle.plugins.jpi.internal;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.jenkinsci.gradle.plugins.jpi.core.PluginDeveloper;
import org.jenkinsci.gradle.plugins.jpi.core.PluginLicense;
import org.jenkinsci.gradle.plugins.jpi.core.PluginLicenseSpec;

import java.util.LinkedList;
import java.util.List;

public class BackwardsCompatiblePluginLicenses implements PluginLicenseSpec {
    private final List<PluginLicense> licenses = new LinkedList<>();
    private final ObjectFactory objects;
    private PluginLicense current;

    public BackwardsCompatiblePluginLicenses(ObjectFactory objects) {
        this.objects = objects;
    }

    @Override
    public void license(Action<? super PluginLicense> action) {
        current = objects.newInstance(PluginLicense.class);
        action.execute(current);
        licenses.add(current);
        current = null;
    }

    public List<PluginLicense> getLicenses() {
        return licenses;
    }

    void setName(String s) {
        current.getName().set(s);
    }

    void name(String s) {
        setName(s);
    }

    void setUrl(String s) {
        current.getUrl().set(s);
    }

    void url(String s) {
        setUrl(s);
    }

    void setDistribution(String s) {
        current.getDistribution().set(s);
    }

    void distribution(String s) {
        setDistribution(s);
    }

    void setComments(String s) {
        current.getComments().set(s);
    }

    void comments(String s) {
        setComments(s);
    }
}
