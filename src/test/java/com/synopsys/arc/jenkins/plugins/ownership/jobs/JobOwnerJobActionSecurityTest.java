/*
 * The MIT License
 *
 * Copyright 2022
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
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.User;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Security tests for JobOwnerJobAction endpoints.
 * Tests CSRF protection and permission checks for SECURITY-2062 fixes.
 */
@WithJenkins
class JobOwnerJobActionSecurityTest {

    private JenkinsRule r;

    private FreeStyleProject project;

    @BeforeEach
    void beforeEach(JenkinsRule rule) throws Exception{
        r = rule;
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        
        // Create users explicitly
        User.get("admin", true);
        User.get("readonly-user", true);
        User.get("configure-user", true);
        
        // Create project first, before setting up authorization strategy
        project = r.createFreeStyleProject("test-project");
        JobOwnerHelper.setOwnership(project, new OwnershipDescription(true, "admin", null));
        
        MockAuthorizationStrategy mas = new MockAuthorizationStrategy();
        
        // Admin has all permissions including MANAGE_ITEMS_OWNERSHIP
        mas.grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("admin");
        
        // Explicitly grant MANAGE_ITEMS_OWNERSHIP on the specific project
        mas.grant(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP)
                .onItems(project)
                .to("admin");
        
        // User with only READ permission (should not be able to modify ownership)
        // Grant READ on the project so action is available, but methods will check MANAGE_ITEMS_OWNERSHIP
        mas.grant(Item.READ, Jenkins.READ)
                .everywhere()
                .to("readonly-user");
        mas.grant(Item.READ)
                .onItems(project)
                .to("readonly-user");
        
        // User with CONFIGURE but not MANAGE_ITEMS_OWNERSHIP (should not be able to modify ownership)
        // Grant READ on the project so action is available, but methods will check MANAGE_ITEMS_OWNERSHIP
        mas.grant(Item.CONFIGURE, Item.READ, Jenkins.READ)
                .everywhere()
                .to("configure-user");
        mas.grant(Item.READ)
                .onItems(project)
                .to("configure-user");
        
        r.jenkins.setAuthorizationStrategy(mas);
        r.jenkins.save(); // Save configuration to ensure it's persisted
    }

    @Test
    @Issue("SECURITY-2062")
    void doOwnersSubmit_requiresPOST() throws Exception {
        WebClient wc = r.createWebClient();
        wc.login("admin", "admin");
        
        // Try GET request - should fail
        try {
            WebRequest req = new WebRequest(
                    wc.createCrumbedUrl(project.getUrl() + "ownership/ownersSubmit"), 
                    HttpMethod.GET);
            wc.getPage(req);
            fail("GET request should be rejected for doOwnersSubmit");
        } catch (FailingHttpStatusCodeException e) {
            // Expected: should return 405 Method Not Allowed or similar
            assertThat("GET request should be rejected", e.getStatusCode(), is(405));
        }
    }

    @Test
    @Issue("SECURITY-2062")
    void doOwnersSubmit_requiresManageOwnershipPermission() throws Exception {
        WebClient wc = r.createWebClient();
        
        // Try with readonly user - should fail
        wc.login("readonly-user", "readonly-user");
        try {
            WebRequest req = new WebRequest(
                    wc.createCrumbedUrl(project.getUrl() + "ownership/ownersSubmit"), 
                    HttpMethod.POST);
            req.setAdditionalHeader("Content-Type", "application/x-www-form-urlencoded");
            req.setRequestBody("owners=" + createOwnershipJSON("readonly-user"));
            wc.getPage(req);
            fail("User with only READ permission should not be able to change ownership");
        } catch (FailingHttpStatusCodeException e) {
            // Expected: should return 403 Forbidden
            assertThat("User without MANAGE_ITEMS_OWNERSHIP should be rejected", 
                    e.getStatusCode(), is(403));
        }
        
        // Verify ownership was not changed
        assertThat(getPrimaryOwner(project), is(equalTo("admin")));
    }

    @Test
    @Issue("SECURITY-2062")
    void doOwnersSubmit_allowsPOSTWithProperPermissions() throws Exception {
        WebClient wc = r.createWebClient();
        wc.login("admin", "admin");
        
        // POST request with proper permissions should work
        // Use direct POST request like other tests, with proper form data
        // Stapler expects form data with JSON object for "owners" field
        JSONObject ownersJson = new JSONObject();
        ownersJson.put("primaryOwner", "new-owner");
        JSONObject formData = new JSONObject();
        formData.put("owners", ownersJson);
        
        WebRequest req = new WebRequest(
                wc.createCrumbedUrl(project.getUrl() + "ownership/ownersSubmit"), 
                HttpMethod.POST);
        req.setAdditionalHeader("Content-Type", "application/x-www-form-urlencoded");
        req.setRequestBody("json=" + formData);
        wc.getPage(req);
        
        // Verify ownership was changed
        assertThat(getPrimaryOwner(project), is(equalTo("new-owner")));
    }

