/*
 * The MIT License
 *
 * Copyright (c) 2016 Oleg Nenashev.
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
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.User;
import hudson.remoting.Callable;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import java.util.Arrays;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.jenkinsci.plugins.ownership.model.folders.FolderOwnershipHelper;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.remoting.RoleChecker;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Checks ownership-based security.
 * @author Oleg Nenashev
 */
public class OwnershipBasedSecurityTest {
    
    @Rule
    public final JenkinsRule j = new JenkinsRule();
    
    @Test
    public void shouldWorkForProjects() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        OwnershipBasedSecurityTestHelper.setup(j.jenkins);
        
        FreeStyleProject project = j.createFreeStyleProject("project");
        JobOwnerHelper.setOwnership(project, new OwnershipDescription(true, "owner", Arrays.asList("coOwner")));
        
        verifyItemPermissions(project);
    }
    
    @Test
    @Issue("JENKINS-28881")
    public void shouldWorkForProjectsWithInheritedOwnership() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        OwnershipBasedSecurityTestHelper.setup(j.jenkins);
        
        Folder folder = j.jenkins.createProject(Folder.class, "folder");
        FreeStyleProject project = folder.createProject(FreeStyleProject.class, "project");
        FolderOwnershipHelper.setOwnership(folder, new OwnershipDescription(true, "owner", Arrays.asList("coOwner")));
        
        // Verify that permissions are inherited by project
        verifyItemPermissions(project);
        
        // Also check folder permissions
        verifyItemPermissions(folder);
    }
    
    @Test
    @Issue("JENKINS-32353")
    public void shouldWorkForPipelineProjectsInFolders() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        OwnershipBasedSecurityTestHelper.setup(j.jenkins);
        
        Folder folder = j.jenkins.createProject(Folder.class, "folder");
        WorkflowJob project = folder.createProject(WorkflowJob.class, "testWorkflowJob");
        project.setDefinition(new CpsFlowDefinition("echo 'Hello, world!'", false));
        FolderOwnershipHelper.setOwnership(folder, new OwnershipDescription(true, "owner", Arrays.asList("coOwner")));
        
        // Verify that permissions are inherited by project
        verifyItemPermissions(project);
        
        // Also check folder permissions
        verifyItemPermissions(folder);
    }
    
    private void verifyItemPermissions(Item item) {
        // Admin
        assertHasPermission("admin", item, Item.READ);
        assertHasPermission("admin", item, Item.DELETE);
        assertHasPermission("admin", item, Item.CONFIGURE);
        
        // Owner
        assertHasPermission("owner", item, Item.READ);
        assertHasPermission("owner", item, Item.DELETE);
        assertHasPermission("owner", item, Item.CONFIGURE);
        
        // CoOwner
        assertHasPermission("coOwner", item, Item.READ);
        assertHasNoPermission("coOwner", item, Item.DELETE);
        assertHasPermission("coOwner", item, Item.CONFIGURE);
        
        // User
        assertHasPermission("user", item, Item.READ);
        assertHasNoPermission("user", item, Item.DELETE);
        assertHasNoPermission("user", item, Item.CONFIGURE);
    }
    
    private void assertHasPermission(String userId, final AccessControlled item, final Permission p) {
        assertThat("User '" + userId + "' has no " + p.getId() + " permission for " + item + ", but it should according to security settings", 
                hasPermission(userId, item, p), equalTo(true));
    }
    
    private void assertHasNoPermission(String userId, final AccessControlled item, final Permission p) {
        assertThat("User '" + userId + "' has the " + p.getId() + " permission for " + item + ", but it should not according to security settings", 
                hasPermission(userId, item, p), equalTo(false));
    }
    
    private boolean hasPermission(String userId, final AccessControlled item, final Permission p) {
        User user = User.get(userId);
        return ACL.impersonate(user.impersonate(), new Callable<Boolean, IllegalStateException>() {
            private static final long serialVersionUID = 1L;
       
            @Override
            public Boolean call() throws IllegalStateException {
                return item.hasPermission(p);
            }

            @Override
            public void checkRoles(RoleChecker checker) throws SecurityException {
                // Do nothing
            }
        });
    }
}
