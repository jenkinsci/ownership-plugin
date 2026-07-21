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
package org.jenkinsci.plugins.ownership.integrations.securityinspector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import hudson.model.FreeStyleProject;
import hudson.model.TopLevelItem;
import hudson.model.User;
import java.util.Collections;
import java.util.List;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for package-private {@link OwnerFilter}.
 */
@WithJenkins
class OwnerFilterTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) throws Exception {
        j = rule;
        OwnershipPluginConfigurer.forJenkinsRule(j).configure();
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
    }

    @Test
    void doFilter_returnsOwnedItemsForUser() throws Exception {
        FreeStyleProject owned = j.createFreeStyleProject("owned-by-alice");
        FreeStyleProject other = j.createFreeStyleProject("owned-by-bob");
        JobOwnerHelper.setOwnership(owned, new OwnershipDescription(true, "alice", Collections.emptyList()));
        JobOwnerHelper.setOwnership(other, new OwnershipDescription(true, "bob", Collections.emptyList()));

        User alice = User.getById("alice", true);
        List<TopLevelItem> items = new OwnerFilter().doFilter(alice);

        assertThat(items, hasItem(owned));
        assertThat(items, not(hasItem(other)));
    }

    @Test
    void doFilter_includesSecondaryOwners() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("with-secondary");
        JobOwnerHelper.setOwnership(job, new OwnershipDescription(true, "alice", Collections.singletonList("bob")));

        User bob = User.getById("bob", true);
        List<TopLevelItem> items = new OwnerFilter().doFilter(bob);
        assertThat(items, contains(job));
    }

    @Test
    void doFilter_skipsJobsWithoutOwnership() throws Exception {
        j.createFreeStyleProject("no-ownership");
        User alice = User.getById("alice", true);
        assertThat(new OwnerFilter().doFilter(alice), empty());
    }
}