    @Test
    @Issue("SECURITY-2062")
    void doProjectSpecificSecuritySubmit_requiresPOST() throws Exception {
        WebClient wc = r.createWebClient();
        wc.login("admin", "admin");
        
        // Try GET request - should fail
        try {
            WebRequest req = new WebRequest(
                    wc.createCrumbedUrl(project.getUrl() + "ownership/projectSpecificSecuritySubmit"), 
                    HttpMethod.GET);
            wc.getPage(req);
            fail("GET request should be rejected for doProjectSpecificSecuritySubmit");
        } catch (FailingHttpStatusCodeException e) {
            // Expected: should return 405 Method Not Allowed or similar
            assertThat("GET request should be rejected", e.getStatusCode(), is(405));
        }
    }

    @Test
    @Issue("SECURITY-2062")
    void doProjectSpecificSecuritySubmit_requiresManageOwnershipPermission() throws Exception {
        WebClient wc = r.createWebClient();
        
        // Try with readonly user - should fail
        wc.login("readonly-user", "readonly-user");
        try {
            WebRequest req = new WebRequest(
                    wc.createCrumbedUrl(project.getUrl() + "ownership/projectSpecificSecuritySubmit"), 
                    HttpMethod.POST);
            req.setAdditionalHeader("Content-Type", "application/x-www-form-urlencoded");
            req.setRequestBody(""); // Empty form
            wc.getPage(req);
            fail("User with only READ permission should not be able to modify project-specific security");
        } catch (FailingHttpStatusCodeException e) {
            // Expected: should return 403 Forbidden
            assertThat("User without MANAGE_ITEMS_OWNERSHIP should be rejected", 
                    e.getStatusCode(), is(403));
        }
    }

    @Test
    @Issue("SECURITY-2062")
    void doRestoreDefaultSpecificSecuritySubmit_requiresPOST() throws Exception {
        WebClient wc = r.createWebClient();
        wc.login("admin", "admin");
        
        // Try GET request - should fail
        try {
            WebRequest req = new WebRequest(
                    wc.createCrumbedUrl(project.getUrl() + "ownership/restoreDefaultSpecificSecuritySubmit"), 
                    HttpMethod.GET);
            wc.getPage(req);
            fail("GET request should be rejected for doRestoreDefaultSpecificSecuritySubmit");
        } catch (FailingHttpStatusCodeException e) {
            // Expected: should return 405 Method Not Allowed or similar
            assertThat("GET request should be rejected", e.getStatusCode(), is(405));
        }
    }

    @Test
    @Issue("SECURITY-2062")
    void doRestoreDefaultSpecificSecuritySubmit_requiresManageOwnershipPermission() throws Exception {
        WebClient wc = r.createWebClient();
        
        // Try with readonly user - should fail
        wc.login("readonly-user", "readonly-user");
        try {
            WebRequest req = new WebRequest(
                    wc.createCrumbedUrl(project.getUrl() + "ownership/restoreDefaultSpecificSecuritySubmit"), 
                    HttpMethod.POST);
            req.setAdditionalHeader("Content-Type", "application/x-www-form-urlencoded");
            req.setRequestBody(""); // Empty form
            wc.getPage(req);
            fail("User with only READ permission should not be able to restore default security");
        } catch (FailingHttpStatusCodeException e) {
            // Expected: should return 403 Forbidden
            assertThat("User without MANAGE_ITEMS_OWNERSHIP should be rejected", 
                    e.getStatusCode(), is(403));
        }
    }

    @Test
    @Issue("SECURITY-2062")
    void configureUser_cannotModifyOwnership() throws Exception {
        WebClient wc = r.createWebClient();
        
        // User with CONFIGURE but not MANAGE_ITEMS_OWNERSHIP should not be able to modify ownership
        wc.login("configure-user", "configure-user");
        try {
            WebRequest req = new WebRequest(
                    wc.createCrumbedUrl(project.getUrl() + "ownership/ownersSubmit"), 
                    HttpMethod.POST);
            req.setAdditionalHeader("Content-Type", "application/x-www-form-urlencoded");
            req.setRequestBody("owners=" + createOwnershipJSON("configure-user"));
            wc.getPage(req);
            fail("User with CONFIGURE but without MANAGE_ITEMS_OWNERSHIP should not be able to change ownership");
        } catch (FailingHttpStatusCodeException e) {
            // Expected: should return 403 Forbidden
            assertThat("User without MANAGE_ITEMS_OWNERSHIP should be rejected", 
                    e.getStatusCode(), is(403));
        }
        
        // Verify ownership was not changed
        assertThat(getPrimaryOwner(project), is(equalTo("admin")));
    }

    private String getPrimaryOwner(FreeStyleProject project) {
        JobOwnerJobProperty prop = JobOwnerHelper.getOwnerProperty(project);
        if (prop != null && prop.getOwnership() != null) {
            return prop.getOwnership().getPrimaryOwnerId();
        }
        return null;
    }

    private String createOwnershipJSON(String ownerId) {
        JSONObject json = new JSONObject();
        json.put("primaryOwner", ownerId);
        return json.toString();
    }
}

