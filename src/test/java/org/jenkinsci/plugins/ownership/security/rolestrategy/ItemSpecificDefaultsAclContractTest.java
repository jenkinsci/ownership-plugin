/*
 * The MIT License
 *
 * Copyright 2026
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.ownership.security.rolestrategy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerJobAction;
import com.synopsys.arc.jenkins.plugins.ownership.security.itemspecific.ItemSpecificSecurity;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.User;
import hudson.remoting.Callable;
import hudson.security.ACL;
import hudson.security.Permission;
import java.io.Serial;
import java.util.HashMap;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import org.jenkinsci.plugins.ownership.model.folders.FolderOwnershipAction;
import org.jenkinsci.plugins.ownership.model.folders.FolderOwnershipHelper;
import org.jenkinsci.plugins.ownership.security.folderspecific.FolderSpecificSecurity;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.jenkinsci.remoting.RoleChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Global defaults are templates for UI/restore; ACL applies only a locally configured matrix.
 */
@WithJenkins
class ItemSpecificDefaultsAclContractTest {

    private JenkinsRule r;

    private FreeStyleProject job;

    private Folder folder;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        r = rule;
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        User.get("bob", true);

        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withDefaultJobsSecurity(defaultJobSecurity(Item.BUILD, "bob"))
                .withDefaultFoldersSecurity(defaultFolderSecurity(Item.BUILD, "bob"))
                .configure();

        job = r.createFreeStyleProject("defaults-job");
        JobOwnerHelper.setOwnership(job, new OwnershipDescription(true, "admin", null));

        folder = r.jenkins.createProject(Folder.class, "defaults-folder");
        FolderOwnershipHelper.setOwnership(folder, new OwnershipDescription(true, "admin", null));
    }

    @Test
    void job_defaultsOnly_aclDeniesWhenLocalNotConfigured() throws Exception {
        OwnershipBasedSecurityTestHelper.setupPlainItemSpecificMacroForUser(r.jenkins, "bob");

        assertFalse(JobOwnerHelper.Instance.hasItemSpecificPermission(job, "bob", Item.BUILD));
        assertThat(hasPermission("bob", job, Item.BUILD), equalTo(false));
    }

    @Test
    void job_defaultsOnly_uiStillShowsDefaults() throws Exception {
        JobOwnerJobAction action = job.getAction(JobOwnerJobAction.class);
        assertThat(action, notNullValue());
        assertFalse(action.hasItemSpecificSecurity());
        assertThat(action.getItemSpecificSecurity(), notNullValue());
    }

    @Test
    void job_afterRestoreDefaults_aclAppliesClonedLocal() throws Exception {
        OwnershipBasedSecurityTestHelper.setupPlainItemSpecificMacroForUser(r.jenkins, "bob");
        ItemSpecificSecurity defaults = OwnershipPlugin.getInstance().getDefaultJobsSecurity();
        assertThat(defaults, notNullValue());

        JobOwnerHelper.setProjectSpecificSecurity(job, defaults.clone());

        assertTrue(JobOwnerHelper.getOwnerProperty(job).hasItemSpecificSecurity());
        assertThat(hasPermission("bob", job, Item.BUILD), equalTo(true));
    }

    @Test
    void job_afterClearLocal_aclDeniesDespiteGlobalDefaults() throws Exception {
        OwnershipBasedSecurityTestHelper.setupPlainItemSpecificMacroForUser(r.jenkins, "bob");
        JobOwnerHelper.setProjectSpecificSecurity(job, defaultJobSecurity(Item.BUILD, "bob"));
        JobOwnerHelper.setProjectSpecificSecurity(job, null);

        assertFalse(JobOwnerHelper.getOwnerProperty(job).hasItemSpecificSecurity());
        assertThat(hasPermission("bob", job, Item.BUILD), equalTo(false));
    }

    @Test
    void folder_defaultsOnly_aclDeniesWhenLocalNotConfigured() throws Exception {
        OwnershipBasedSecurityTestHelper.setupPlainItemSpecificMacroForUser(r.jenkins, "bob");

        assertFalse(FolderOwnershipHelper.getInstance().hasItemSpecificPermission(folder, "bob", Item.BUILD));
        assertThat(hasPermission("bob", folder, Item.BUILD), equalTo(false));
    }

    @Test
    void folder_defaultsOnly_uiStillShowsDefaults() throws Exception {
        FolderOwnershipAction action = folder.getAction(FolderOwnershipAction.class);
        assertThat(action, notNullValue());
        assertFalse(action.hasItemSpecificSecurity());
        assertThat(action.getItemSpecificSecurity(), notNullValue());
    }

    @Test
    void folder_afterRestoreDefaults_aclAppliesClonedLocal() throws Exception {
        OwnershipBasedSecurityTestHelper.setupPlainItemSpecificMacroForUser(r.jenkins, "bob");
        FolderSpecificSecurity defaults = OwnershipPlugin.getInstance().getDefaultFoldersSecurity();
        assertThat(defaults, notNullValue());

        FolderOwnershipHelper.setProjectSpecificSecurity(folder, defaults.clone());

        assertTrue(FolderOwnershipHelper.getOwnerProperty(folder).hasItemSpecificSecurity());
        assertThat(hasPermission("bob", folder, Item.BUILD), equalTo(true));
    }

    @Test
    void folder_afterClearLocal_aclDeniesDespiteGlobalDefaults() throws Exception {
        OwnershipBasedSecurityTestHelper.setupPlainItemSpecificMacroForUser(r.jenkins, "bob");
        FolderOwnershipHelper.setProjectSpecificSecurity(folder, defaultFolderSecurity(Item.BUILD, "bob"));
        FolderOwnershipHelper.setProjectSpecificSecurity(folder, null);

        assertFalse(FolderOwnershipHelper.getOwnerProperty(folder).hasItemSpecificSecurity());
        assertThat(hasPermission("bob", folder, Item.BUILD), equalTo(false));
    }

    private static ItemSpecificSecurity defaultJobSecurity(Permission permission, String sid) {
        hudson.security.AuthorizationMatrixProperty matrix =
                new hudson.security.AuthorizationMatrixProperty(new HashMap<>());
        matrix.add(permission, PermissionEntry.user(sid));
        return new ItemSpecificSecurity(matrix);
    }

    private static FolderSpecificSecurity defaultFolderSecurity(Permission permission, String sid) {
        AuthorizationMatrixProperty matrix = new AuthorizationMatrixProperty(new HashMap<>());
        matrix.add(permission, PermissionEntry.user(sid));
        return new FolderSpecificSecurity(matrix);
    }

    private boolean hasPermission(String userId, hudson.security.AccessControlled item, Permission permission) {
        User user = User.get(userId);
        return ACL.impersonate(user.impersonate(), new Callable<Boolean, IllegalStateException>() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public Boolean call() {
                return item.hasPermission(permission);
            }

            @Override
            public void checkRoles(RoleChecker checker) throws SecurityException {
                // no-op
            }
        });
    }
}
