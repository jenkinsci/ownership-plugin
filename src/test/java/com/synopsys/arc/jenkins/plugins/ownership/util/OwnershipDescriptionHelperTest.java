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
package com.synopsys.arc.jenkins.plugins.ownership.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import hudson.model.User;
import hudson.tasks.Mailer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for {@link OwnershipDescriptionHelper}.
 */
@WithJenkins
class OwnershipDescriptionHelperTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) throws Exception {
        j = rule;
        OwnershipPluginConfigurer.forJenkinsRule(j).configure();
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        User alice = User.getById("alice", true);
        alice.addProperty(new Mailer.UserProperty("alice@example.com"));
        User bob = User.getById("bob", true);
        bob.addProperty(new Mailer.UserProperty("bob@example.com"));
    }

    @Test
    void primaryOwnerIdAndDeprecatedAlias() {
        OwnershipDescription descr = new OwnershipDescription(true, "alice", Collections.emptyList());
        assertThat(OwnershipDescriptionHelper.getPrimaryOwnerId(descr), equalTo("alice"));
        assertThat(OwnershipDescriptionHelper.getOwnerID(descr), equalTo("alice"));
    }

    @Test
    void primaryOwnerEmail() {
        OwnershipDescription descr = new OwnershipDescription(true, "alice", Collections.emptyList());
        assertThat(OwnershipDescriptionHelper.getPrimaryOwnerEmail(descr), equalTo("alice@example.com"));
        assertThat(OwnershipDescriptionHelper.getOwnerEmail(descr), equalTo("alice@example.com"));
    }

    @Test
    void primaryOwnerEmail_unknownUser_isEmpty() {
        OwnershipDescription descr = new OwnershipDescription(true, "nobody", Collections.emptyList());
        assertThat(OwnershipDescriptionHelper.getPrimaryOwnerEmail(descr), is(""));
    }

    @Test
    void secondaryOwnerIds_withAndWithoutPrimary() {
        OwnershipDescription descr = new OwnershipDescription(true, "alice", Arrays.asList("bob", "carol"));
        Set<String> withoutPrimary = OwnershipDescriptionHelper.getSecondaryOwnerIds(descr, false);
        assertThat(withoutPrimary, containsInAnyOrder("bob", "carol"));

        Set<String> withPrimary = OwnershipDescriptionHelper.getSecondaryOwnerIds(descr, true);
        assertThat(withPrimary, containsInAnyOrder("alice", "bob", "carol"));
    }

    @Test
    void allOwnerIdsString() {
        OwnershipDescription descr = new OwnershipDescription(true, "alice", Collections.singletonList("bob"));
        String ids = OwnershipDescriptionHelper.getAllOwnerIdsString(descr);
        assertThat(ids, equalTo("alice,bob"));
    }

    @Test
    void secondaryOwnerEmails_andAllEmailsString() {
        OwnershipDescription descr = new OwnershipDescription(true, "alice", Collections.singletonList("bob"));
        Set<String> emails = OwnershipDescriptionHelper.getSecondaryOwnerEmails(descr, true);
        assertThat(emails, contains("alice@example.com", "bob@example.com"));

        String all = OwnershipDescriptionHelper.getAllOwnerEmailsString(descr);
        assertThat(all, equalTo("alice@example.com,bob@example.com"));
    }
}
