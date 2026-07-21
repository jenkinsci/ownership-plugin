/*
 * The MIT License
 *
 * Copyright 2026
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
package org.jenkinsci.plugins.ownership.model.folders;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import hudson.model.Item;
import hudson.model.User;
import jenkins.model.Jenkins;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Security tests for FolderOwnershipAction item-specific security endpoints.
 * Mirrors {@link com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerJobActionSecurityTest}.
 */
@WithJenkins
class FolderOwnershipActionSecurityTest {

    private JenkinsRule r;

    private Folder folder;

    @BeforeEach
    void beforeEach(JenkinsRule rule) throws Exception {
        r = rule;
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());

        User.get("admin", true);
        User.get("readonly-user", true);

        folder = r.jenkins.createProject(Folder.class, "test-folder");
        FolderOwnershipHelper.setOwnership(folder, new OwnershipDescription(true, "admin", null));

        MockAuthorizationStrategy mas = new MockAuthorizationStrategy();
        mas.grant(Jenkins.ADMINISTER).everywhere().to("admin");
        mas.grant(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP).onItems(folder).to("admin");
        mas.grant(Item.READ, Jenkins.READ).everywhere().to("readonly-user");
        mas.grant(Item.READ).onItems(folder).to("readonly-user");

        r.jenkins.setAuthorizationStrategy(mas);
        r.jenkins.save();
    }

    @Test
    @Issue("SECURITY-2062")
    void doProjectSpecificSecuritySubmit_requiresPOST() throws Exception {
        WebClient wc = r.createWebClient();
        wc.login("admin", "admin");

        try {
            WebRequest req = new WebRequest(
                    wc.createCrumbedUrl(folder.getUrl() + "ownership/projectSpecificSecuritySubmit"),
                    HttpMethod.GET);
            wc.getPage(req);
            fail("GET request should be rejected for doProjectSpecificSecuritySubmit");
        } catch (FailingHttpStatusCodeException e) {
            assertThat("GET request should be rejected", e.getStatusCode(), is(405));
        }
    }

    @Test
    @Issue("SECURITY-2062")
    void doProjectSpecificSecuritySubmit_requiresManageOwnershipPermission() throws Exception {
        WebClient wc = r.createWebClient();
        wc.login("readonly-user", "readonly-user");

        try {
            WebRequest req = new WebRequest(
                    wc.createCrumbedUrl(folder.getUrl() + "ownership/projectSpecificSecuritySubmit"),
                    HttpMethod.POST);
            req.setAdditionalHeader("Content-Type", "application/x-www-form-urlencoded");
            req.setRequestBody("");
            wc.getPage(req);
            fail("User with only READ permission should not be able to modify folder-specific security");
        } catch (FailingHttpStatusCodeException e) {
            assertThat("User without MANAGE_ITEMS_OWNERSHIP should be rejected",
                    e.getStatusCode(), is(403));
        }
    }

    @Test
    @Issue("SECURITY-2062")
    void doRestoreDefaultSpecificSecuritySubmit_requiresPOST() throws Exception {
        WebClient wc = r.createWebClient();
        wc.login("admin", "admin");

        try {
            WebRequest req = new WebRequest(
                    wc.createCrumbedUrl(folder.getUrl() + "ownership/restoreDefaultSpecificSecuritySubmit"),
                    HttpMethod.GET);
            wc.getPage(req);
            fail("GET request should be rejected for doRestoreDefaultSpecificSecuritySubmit");
        } catch (FailingHttpStatusCodeException e) {
            assertThat("GET request should be rejected", e.getStatusCode(), is(405));
        }
    }

    @Test
    @Issue("SECURITY-2062")
    void doRestoreDefaultSpecificSecuritySubmit_requiresManageOwnershipPermission() throws Exception {
        WebClient wc = r.createWebClient();
        wc.login("readonly-user", "readonly-user");

        try {
            WebRequest req = new WebRequest(
                    wc.createCrumbedUrl(folder.getUrl() + "ownership/restoreDefaultSpecificSecuritySubmit"),
                    HttpMethod.POST);
            req.setAdditionalHeader("Content-Type", "application/x-www-form-urlencoded");
            req.setRequestBody("");
            wc.getPage(req);
            fail("User with only READ permission should not be able to restore default security");
        } catch (FailingHttpStatusCodeException e) {
            assertThat("User without MANAGE_ITEMS_OWNERSHIP should be rejected",
                    e.getStatusCode(), is(403));
        }
    }
}
