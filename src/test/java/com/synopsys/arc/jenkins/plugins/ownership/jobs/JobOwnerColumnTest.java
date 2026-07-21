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
package com.synopsys.arc.jenkins.plugins.ownership.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import hudson.model.FreeStyleProject;
import hudson.model.User;
import java.util.Collections;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for {@link JobOwnerColumn}.
 */
@WithJenkins
class JobOwnerColumnTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) throws Exception {
        j = rule;
        OwnershipPluginConfigurer.forJenkinsRule(j).configure();
    }

    @Test
    void getJobOwner_andExists_withOwnership() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        User.getById("alice", true);
        FreeStyleProject job = j.createFreeStyleProject();
        JobOwnerHelper.setOwnership(job, new OwnershipDescription(true, "alice", Collections.emptyList()));

        JobOwnerColumn column = new JobOwnerColumn();
        assertThat(column.getJobOwner(job), equalTo("alice"));
        assertTrue(column.isOwnerExists(job));
    }

    @Test
    void getJobOwner_withoutOwnership() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject();
        JobOwnerColumn column = new JobOwnerColumn();
        assertThat(column.getJobOwner(job), equalTo(User.getUnknown().getId()));
        assertFalse(column.isOwnerExists(job));
    }

    @Test
    void descriptor() {
        JobOwnerColumn.DescriptorImpl descriptor = new JobOwnerColumn.DescriptorImpl();
        assertFalse(descriptor.shownByDefault());
        assertThat(
                descriptor.getDisplayName(),
                equalTo(com.synopsys.arc.jenkins.plugins.ownership.Messages.JobOwnership_Column_Title()));
    }
}
