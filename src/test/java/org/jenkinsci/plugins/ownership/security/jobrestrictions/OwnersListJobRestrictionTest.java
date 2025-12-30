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
package org.jenkinsci.plugins.ownership.security.jobrestrictions;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.security.jobrestrictions.OwnersListJobRestriction;
import com.synopsys.arc.jenkins.plugins.ownership.util.ui.UserSelector;
import com.synopsys.arc.jenkinsci.plugins.jobrestrictions.nodes.JobRestrictionProperty;
import com.synopsys.arc.jenkinsci.plugins.jobrestrictions.restrictions.JobRestrictionBlockageCause;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Queue;
import hudson.model.labels.LabelAtom;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.slaves.DumbSlave;
import jenkins.model.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.hamcrest.Matchers.*;
import org.jenkinsci.plugins.ownership.model.folders.FolderOwnershipHelper;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests of {@link OwnersListJobRestriction}.
 * @author Oleg Nenashev
 */
public class OwnersListJobRestrictionTest {
    private static final IdStrategy CASE_SENSITIVE = new IdStrategy.CaseSensitive();

    @Rule
    public final JenkinsRule j = new JenkinsRule();
    
    private Label testLabel;
    private DumbSlave slave;
    private JobRestrictionProperty jobRestrictionProperty;
               
    @Before
    public void setUp() throws Exception {
        // Initialize plugin before using it
        OwnershipPluginConfigurer.forJenkinsRule(j).configure();
        
        testLabel = new LabelAtom("testLabel");
        slave = j.createOnlineSlave(testLabel);
        jobRestrictionProperty = new JobRestrictionProperty(
                new OwnersListJobRestriction(Arrays.asList(new UserSelector("owner")), false));;
        slave.getNodeProperties().add(jobRestrictionProperty);

        applyIdStrategy(CASE_SENSITIVE);
    }
    
    @Test
    public void nodeShouldAcceptRunsFromOwner() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.setAssignedLabel(testLabel);
        JobOwnerHelper.setOwnership(project, 
                new OwnershipDescription(true, "owner", Collections.<String>emptyList()));
        
        project.scheduleBuild2(0);
        j.jenkins.getQueue().maintain();
        
        List<Queue.BuildableItem> items = j.jenkins.getQueue().getBuildableItems();
        assertThat("1 item should be in the queue", items.size(), equalTo(1));
        Queue.BuildableItem item = items.get(0);
        
