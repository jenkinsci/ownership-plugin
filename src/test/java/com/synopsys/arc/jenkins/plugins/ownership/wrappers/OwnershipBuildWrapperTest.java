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
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPluginConfiguration;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.item_ownership_policy.AssignCreatorPolicy;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.nodes.NodeOwnerHelper;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Slave;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.scm.NullSCM;
import hudson.tasks.Shell;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Future;
import static junit.framework.Assert.*;
import org.jenkinsci.plugins.ownership.model.folders.FolderOwnershipHelper;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.jenkinsci.plugins.ownership.util.environment.EnvSetupOptions;
import org.jenkinsci.plugins.ownership.util.mail.MailOptions;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests for {@link OwnershipBuildWrapper}.
 * @author Oleg Nenashev
 */
public class OwnershipBuildWrapperTest {
    
    public @Rule JenkinsRule r = new JenkinsRule();
    private Slave node;
    private FreeStyleProject project;
    private User nodeOwner;
    private User projectOwner;
    
    private final static String NODE_OWNER_ID = "test_node_owner";
    private final static String PROJECT_OWNER_ID = "test_project_owner";
    
    // @Before does not work due to classloader issues (?)
    // See https://groups.google.com/forum/#!topic/jenkinsci-dev/3pHRVFai7T4
    public void initJenkinsInstance() throws Exception {
        // Create test user
        nodeOwner = User.get(NODE_OWNER_ID);
        nodeOwner.setFullName("Test Node Owner");
        projectOwner = User.get(PROJECT_OWNER_ID);
        projectOwner.setFullName("Test Project Owner");
        
        // Configure ownership plugin
        r.jenkins.getPlugin(OwnershipPlugin.class).configure(
                true, null, null, new OwnershipPluginConfiguration(new AssignCreatorPolicy()));
        
        // Create node with ownership
        node = r.createOnlineSlave();
        NodeOwnerHelper.setOwnership(node, new OwnershipDescription(true, NODE_OWNER_ID));
        
        // Create project
        project = r.createFreeStyleProject();
        JobOwnerHelper.setOwnership(project, new OwnershipDescription(true, PROJECT_OWNER_ID));
        project.getBuildersList().add(new Shell("env"));
    }
    
    public @Test void testVarsPresenseOnSuccess() throws Exception {
        initJenkinsInstance();
        final OwnershipBuildWrapper wrapper = new OwnershipBuildWrapper(true, true);
        project.getBuildWrappersList().add(wrapper);
        testVarsPresense(false);
    }
    
    @Bug(23926)
    public @Test void testVarsPresenseOnSCMFailure() throws Exception {
        initJenkinsInstance();
        final OwnershipBuildWrapper wrapper = new OwnershipBuildWrapper(true, true);
        project.getBuildWrappersList().add(wrapper);

        testVarsPresense(true);
    }
    
    @Bug(23947)
    public @Test void testVarsPresenseOnGlobalOptions() throws Exception {
        initJenkinsInstance();
        final OwnershipPluginConfiguration pluginConf = new OwnershipPluginConfiguration(
                new AssignCreatorPolicy(),MailOptions.DEFAULT, 
                new EnvSetupOptions(true, true));
        r.jenkins.getPlugin(OwnershipPlugin.class).configure(true, null, null, pluginConf);
        testVarsPresense(true);
    }
    
    @Bug(27715)
    public @Test void testCoOwnersVarsInjection() throws Exception {
        initJenkinsInstance();
        final OwnershipPluginConfiguration pluginConf = new OwnershipPluginConfiguration(
                new AssignCreatorPolicy(),MailOptions.DEFAULT, 
                new EnvSetupOptions(true, true));
        r.jenkins.getPlugin(OwnershipPlugin.class).configure(true, null, null, pluginConf);
        
        FreeStyleBuild build = testVarsPresense(false);
        r.assertLogContains("NODE_COOWNERS="+NODE_OWNER_ID, build);
        r.assertLogContains("JOB_COOWNERS="+PROJECT_OWNER_ID, build);
    }
    
    @Test
    @Bug(28881)
    public void shouldInjectInheritedOwnershipInfo() throws Exception {
        initJenkinsInstance();
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withGlobalEnvSetupOptions(new EnvSetupOptions(true, true))
                .configure();
        
        // Init folder with a nested job
        Folder folder = r.jenkins.createProject(Folder.class, "folder");
        FolderOwnershipHelper.setOwnership(folder, new OwnershipDescription(true, PROJECT_OWNER_ID, Collections.<String>emptyList()));
        FreeStyleProject prj = folder.createProject(FreeStyleProject.class, "projectInsideFolder");
        prj.getBuildersList().add(new Shell("env"));
        
        // Run test. We expect Ownership info to be inherited for the project
        FreeStyleBuild build = testVarsPresense(false);
        r.assertLogContains("NODE_COOWNERS="+NODE_OWNER_ID, build);
        r.assertLogContains("JOB_COOWNERS="+PROJECT_OWNER_ID, build);
    }
    
    private FreeStyleBuild testVarsPresense(boolean failSCM) throws Exception {              
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
            r.assertLogContains("NODE_OWNER="+NODE_OWNER_ID, build);
            r.assertLogContains("JOB_OWNER="+PROJECT_OWNER_ID, build);
        }
        assertTrue(env.containsKey("NODE_OWNER"));
        assertTrue(env.containsKey("JOB_OWNER"));  
        return build;
    }
    
    private static class AlwaysFailNullSCM extends NullSCM {
        
        @Override
        public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher, FilePath remoteDir, BuildListener listener, File changeLogFile) throws IOException, InterruptedException {
            throw new IOException("Checkout failed (as designed)");
        }
    }
}
