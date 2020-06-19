package org.jenkinsci.plugins.ownership.config;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

public class ConfigMigrationTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @LocalData
    public void shouldMigrateDataFromPluginImpl() {
        OwnershipGlobalConfiguration.get();
    }
}
