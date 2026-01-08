/*
 * The MIT License
 *
 * Copyright 2015 Oleg Nenashev <o.v.nenashev@gmail.com>.
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

package com.synopsys.arc.jenkins.plugins.ownership.wrappers;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.item_ownership_policy.AssignCreatorPolicy;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.nodes.NodeOwnerHelper;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.model.Slave;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.scm.NullSCM;
import hudson.scm.SCMRevisionState;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Future;
import org.jenkinsci.plugins.ownership.model.folders.FolderOwnershipHelper;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.jenkinsci.plugins.ownership.util.environment.EnvSetupOptions;
import org.jenkinsci.plugins.ownership.util.mail.MailOptions;

import static hudson.Functions.isWindows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for {@link OwnershipBuildWrapper}.
 * @author Oleg Nenashev
 */
@WithJenkins
class OwnershipBuildWrapperTest {

    private JenkinsRule r;
    private Slave node;
    private FreeStyleProject project;
    private User nodeOwner;
    private User projectOwner;

    private static final String NODE_OWNER_ID = "test_node_owner";
    private static final String PROJECT_OWNER_ID = "test_project_owner";

    @BeforeEach
    void beforeEach(JenkinsRule rule) throws Exception {
        r = rule;
        // Create test user
        nodeOwner = User.get(NODE_OWNER_ID);
        nodeOwner.setFullName("Test Node Owner");
        projectOwner = User.get(PROJECT_OWNER_ID);
        projectOwner.setFullName("Test Project Owner");
        
        // Configure ownership plugin
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withItemOwnershipPolicy(new AssignCreatorPolicy())
                .configure();
        
        // Create node with ownership
        node = r.createOnlineSlave();
        NodeOwnerHelper.setOwnership(node, new OwnershipDescription(true, NODE_OWNER_ID, Collections.emptyList()));
        
        // Create project
        project = r.createFreeStyleProject();
        JobOwnerHelper.setOwnership(project, new OwnershipDescription(true, PROJECT_OWNER_ID, Collections.emptyList()));
        project.getBuildersList().add(isWindows() ? new BatchFile("set") : new Shell("env"));
    }
    
    @Test
    void testVarsPresenceOnSuccess() throws Exception {
        final OwnershipBuildWrapper wrapper = new OwnershipBuildWrapper(true, true);
        project.getBuildWrappersList().add(wrapper);
        testVarsPresence(false);
    }
    
    @Issue("JENKINS-23926")
    @Test
    void testVarsPresenceOnSCMFailure() throws Exception {
        final OwnershipBuildWrapper wrapper = new OwnershipBuildWrapper(true, true);
        project.getBuildWrappersList().add(wrapper);

        testVarsPresence(true);
    }
    
    @Issue("JENKINS-23947")
    @Test
    void testVarsPresenceOnGlobalOptions() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withItemOwnershipPolicy(new AssignCreatorPolicy())
                .withMailOptions(MailOptions.DEFAULT)
                .withGlobalEnvSetupOptions(new EnvSetupOptions(true, true))
                .configure();
        testVarsPresence(true);
    }
    
    @Issue("JENKINS-27715")
    @Test
    void testCoOwnersVarsInjection() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withItemOwnershipPolicy(new AssignCreatorPolicy())
                .withMailOptions(MailOptions.DEFAULT)
                .withGlobalEnvSetupOptions(new EnvSetupOptions(true, true))
                .configure();
        
        FreeStyleBuild build = testVarsPresence(false);
        r.assertLogContains("NODE_COOWNERS=" + NODE_OWNER_ID, build);
        r.assertLogContains("JOB_COOWNERS=" + PROJECT_OWNER_ID, build);
    }

    @Test
    @Issue("JENKINS-28881")
    void shouldInjectInheritedOwnershipInfo() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withGlobalEnvSetupOptions(new EnvSetupOptions(true, true))
                .configure();
        
        // Init folder with a nested job
        Folder folder = r.jenkins.createProject(Folder.class, "folder");
        FolderOwnershipHelper.setOwnership(folder, new OwnershipDescription(true, PROJECT_OWNER_ID, Collections.emptyList()));
        FreeStyleProject prj = folder.createProject(FreeStyleProject.class, "projectInsideFolder");
        prj.getBuildersList().add(isWindows() ? new BatchFile("set") : new Shell("env"));
        
        // Run test. We expect Ownership info to be inherited for the project
        FreeStyleBuild build = testVarsPresence(false);
        r.assertLogContains("NODE_COOWNERS=" + NODE_OWNER_ID, build);
        r.assertLogContains("JOB_COOWNERS=" + PROJECT_OWNER_ID, build);
    }
    
    private FreeStyleBuild testVarsPresence(boolean failSCM) throws Exception {
        project.setAssignedNode(node);
        if (failSCM) {
            project.setScm(new AlwaysFailNullSCM());
        }
        
        // Run without params
        Future<FreeStyleBuild> fBuild = project.scheduleBuild2(0);
        assertNotNull(fBuild);
        FreeStyleBuild build = fBuild.get();
        
        // Check the build environment
        final EnvVars env = build.getEnvironment(TaskListener.NULL);
        if (!failSCM) {
            r.assertLogContains("NODE_OWNER=" + NODE_OWNER_ID, build);
            r.assertLogContains("JOB_OWNER=" + PROJECT_OWNER_ID, build);
        }
        assertTrue(env.containsKey("NODE_OWNER"));
        assertTrue(env.containsKey("JOB_OWNER"));  
        return build;
    }
    
    private static class AlwaysFailNullSCM extends NullSCM {
        
        @Override
        public void checkout(Run<?, ?> build, Launcher launcher, FilePath workspace, TaskListener listener, File changelogFile, SCMRevisionState baseline) throws IOException, InterruptedException {
            throw new IOException("Checkout failed (as designed)");
        }     
    }
}
