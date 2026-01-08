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
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.DumbSlave;
import hudson.tasks.Mailer;
import java.util.Arrays;
import javax.annotation.Nonnull;
import static org.hamcrest.Matchers.*;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests of {@link OwnershipGlobalVariable}.
 *
 * @author Oleg Nenashev
 */
@ParameterizedClass(name = "{index}: sandbox={0}")
@ValueSource(booleans =  {true, false})
@WithJenkins
class OwnershipGlobalVariableTest {

    private JenkinsRule j;

    @Parameter
    private boolean useSandbox;

    @BeforeEach
    void beforeEach(JenkinsRule rule) throws Exception {
        j = rule;

        // Initialize Mail Suffix
        Descriptor<?> mailerDescriptor = j.jenkins.getDescriptor(Mailer.class);
        assertThat(mailerDescriptor, instanceOf(Mailer.DescriptorImpl.class));
        ((Mailer.DescriptorImpl)mailerDescriptor).setDefaultSuffix("mailsuff.ix");

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        // Initialize plugin before using it in Pipeline scripts
        OwnershipPluginConfigurer.forJenkinsRule(j).configure();
    }

    @Test
    void jobOwnershipSnippet_noOwnership() throws Exception {
        OwnershipDescription d = OwnershipDescription.DISABLED_DESCR;
        WorkflowRun run = buildSnippetAndAssertSuccess("printJobOwnershipInfo", d);
        assertThat(run.getLog(), containsString("Ownership is disabled"));
    }

    @Test
    void jobOwnershipSnippet_withOwner() throws Exception {
        OwnershipDescription d = new OwnershipDescription(true, "owner",
                Arrays.asList("coowner1", "coowner2"));
        WorkflowRun run = buildSnippetAndAssertSuccess("printJobOwnershipInfo", d);
        assertThat(run.getLog(), containsString("owner"));
    }

    @Test
    void nodeOwnershipSnippet_onSlave() throws Exception {
        DumbSlave slave = j.createOnlineSlave(new LabelAtom("requiredLabel"));
        NodeOwnerHelper.setOwnership(slave, new OwnershipDescription(true, "ownerOfJenkins",
                Arrays.asList("coowner1", "coowner2")));
        OwnershipDescription d = OwnershipDescription.DISABLED_DESCR;
        WorkflowRun run = buildSnippetAndAssertSuccess("printNodeOwnershipInfo", d);
        assertThat(run.getLog(), containsString("ownerOfJenkins"));
    }

    @Test
    void nodeOwnershipSnippet_onMaster() throws Exception {
        NodeOwnerHelper.setOwnership(j.jenkins, new OwnershipDescription(true, "ownerOfJenkins",
                Arrays.asList("coowner1", "coowner2")));
        OwnershipDescription d = OwnershipDescription.DISABLED_DESCR;
        // Use node() without parameters - it will use master node by default
        // The issue is that env.NODE_NAME might be null or empty for master, so we need to handle it
        String script = """
                node() {
                  def nodeName = env.NODE_NAME ?: 'master'
                  echo "Current NODE_NAME = ${nodeName}"
                  if (ownership.node.ownershipEnabled) {
                    println "Owner ID: ${ownership.node.primaryOwnerId}"
                    println "Owner e-mail: ${ownership.node.primaryOwnerEmail}"
                    println "Co-owner IDs: ${ownership.node.secondaryOwnerIds}"
                    println "Co-owner e-mails: ${ownership.node.secondaryOwnerEmails}"
                  } else {
                    println "Ownership of ${nodeName} is disabled"
                  }
                }""";
        WorkflowRun run = buildAndAssertSuccess(script, d, "printNodeOwnershipInfo");
        assertThat(run.getLog(), containsString("ownerOfJenkins"));
    }
    
    private WorkflowRun buildSnippetAndAssertSuccess(@Nonnull String snippetName,
            @Nonnull OwnershipDescription ownershipDescription) throws Exception {
        return buildAndAssertSuccess(OwnershipGlobalVariable.getSampleSnippet(snippetName),
                ownershipDescription, snippetName);
    }

    private WorkflowRun buildAndAssertSuccess(@Nonnull String pipelineScript,
            @Nonnull OwnershipDescription ownershipDescription,
            @Nonnull String projectName) throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, projectName);
        job.setDefinition(new CpsFlowDefinition(pipelineScript, useSandbox));
        JobOwnerHelper.setOwnership(job, ownershipDescription);
        
        // We are going to run Pipeline as System, and the script won't be automatically approved in such case.
        // So we approve the script.
        if (!useSandbox) {
            ScriptApproval.get().preapprove(pipelineScript, GroovyLanguage.get());
        }
        
        return buildAndAssertSuccess(job);
    }

    private WorkflowRun buildAndAssertSuccess(@Nonnull WorkflowJob job) throws Exception {
        QueueTaskFuture<WorkflowRun> runFuture = job.scheduleBuild2(0);
        assertThat("build was actually scheduled", runFuture, notNullValue());
        WorkflowRun run = runFuture.get();

        if (run.getResult() != Result.SUCCESS) {
            // Print the log to help debug the issue
            System.err.println("Pipeline run " + run + " failed. Log:");
            System.err.println(run.getLog());
        }
        assertThat("Pipeline run " + run + " failed", run.getResult(), equalTo(Result.SUCCESS));
        return run;
    }
}