        assertThat("Job has not been accepted, but JobRestrictions should allow the run", 
                jobRestrictionProperty.canTake(item), nullValue());
    }
    
    @Test
    public void nodeShouldDeclineRunsFromNotOwner() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.setAssignedLabel(testLabel);
        JobOwnerHelper.setOwnership(project, 
                new OwnershipDescription(true, "notOwner", Collections.<String>emptyList()));
        
        project.scheduleBuild2(0);
        j.jenkins.getQueue().maintain();
        
        List<Queue.BuildableItem> items = j.jenkins.getQueue().getBuildableItems();
        assertThat("1 item should be in the queue", items.size(), equalTo(1));
        Queue.BuildableItem item = items.get(0);
        
        assertThat("Job restrictions should not allow the run, because the job has wrong owner",
                jobRestrictionProperty.canTake(item), instanceOf(JobRestrictionBlockageCause.class));
    }
    
    @Test
    @Issue("JENKINS-28881")
    public void nodeShouldAcceptRunsFromInheritedOwner() throws Exception {
        Folder folder = j.jenkins.createProject(Folder.class, "folder");
        FreeStyleProject project = folder.createProject(FreeStyleProject.class, "project");
        project.setAssignedLabel(testLabel);
        FolderOwnershipHelper.setOwnership(folder, 
                new OwnershipDescription(true, "owner", Collections.<String>emptyList()));
        
        project.scheduleBuild2(0);
        j.jenkins.getQueue().maintain();
        
        List<Queue.BuildableItem> items = j.jenkins.getQueue().getBuildableItems();
        assertThat("1 item should be in the queue", items.size(), equalTo(1));
        Queue.BuildableItem item = items.get(0);
        
        assertThat("Run has been prohibited, but Ownership plugin should allow it according to the inherited value",
                jobRestrictionProperty.canTake(item), nullValue());
    }
    
    @Test
    @Issue("JENKINS-28881")
    public void nodeShouldDeclineRunsFromInheritedNotOwner() throws Exception {
        Folder folder = j.jenkins.createProject(Folder.class, "folder");
        FreeStyleProject project = folder.createProject(FreeStyleProject.class, "project");
        project.setAssignedLabel(testLabel);
        FolderOwnershipHelper.setOwnership(folder, 
                new OwnershipDescription(true, "notOwner", Collections.<String>emptyList()));
        
        project.scheduleBuild2(0);
        j.jenkins.getQueue().maintain();
        
        List<Queue.BuildableItem> items = j.jenkins.getQueue().getBuildableItems();
        assertThat("1 item should be in the queue", items.size(), equalTo(1));
        Queue.BuildableItem item = items.get(0);
        
        assertThat("Job restrictions should not allow the run, because the job has wrong owner",
                jobRestrictionProperty.canTake(item), instanceOf(JobRestrictionBlockageCause.class));
    }

    @Test
    @Issue("JENKINS-20832")
    public void nodeShouldAcceptRunsFromWithInsensitiveCaseOnOwner() throws Exception {
        applyIdStrategy(IdStrategy.CASE_INSENSITIVE);

        Folder folder = j.jenkins.createProject(Folder.class, "folder");
        FreeStyleProject project = folder.createProject(FreeStyleProject.class, "project2");
        project.setAssignedLabel(testLabel);
        FolderOwnershipHelper.setOwnership(folder,
                new OwnershipDescription(true, "Owner", Collections.<String>emptyList()));

        project.scheduleBuild2(0);
        j.jenkins.getQueue().maintain();

        List<Queue.BuildableItem> items = j.jenkins.getQueue().getBuildableItems();
        assertThat("1 item should be in the queue", items.size(), equalTo(1));
        Queue.BuildableItem item = items.get(0);

        assertThat("Run has been prohibited, but Ownership plugin should allow it according to the inherited value",
                jobRestrictionProperty.canTake(item), nullValue());
    }

    @Test
    @Issue("JENKINS-20832")
    public void nodeShouldAcceptRunsFromWithInsensitiveCaseOnCoOwner() throws Exception {
        applyIdStrategy(IdStrategy.CASE_INSENSITIVE);

        // Change to accept coowners
        jobRestrictionProperty = new JobRestrictionProperty(
                new OwnersListJobRestriction(Arrays.asList(new UserSelector("owner")), true));
        slave.getNodeProperties().add(jobRestrictionProperty);

        Folder folder = j.jenkins.createProject(Folder.class, "folder");
        FreeStyleProject project = folder.createProject(FreeStyleProject.class, "project3");
        project.setAssignedLabel(testLabel);
        FolderOwnershipHelper.setOwnership(folder,
                new OwnershipDescription(true, "owner1", Arrays.asList("owner2", "Owner")));

        project.scheduleBuild2(0);
        j.jenkins.getQueue().maintain();

        List<Queue.BuildableItem> items = j.jenkins.getQueue().getBuildableItems();
        assertThat("1 item should be in the queue", items.size(), equalTo(1));
        Queue.BuildableItem item = items.get(0);

        assertThat("Run has been prohibited, but Ownership plugin should allow it according to the inherited value",
                jobRestrictionProperty.canTake(item), nullValue());
    }

    private void applyIdStrategy(final IdStrategy idStrategy) throws IOException {
        HudsonPrivateSecurityRealm realm = new HudsonPrivateSecurityRealm(false, false, null) {
            @Override
            public IdStrategy getUserIdStrategy() {
                return idStrategy;
            }

            @Override
            public IdStrategy getGroupIdStrategy() {
                return idStrategy;
            }
        };
        realm.createAccount("owner", "owner");
        j.jenkins.setSecurityRealm(realm);
    }
}
