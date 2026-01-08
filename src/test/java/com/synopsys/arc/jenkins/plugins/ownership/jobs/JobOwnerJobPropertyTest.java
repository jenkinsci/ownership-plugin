/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
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

import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import hudson.cli.CLICommandInvoker;
import hudson.cli.UpdateJobCommand;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Job;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static hudson.cli.CLICommandInvoker.Matcher.failedWith;
import static hudson.cli.CLICommandInvoker.Matcher.succeededSilently;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@WithJenkins
class JobOwnerJobPropertyTest {

    private JenkinsRule r;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        r = rule;
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        MockAuthorizationStrategy mas = new MockAuthorizationStrategy();
        mas.grant(Jenkins.ADMINISTER) // Implies MANAGE_ITEMS_OWNERSHIP.
                .everywhere()
                .to("admin");
        mas.grant(Item.CONFIGURE, Item.READ, Jenkins.READ)
                .everywhere()
                .to("non-admin");
        r.jenkins.setAuthorizationStrategy(mas);
    }

    @Test
    @Issue("SECURITY-498")
    void changeOwnerViaPost() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject();
        p.getProperty(JobOwnerJobProperty.class).setOwnershipDescription(new OwnershipDescription(true, "admin", null));

        WebClient wc = r.createWebClient();
        wc.login("non-admin", "non-admin");
        WebRequest req = new WebRequest(wc.createCrumbedUrl(String.format("%sconfig.xml", p.getUrl())), HttpMethod.POST);
        req.setAdditionalHeader("Content-Type", "application/xml");
        req.setRequestBody(getJobXml("admin"));
        wc.getPage(req);
        assertThat("Users should be able to configure jobs when ownership is unchanged",
                getPrimaryOwner(p), is(equalTo("admin")));

        try {
            wc.login("non-admin", "non-admin");
            req.setRequestBody(getJobXml("non-admin"));
            wc.getPage(req);
            fail("Users should not be able to configure job ownership without Manger Ownership/Jobs permissions");
        } catch (FailingHttpStatusCodeException e) {
            assertThat(getPrimaryOwner(p), is(equalTo("admin")));
        }

        wc.login("admin", "admin");
        req.setRequestBody(getJobXml("non-admin"));
        wc.getPage(req);
        assertThat("Users with Manage Ownership/Jobs permissions should be able to change ownership",
                getPrimaryOwner(p), is(equalTo("non-admin")));
    }

    @Test
    @Issue("SECURITY-498")
    void changeOwnerViaCLI() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject();
        p.getProperty(JobOwnerJobProperty.class).setOwnershipDescription(new OwnershipDescription(true, "admin", null));

        CLICommandInvoker command = new CLICommandInvoker(r, new UpdateJobCommand())
                .asUser("non-admin")
                .withArgs(p.getFullName())
                .withStdin(getJobXmlAsStream("admin"));
        assertThat("Users without Overall/Administer permissions should not be able to configure jobs via CLI", 
                command.invoke(), failedWith(1));
        assertThat(getPrimaryOwner(p), is(equalTo("admin")));

        command.asUser("admin")
                .withArgs(p.getFullName())
                .withStdin(getJobXmlAsStream("non-admin"));
        assertThat("Users with Overall/Administer permissions should be able to configure jobs via CLI", 
                command.invoke(), succeededSilently());
        assertThat(getPrimaryOwner(p), is(equalTo("non-admin")));
    }

    private String getPrimaryOwner(Job<?,?> job) {
        return job.getProperty(JobOwnerJobProperty.class).getOwnership().getPrimaryOwnerId();
    }

    private String getJobXml(String ownerSid) {
        return String.format(JOB_XML_TEMPLATE, ownerSid);
    }

    private InputStream getJobXmlAsStream(String ownerSid) {
        return new ByteArrayInputStream(getJobXml(ownerSid).getBytes(StandardCharsets.UTF_8));
    }

    private static final String JOB_XML_TEMPLATE =
            "<?xml version='1.0' encoding='UTF-8'?>" +
            "<project>" +
            "<properties>" +
            "    <com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerJobProperty plugin=\"ownership@0.10.1-SNAPSHOT\">" +
            "        <ownership>" +
            "           <ownershipEnabled>true</ownershipEnabled>" +
            "           <primaryOwnerId>%s</primaryOwnerId>" +
            "           <coownersIds class=\"sorted-set\"/>" +
            "       </ownership>" +
            "   </com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerJobProperty>" +
            "</properties>" +
            "</project>";
}
