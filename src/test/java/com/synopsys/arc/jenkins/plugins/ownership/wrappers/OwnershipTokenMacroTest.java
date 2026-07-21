/*
 * The MIT License
 *
 * Copyright 2026 Ownership Plugin contributors.
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

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.nodes.NodeOwnerHelper;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Slave;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.tasks.Mailer;
import java.util.Collections;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link OwnershipTokenMacro}.
 */
@WithJenkins
class OwnershipTokenMacroTest {

    private JenkinsRule j;
    private Slave node;
    private FreeStyleProject project;

    @BeforeEach
    void beforeEach(JenkinsRule rule) throws Exception {
        j = rule;
        OwnershipPluginConfigurer.forJenkinsRule(j).configure();
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        User alice = User.getById("alice", true);
        alice.addProperty(new Mailer.UserProperty("alice@example.com"));
        User bob = User.getById("bob", true);
        bob.addProperty(new Mailer.UserProperty("bob@example.com"));
        User.getById("carol", true);

        node = j.createOnlineSlave();
        NodeOwnerHelper.setOwnership(node, new OwnershipDescription(true, "bob", Collections.emptyList()));

        project = j.createFreeStyleProject();
        project.setAssignedNode(node);
        JobOwnerHelper.setOwnership(project,
                new OwnershipDescription(true, "alice", Collections.singletonList("carol")));
    }

    @Test
    void acceptsMacroName() {
        OwnershipTokenMacro macro = new OwnershipTokenMacro();
        assertTrue(macro.acceptsMacroName("OWNERSHIP"));
        assertThat(macro.acceptsMacroName("OTHER"), equalTo(false));
    }

    @Test
    void evaluate_jobOwner() throws Exception {
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        String result = TokenMacro.expand(build, TaskListener.NULL, "${OWNERSHIP,var=\"JOB_OWNER\"}");
        assertThat(result, equalTo("alice"));
    }

    @Test
    void evaluate_jobCoowners() throws Exception {
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        String result = TokenMacro.expand(build, TaskListener.NULL, "${OWNERSHIP,var=\"JOB_COOWNERS\"}");
        assertThat(result, equalTo("alice,carol"));
    }

    @Test
    void evaluate_nodeOwner() throws Exception {
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        String result = TokenMacro.expand(build, TaskListener.NULL, "${OWNERSHIP,var=\"NODE_OWNER\"}");
        assertThat(result, equalTo("bob"));
    }

    @Test
    void evaluate_jobOwnerEmail() throws Exception {
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        String result = TokenMacro.expand(build, TaskListener.NULL, "${OWNERSHIP,var=\"JOB_OWNER_EMAIL\"}");
        assertThat(result, equalTo("alice@example.com"));
    }

    @Test
    void evaluate_requiresVar() throws Exception {
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        OwnershipTokenMacro macro = new OwnershipTokenMacro();
        MacroEvaluationException ex = assertThrows(MacroEvaluationException.class,
                () -> macro.evaluate(build, TaskListener.NULL, "OWNERSHIP"));
        assertThat(ex.getMessage(), containsString("var"));
    }

    @Test
    void evaluate_unsupportedVar() throws Exception {
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        OwnershipTokenMacro macro = new OwnershipTokenMacro();
        macro.var = "NOT_A_THING";
        MacroEvaluationException ex = assertThrows(MacroEvaluationException.class,
                () -> macro.evaluate(build, TaskListener.NULL, "OWNERSHIP"));
        assertThat(ex.getMessage(), containsString("NOT_A_THING"));
    }
}
