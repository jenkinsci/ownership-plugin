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
package com.synopsys.arc.jenkins.plugins.ownership.nodes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import hudson.model.Node;
import hudson.model.Slave;
import java.util.Arrays;
import java.util.Collections;
import org.jenkinsci.plugins.ownership.model.OwnershipHelperLocator;
import org.jenkinsci.plugins.ownership.model.OwnershipInfo;
import org.jenkinsci.plugins.ownership.model.nodes.NodeOwnershipDescriptionSource;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Tests for {@link NodeOwnerHelper}.
 */
@WithJenkins
class NodeOwnerHelperTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) throws Exception {
        j = rule;
        OwnershipPluginConfigurer.forJenkinsRule(j).configure();
    }

    @Test
    void locatorShouldReturnNodeOwnerHelper() throws Exception {
        Slave node = j.createSlave();
        assertThat(OwnershipHelperLocator.locate(node), sameInstance(NodeOwnerHelper.Instance));
    }

    @Test
    void getOwnershipDescription_withoutProperty_isDisabled() throws Exception {
        Slave node = j.createSlave();
        OwnershipDescription descr = NodeOwnerHelper.Instance.getOwnershipDescription(node);
        assertThat(descr, equalTo(OwnershipDescription.DISABLED_DESCR));
        assertFalse(NodeOwnerHelper.Instance.hasLocallyDefinedOwnership(node));
    }

    @Test
    void getOwnershipDescription_withOwnership_returnsDescription() throws Exception {
        Slave node = j.createSlave();
        OwnershipDescription expected = new OwnershipDescription(true, "alice", Arrays.asList("bob"));
        NodeOwnerHelper.setOwnership(node, expected);

        OwnershipDescription actual = NodeOwnerHelper.Instance.getOwnershipDescription(node);
        assertTrue(actual.isOwnershipEnabled());
        assertThat(actual.getPrimaryOwnerId(), equalTo("alice"));
        assertThat(actual.getSecondaryOwnerIds(), equalTo(expected.getSecondaryOwnerIds()));
        assertTrue(NodeOwnerHelper.Instance.hasLocallyDefinedOwnership(node));
    }

    @Test
    void getOwnershipInfo_withoutProperty_isDisabled() throws Exception {
        Slave node = j.createSlave();
        OwnershipInfo info = NodeOwnerHelper.Instance.getOwnershipInfo(node);
        assertThat(info.getDescription(), equalTo(OwnershipDescription.DISABLED_DESCR));
    }

    @Test
    void getOwnershipInfo_nullNode_isDisabled() {
        OwnershipInfo info = NodeOwnerHelper.Instance.getOwnershipInfo(null);
        assertThat(info.getDescription(), equalTo(OwnershipDescription.DISABLED_DESCR));
        assertThat(
                NodeOwnerHelper.Instance.getOwnershipDescription(null), equalTo(OwnershipDescription.DISABLED_DESCR));
    }

    @Test
    @Issue("94")
    void getOwnershipInfo_withOwnership_matchesDescription() throws Exception {
        Slave node = j.createSlave();
        OwnershipDescription expected = new OwnershipDescription(true, "alice", Collections.singletonList("bob"));
        NodeOwnerHelper.setOwnership(node, expected);

        OwnershipInfo info = NodeOwnerHelper.Instance.getOwnershipInfo(node);
        assertThat(info.getDescription(), equalTo(NodeOwnerHelper.Instance.getOwnershipDescription(node)));
        assertTrue(info.getDescription().isOwnershipEnabled());
        assertThat(info.getDescription().getPrimaryOwnerId(), equalTo("alice"));
        assertThat(info.getSource(), instanceOf(NodeOwnershipDescriptionSource.class));
        assertThat(info.getSource().getItem(), is((Node) node));
    }

    @Test
    void itemMetadata() throws Exception {
        Slave node = j.createSlave();
        assertThat(NodeOwnerHelper.Instance.getItemTypeName(node), equalTo("node"));
        assertThat(NodeOwnerHelper.Instance.getItemDisplayName(node), equalTo(node.getDisplayName()));
    }
}
