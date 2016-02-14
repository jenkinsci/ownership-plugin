/*
 * The MIT License
 *
 * Copyright (c) 2015 Red Hat, Inc.
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
package com.synopsys.arc.jenkins.plugins.ownership;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;

import hudson.model.FreeStyleProject;
import hudson.model.User;
import hudson.tasks.Mailer;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.nodes.NodeOwnerHelper;

public class OwnershipActionTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @Test
    public void test() throws Exception {
        // There is no particular reason why email value should look like this, but for a user configurable field this is a realistic scenario.
        String mail = "\"T&J\" <TnJ@mailinator.com>";
        String id = "_T&J_";
        User user = User.get("<T&J>");
        user.addProperty(new Mailer.UserProperty(mail));

        FreeStyleProject project = j.createFreeStyleProject();
        JobOwnerHelper.setOwnership(project, new OwnershipDescription(true, id, Arrays.asList(id)));
        NodeOwnerHelper.setOwnership(j.jenkins, new OwnershipDescription(true, id, Arrays.asList(id)));

        final WebClient wc = j.createWebClient();

        final HtmlPage job = wc.getPage(project);
        assertThat(job.asXml(), not(containsString("<T&J>")));
        job.getAnchorByHref(j.getURL() + "user/" + id).click();

        final HtmlPage slave = wc.getPage(j.jenkins);
        assertThat(slave.asXml(), not(containsString("<T&J>")));
        job.getAnchorByHref(j.getURL() + "user/" + id).click();
    }
}
