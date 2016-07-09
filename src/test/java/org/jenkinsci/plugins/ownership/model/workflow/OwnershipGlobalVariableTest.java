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
package org.jenkinsci.plugins.ownership.model.workflow;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.nodes.NodeOwnerHelper;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.DumbSlave;
import hudson.tasks.MailAddressResolver;
import hudson.tasks.Mailer;
import java.util.Arrays;
import javax.annotation.Nonnull;
import jenkins.model.JenkinsLocationConfiguration;
import static org.hamcrest.Matchers.*;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests of {@link OwnershipGlobalVariable}.
 *
 * @author Oleg Nenashev
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.crypto.*" })
@PrepareForTest({MailAddressResolver.class, Mailer.class, Mailer.DescriptorImpl.class})
public class OwnershipGlobalVariableTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private Mailer.DescriptorImpl descriptor;

    //TODO: We have to do Mocking due to the Bad Mailer Plugin API
    @Before
    public void setupMailOptions() throws Exception {
        PowerMockito.spy(Mailer.class);
        descriptor = PowerMockito.mock(Mailer.DescriptorImpl.class);
        PowerMockito.doReturn(descriptor).when(Mailer.class, "descriptor");
        PowerMockito.when(descriptor.getDefaultSuffix()).thenReturn("mailsuff.ix");
    }

    @Before
    public void initSecurityRealm() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
    }
    
    @Test
    public void jobOwnershipSnippet_noOwnership_sandbox() throws Exception {
        OwnershipDescription d = OwnershipDescription.DISABLED_DESCR;
        WorkflowRun run = buildSnippetAndAssertSuccess("printJobOwnershipInfo", d, false);
        assertThat(run.getLog(), containsString("Ownership is disabled"));
    }
    
    @Test
    public void jobOwnershipSnippet_noOwnership() throws Exception {
        OwnershipDescription d = OwnershipDescription.DISABLED_DESCR;
        WorkflowRun run = buildSnippetAndAssertSuccess("printJobOwnershipInfo", d, true);
        assertThat(run.getLog(), containsString("Ownership is disabled"));
    }
    
    @Test
    public void shouldProvideJobOwnershipInfo_sandbox() throws Exception {
        OwnershipDescription d = new OwnershipDescription(true, "owner",
                Arrays.asList("coowner1", "coowner2"));
        WorkflowRun run = buildSnippetAndAssertSuccess("printJobOwnershipInfo", d, false);
        assertThat(run.getLog(), containsString("owner"));
    }
    
    @Test
    public void shouldProvideJobOwnershipInfo() throws Exception {
        OwnershipDescription d = new OwnershipDescription(true, "owner",
                Arrays.asList("coowner1", "coowner2"));
        WorkflowRun run = buildSnippetAndAssertSuccess("printJobOwnershipInfo", d, true);
        assertThat(run.getLog(), containsString("owner"));
    }
    
    @Test
    public void nodeOwnershipSnippet_onSlave() throws Exception {
        DumbSlave slave = j.createOnlineSlave(new LabelAtom("requiredLabel"));
        NodeOwnerHelper.setOwnership(slave, new OwnershipDescription(true, "ownerOfJenkins",
                Arrays.asList("coowner1", "coowner2")));
        OwnershipDescription d = OwnershipDescription.DISABLED_DESCR;
        WorkflowRun run = buildSnippetAndAssertSuccess("printNodeOwnershipInfo", d, false);
        assertThat(run.getLog(), containsString("ownerOfJenkins"));
    }
    
    @Test
    public void nodeOwnershipSnippet_onMaster() throws Exception {
        j.jenkins.setLabelString("requiredLabel");
        NodeOwnerHelper.setOwnership(j.jenkins, new OwnershipDescription(true, "ownerOfJenkins",
                Arrays.asList("coowner1", "coowner2")));
        OwnershipDescription d = OwnershipDescription.DISABLED_DESCR;
        WorkflowRun run = buildSnippetAndAssertSuccess("printNodeOwnershipInfo", d, true);
        assertThat(run.getLog(), containsString("ownerOfJenkins"));
    }
    
    @Test
    public void nodeOwnershipSnippet_onMaster_sandbox() throws Exception {
        j.jenkins.setLabelString("requiredLabel");
        NodeOwnerHelper.setOwnership(j.jenkins, new OwnershipDescription(true, "ownerOfJenkins",
                Arrays.asList("coowner1", "coowner2")));
        OwnershipDescription d = OwnershipDescription.DISABLED_DESCR;
        WorkflowRun run = buildSnippetAndAssertSuccess("printNodeOwnershipInfo", d, false);
        assertThat(run.getLog(), containsString("ownerOfJenkins"));
    }
    
    private WorkflowRun buildSnippetAndAssertSuccess(@Nonnull String snippetName,
            @Nonnull OwnershipDescription ownershipDescription, boolean useSandbox) throws Exception {
        return buildAndAssertSuccess(OwnershipGlobalVariable.getSampleSnippet(snippetName),
                ownershipDescription, snippetName, useSandbox);
    }

    private WorkflowRun buildAndAssertSuccess(@Nonnull String pipelineScript,
            @Nonnull OwnershipDescription ownershipDescription,
            @Nonnull String projectName, boolean useSandbox) throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, projectName);
        
        // TODO: Actually sandbox flag in CpsFlowDefinition is inverted
        job.setDefinition(new CpsFlowDefinition(pipelineScript, useSandbox));
        JobOwnerHelper.setOwnership(job, ownershipDescription);
        
        // We are going to run Pipeline as System, and the script won't be automatically approved in such case.
        // So we approve the script.
        ScriptApproval.get().preapprove(pipelineScript, GroovyLanguage.get());
        
        return buildAndAssertSuccess(job);
    }

    private WorkflowRun buildAndAssertSuccess(@Nonnull WorkflowJob job) throws Exception {
        // Run job
        QueueTaskFuture<WorkflowRun> runFuture = job.scheduleBuild2(0, new Action[0]);
        assertThat("build was actually scheduled", runFuture, notNullValue());
        WorkflowRun run = runFuture.get();

        assertThat("Pipeline run " + run + " failed", run.getResult(), equalTo(Result.SUCCESS));
        return run;
    }
}
