/*
 * The MIT License
 *
 * Copyright (c) 2026 Ownership Plugin Contributors.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.tasks.Mailer;
import java.util.Arrays;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Regression for
 * <a href="https://github.com/jenkinsci/ownership-plugin/issues/84">issue #84</a>:
 * a second {@code OwnershipGlobalVariable#getValue} without a binding cache
 * entry must not throw {@code LinkageError} (duplicate Groovy Impl class).
 *
 * <p>The binding-clear script is synthetic (deterministic). Declarative
 * stage + cleanup is a smoke of the original report shape.
 */
@WithJenkins
class OwnershipGlobalVariableLinkageErrorReproTest {

    /** Synthetic double-load is deterministic; one build is enough. */
    private static final int ITERATIONS = 1;

    private static final String DECLARATIVE_PIPELINE = ""
            + "pipeline {\n"
            + "  agent any\n"
            + "  stages {\n"
            + "    stage('A') {\n"
            + "      steps {\n"
            + "        script {\n"
            + "          echo \"stage=${ownership.job.ownershipEnabled}/${ownership.job.primaryOwnerEmail}\"\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "  post {\n"
            + "    cleanup {\n"
            + "      script {\n"
            + "        echo \"cleanup=${ownership.job.ownershipEnabled}/${ownership.job.primaryOwnerEmail}\"\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    /**
     * Force a second {@code getValue} by dropping the binding cache between
     * accesses (same path that intermittent Declarative/shared-library runs hit).
     */
    private static final String SCRIPTED_RELOAD_VIA_BINDING_CLEAR = ""
            + "node {\n"
            + "  echo \"first=${ownership.job.ownershipEnabled}/${ownership.job.primaryOwnerEmail}\"\n"
            + "  binding.variables.remove('ownership')\n"
            + "  echo \"second=${ownership.job.ownershipEnabled}/${ownership.job.primaryOwnerEmail}\"\n"
            + "}\n";

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) throws Exception {
        j = rule;
        Descriptor<?> mailerDescriptor = j.jenkins.getDescriptor(Mailer.class);
        assertThat(mailerDescriptor, instanceOf(Mailer.DescriptorImpl.class));
        ((Mailer.DescriptorImpl) mailerDescriptor).setDefaultSuffix("mailsuff.ix");

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        OwnershipPluginConfigurer.forJenkinsRule(j).configure();
    }

    @Test
    void declarativeStageAndCleanup_succeeds() throws Exception {
        ReproStats stats = runBuilds("issue84-declarative", DECLARATIVE_PIPELINE, true);
        assertNoLinkageError(stats);
        assertThat(stats.lastResult, equalTo(Result.SUCCESS));
    }

    @Test
    void secondGetValueWithoutBindingCache_mustNotLinkageError() throws Exception {
        ReproStats stats = runBuilds("issue84-scripted-binding-clear", SCRIPTED_RELOAD_VIA_BINDING_CLEAR, false);
        assertNoLinkageError(stats);
        assertThat(stats.lastResult, equalTo(Result.SUCCESS));
    }

    private ReproStats runBuilds(String jobName, String pipelineScript, boolean sandbox) throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, jobName);
        job.setDefinition(new CpsFlowDefinition(pipelineScript, sandbox));
        JobOwnerHelper.setOwnership(
                job, new OwnershipDescription(true, "owner", Arrays.asList("coowner1", "coowner2")));
        if (!sandbox) {
            ScriptApproval.get().preapprove(pipelineScript, GroovyLanguage.get());
        }

        int linkageErrors = 0;
        int otherFailures = 0;
        Result lastResult = null;
        String firstLinkageLog = null;

        for (int i = 1; i <= ITERATIONS; i++) {
            QueueTaskFuture<WorkflowRun> future = job.scheduleBuild2(0, new Action[0]);
            assertThat("build " + i + " was scheduled", future, notNullValue());
            WorkflowRun run = future.get();
            lastResult = run.getResult();
            String log = run.getLog();

            if (isLinkageError(log)) {
                linkageErrors++;
                if (firstLinkageLog == null) {
                    firstLinkageLog = log;
                }
            } else if (lastResult != Result.SUCCESS) {
                otherFailures++;
                System.err.println(
                        "[" + jobName + "] Build #" + run.getNumber() + " failed without LinkageError. Log:");
                System.err.println(log);
            }
        }

        System.out.println("issue #84 regression [" + jobName + "]: linkageErrors=" + linkageErrors + "/" + ITERATIONS
                + ", otherFailures=" + otherFailures);
        if (firstLinkageLog != null) {
            System.out.println("First LinkageError build log (truncated):");
            System.out.println(truncate(firstLinkageLog, 4000));
        }
        return new ReproStats(linkageErrors, otherFailures, lastResult);
    }

    private static void assertNoLinkageError(ReproStats stats) {
        assertFalse(
                stats.linkageErrors > 0,
                "expected 0 LinkageError in " + ITERATIONS
                        + " runs, got " + stats.linkageErrors
                        + " (otherFailures=" + stats.otherFailures + ")");
    }

    private static boolean isLinkageError(String log) {
        return log != null
                && log.contains("LinkageError")
                && (log.contains("OwnershipGlobalVariableImpl")
                        || log.contains("duplicate class definition")
                        || log.contains("OwnershipPipelineVar"));
    }

    private static String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "\n...[truncated]...";
    }

    private static final class ReproStats {
        final int linkageErrors;
        final int otherFailures;
        final Result lastResult;

        ReproStats(int linkageErrors, int otherFailures, Result lastResult) {
            this.linkageErrors = linkageErrors;
            this.otherFailures = otherFailures;
            this.lastResult = lastResult;
        }
    }
}
