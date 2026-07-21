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

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.security.itemspecific.ItemSpecificSecurity;
import com.synopsys.arc.jenkins.plugins.ownership.security.rolestrategy.ItemSpecificRoleMacro;
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.User;
import hudson.remoting.Callable;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import hudson.model.queue.QueueTaskFuture;
import java.io.Serial;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.ownership.model.folders.FolderOwnershipHelper;
import org.jenkinsci.plugins.ownership.security.folderspecific.FolderSpecificSecurity;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.jenkinsci.remoting.RoleChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end coverage for {@link ItemSpecificRoleMacro} on jobs and folders.
 */
@WithJenkins
class ItemSpecificRoleMacroIntegrationTest {

    private JenkinsRule r;

    private ItemSpecificRoleMacro macro;

    private FreeStyleProject job;

    private Folder folder;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        r = rule;
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        OwnershipPluginConfigurer.forJenkinsRule(r).configure();

        macro = Jenkins.get().getExtensionList(ItemSpecificRoleMacro.class).stream()
                .findFirst()
                .orElse(null);
        assertThat("ItemSpecificRoleMacro extension", macro, notNullValue());

        User.get("alice", true);
        User.get("bob", true);
        User.get("admin", true);

        job = r.createFreeStyleProject("macro-job");
        JobOwnerHelper.setOwnership(job, new OwnershipDescription(true, "admin", null));

