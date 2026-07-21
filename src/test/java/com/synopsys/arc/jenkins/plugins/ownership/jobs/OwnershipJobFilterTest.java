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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import hudson.model.FreeStyleProject;
import hudson.model.ListView;
import hudson.model.TopLevelItem;
import hudson.model.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for {@link OwnershipJobFilter}.
 */
@WithJenkins
class OwnershipJobFilterTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) throws Exception {
        j = rule;
        OwnershipPluginConfigurer.forJenkinsRule(j).configure();
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        User.getById("alice", true);
        User.getById("bob", true);
    }

    @Test
    void filter_matchesPrimaryOwner() throws Exception {
        FreeStyleProject owned = j.createFreeStyleProject("owned");
        FreeStyleProject other = j.createFreeStyleProject("other");
        JobOwnerHelper.setOwnership(owned, new OwnershipDescription(true, "alice", Collections.emptyList()));
        JobOwnerHelper.setOwnership(other, new OwnershipDescription(true, "bob", Collections.emptyList()));

        ListView view = new ListView("v", j.jenkins);
        OwnershipJobFilter filter = new OwnershipJobFilter("alice", false);
        List<TopLevelItem> result = filter.filter(Arrays.asList(owned, other), Arrays.asList(owned, other), view);

        assertThat(result, contains(owned));
    }

    @Test
    void filter_matchesSecondaryOwner_whenEnabled() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("job");
        JobOwnerHelper.setOwnership(job, new OwnershipDescription(true, "alice", Collections.singletonList("bob")));

        ListView view = new ListView("v", j.jenkins);
        OwnershipJobFilter primaryOnly = new OwnershipJobFilter("bob", false);
        assertThat(primaryOnly.filter(Collections.singletonList(job), Collections.singletonList(job), view), empty());

        OwnershipJobFilter withSecondary = new OwnershipJobFilter("bob", true);
        assertThat(
                withSecondary.filter(Collections.singletonList(job), Collections.singletonList(job), view),
                contains(job));
        assertThat(withSecondary.isAcceptSecondaryOwners(), equalTo(true));
        assertThat(withSecondary.isAcceptsCoowners(), equalTo(true));
    }

    @Test
    void filter_skipsJobsWithoutOwnership() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("bare");
        ListView view = new ListView("v", j.jenkins);
        OwnershipJobFilter filter = new OwnershipJobFilter("alice", true);
        assertThat(filter.filter(Collections.singletonList(job), Collections.singletonList(job), view), empty());
    }

    @Test
    void availableUsers_includeMeMacro() {
        boolean hasMe = OwnershipJobFilter.getAvailableUsers().stream().anyMatch(u -> "@Me".equals(u.getId()));
        assertThat(hasMe, equalTo(true));
    }
}