        folder = r.jenkins.createProject(Folder.class, "macro-folder");
        FolderOwnershipHelper.setOwnership(folder, new OwnershipDescription(true, "admin", null));
    }

    @Test
    void macro_hasPermission_respectsUserTypedJobMatrixEntry() throws Exception {
        JobOwnerHelper.setProjectSpecificSecurity(job, jobMatrixWithUser(Item.BUILD, "alice"));

        assertTrue(macro.hasPermission("alice", Item.BUILD, RoleType.Project, job, null));
        assertFalse(macro.hasPermission("bob", Item.BUILD, RoleType.Project, job, null));
    }

    @Test
    void macro_hasPermission_respectsUserTypedFolderMatrixEntry() throws Exception {
        FolderOwnershipHelper.setProjectSpecificSecurity(folder, folderMatrixWithUser(Item.BUILD, "alice"));

        assertTrue(macro.hasPermission("alice", Item.BUILD, RoleType.Project, folder, null));
        assertFalse(macro.hasPermission("bob", Item.BUILD, RoleType.Project, folder, null));
    }

    @Test
    void roleStrategy_itemSpecificMacro_grantsBuildOnJobWhenUserTypedMatrixAllows() throws Exception {
        OwnershipBasedSecurityTestHelper.setupItemSpecificMacro(r.jenkins);
        JobOwnerHelper.setProjectSpecificSecurity(job, jobMatrixWithUser(Item.BUILD, "alice"));

        assertHasPermission("alice", job, Item.BUILD);
        assertHasNoPermission("bob", job, Item.BUILD);
    }

    @Test
    void roleStrategy_itemSpecificMacro_allowsScheduleBuildWhenUserTypedMatrixAllows() throws Exception {
        OwnershipBasedSecurityTestHelper.setupItemSpecificMacro(r.jenkins);
        JobOwnerHelper.setProjectSpecificSecurity(job, jobMatrixWithUser(Item.BUILD, "alice"));
        r.jenkins.setQuietPeriod(0);

        QueueTaskFuture<?> future = ACL.impersonate(User.get("alice").impersonate(), new Callable<QueueTaskFuture<?>, IllegalStateException>() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public QueueTaskFuture<?> call() throws IllegalStateException {
                return job.scheduleBuild2(0);
            }

            @Override
            public void checkRoles(RoleChecker checker) throws SecurityException {
                // no-op
            }
        });
        assertThat(future, notNullValue());
    }

    @Test
    void macro_hasPermission_respectsJobMatrix() throws Exception {
        JobOwnerHelper.setProjectSpecificSecurity(job, jobMatrixWith(Item.CONFIGURE, "alice"));

        assertTrue(macro.hasPermission("alice", Item.CONFIGURE, RoleType.Project, job, null));
        assertFalse(macro.hasPermission("bob", Item.CONFIGURE, RoleType.Project, job, null));
    }

    @Test
    void macro_hasPermission_respectsFolderMatrix() throws Exception {
        FolderOwnershipHelper.setProjectSpecificSecurity(folder, folderMatrixWith(Item.CONFIGURE, "alice"));

        assertTrue(macro.hasPermission("alice", Item.CONFIGURE, RoleType.Project, folder, null));
        assertFalse(macro.hasPermission("bob", Item.CONFIGURE, RoleType.Project, folder, null));
    }

    @Test
    void roleStrategy_itemSpecificMacro_grantsConfigureOnJobWhenMatrixAllows() throws Exception {
        OwnershipBasedSecurityTestHelper.setupItemSpecificMacro(r.jenkins);
        JobOwnerHelper.setProjectSpecificSecurity(job, jobMatrixWith(Item.CONFIGURE, "alice"));

        assertHasPermission("alice", job, Item.CONFIGURE);
        assertHasNoPermission("bob", job, Item.CONFIGURE);
    }

    @Test
    void roleStrategy_itemSpecificMacro_grantsConfigureOnFolderWhenMatrixAllows() throws Exception {
        OwnershipBasedSecurityTestHelper.setupItemSpecificMacro(r.jenkins);
        FolderOwnershipHelper.setProjectSpecificSecurity(folder, folderMatrixWith(Item.CONFIGURE, "alice"));

        assertHasPermission("alice", folder, Item.CONFIGURE);
        assertHasNoPermission("bob", folder, Item.CONFIGURE);
    }

    private static ItemSpecificSecurity jobMatrixWith(Permission permission, String sid) {
        Map<Permission, Set<String>> permissions = new HashMap<>();
        permissions.put(permission, Collections.singleton(sid));
        return new ItemSpecificSecurity(new hudson.security.AuthorizationMatrixProperty(permissions));
    }

    private static FolderSpecificSecurity folderMatrixWith(Permission permission, String sid) {
        Map<Permission, Set<String>> permissions = new HashMap<>();
        permissions.put(permission, Collections.singleton(sid));
        return new FolderSpecificSecurity(new AuthorizationMatrixProperty(permissions));
    }

    private static ItemSpecificSecurity jobMatrixWithUser(Permission permission, String sid) {
        hudson.security.AuthorizationMatrixProperty matrix =
                new hudson.security.AuthorizationMatrixProperty(new HashMap<>());
        matrix.add(permission, PermissionEntry.user(sid));
        return new ItemSpecificSecurity(matrix);
    }

    private static FolderSpecificSecurity folderMatrixWithUser(Permission permission, String sid) {
        AuthorizationMatrixProperty matrix = new AuthorizationMatrixProperty(new HashMap<>());
        matrix.add(permission, PermissionEntry.user(sid));
        return new FolderSpecificSecurity(matrix);
    }

    private void assertHasPermission(String userId, AccessControlled item, Permission permission) {
        assertThat(hasPermission(userId, item, permission), equalTo(true));
    }

    private void assertHasNoPermission(String userId, AccessControlled item, Permission permission) {
        assertThat(hasPermission(userId, item, permission), equalTo(false));
    }

    private boolean hasPermission(String userId, AccessControlled item, Permission permission) {
        User user = User.get(userId);
        return ACL.impersonate(user.impersonate(), new Callable<Boolean, IllegalStateException>() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public Boolean call() throws IllegalStateException {
                return item.hasPermission(permission);
            }

            @Override
            public void checkRoles(RoleChecker checker) throws SecurityException {
                // no-op
            }
        });
    }
}
