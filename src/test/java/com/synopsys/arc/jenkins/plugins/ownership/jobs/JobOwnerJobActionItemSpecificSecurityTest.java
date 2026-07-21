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
package com.synopsys.arc.jenkins.plugins.ownership.jobs;

import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlCheckBoxInput;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.security.itemspecific.ItemSpecificSecurity;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.AuthorizationMatrixProperty;
import hudson.security.Permission;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional coverage for JobOwnerJobAction item-specific security flows
 * beyond CSRF/permission gates.
 */
@WithJenkins
class JobOwnerJobActionItemSpecificSecurityTest {

    private JenkinsRule r;

    private FreeStyleProject project;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        r = rule;
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        project = r.createFreeStyleProject("owned-job");
        JobOwnerHelper.setOwnership(project, new OwnershipDescription(true, "admin", null));

        MockAuthorizationStrategy mas = new MockAuthorizationStrategy();
        mas.grant(Jenkins.ADMINISTER).everywhere().to("admin");
        mas.grant(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP).onItems(project).to("admin");
        mas.grant(Item.READ, Item.CONFIGURE).onItems(project).to("admin");
        r.jenkins.setAuthorizationStrategy(mas);
    }

    @Test
    void restoreDefault_clonesGlobalDefaultsOntoJob() throws Exception {
        ItemSpecificSecurity defaults = matrixWith(Item.READ, "alice");
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withDefaultJobsSecurity(defaults)
                .configure();

        JobOwnerHelper.setProjectSpecificSecurity(project, matrixWith(Item.BUILD, "bob"));
        assertTrue(action().hasItemSpecificSecurity());

        JenkinsRule.WebClient wc = r.createWebClient();
        wc.login("admin", "admin");
        WebRequest req = new WebRequest(
                wc.createCrumbedUrl(project.getUrl() + "ownership/restoreDefaultSpecificSecuritySubmit"),
                HttpMethod.POST);
        wc.getPage(req);

        JobOwnerJobProperty prop = JobOwnerHelper.getOwnerProperty(project);
        assertThat(prop, notNullValue());
        assertTrue(prop.hasItemSpecificSecurity());
        assertTrue(prop.getItemSpecificSecurity().getPermissionsMatrix()
                .getGrantedPermissionEntries().get(Item.READ)
                .stream().anyMatch(e -> "alice".equals(e.getSid())));
        assertThat(prop.getItemSpecificSecurity(), not(sameInstance(defaults)));
    }

    @Test
    void restoreDefault_whenNoGlobalDefaults_clearsJobSecurity() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(r).configure();
        JobOwnerHelper.setProjectSpecificSecurity(project, matrixWith(Item.READ, "alice"));
        assertTrue(action().hasItemSpecificSecurity());

        JenkinsRule.WebClient wc = r.createWebClient();
        wc.login("admin", "admin");
        WebRequest req = new WebRequest(
                wc.createCrumbedUrl(project.getUrl() + "ownership/restoreDefaultSpecificSecuritySubmit"),
                HttpMethod.POST);
        wc.getPage(req);

        assertFalse(action().hasItemSpecificSecurity());
        assertThat(JobOwnerHelper.getOwnerProperty(project).getItemSpecificSecurity(), nullValue());
    }

    @Test
    void projectSpecificSecuritySubmit_withoutOptionalBlock_clearsSecurity() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(r).configure();
        JobOwnerHelper.setProjectSpecificSecurity(project, matrixWith(Item.READ, "alice"));
        assertTrue(action().hasItemSpecificSecurity());

        JenkinsRule.WebClient wc = r.createWebClient();
        wc.login("admin", "admin");
        HtmlPage page = wc.goTo(project.getUrl() + "ownership/configure-project-specifics");
        HtmlForm form = page.getFormByName("config");
        HtmlCheckBoxInput itemSpecific = form.getInputByName("itemSpecificSecurity");
        assertThat(itemSpecific, notNullValue());
        if (itemSpecific.isChecked()) {
            itemSpecific.setChecked(false);
        }
        r.submit(form);

        assertFalse(action().hasItemSpecificSecurity());
        assertThat(JobOwnerHelper.getOwnerProperty(project).getItemSpecificSecurity(), nullValue());
    }

    @Test
    void getItemSpecificSecurity_fallsBackToGlobalDefaults() throws Exception {
        ItemSpecificSecurity defaults = matrixWith(Item.READ, "alice");
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withDefaultJobsSecurity(defaults)
                .configure();

        assertFalse(action().hasItemSpecificSecurity());
        ItemSpecificSecurity resolved = action().getItemSpecificSecurity();
        assertThat(resolved, sameInstance(defaults));
        assertTrue(resolved.getPermissionsMatrix().getGrantedPermissionEntries().get(Item.READ)
                .stream().anyMatch(e -> "alice".equals(e.getSid())));
    }

    @Test
    void getItemSpecificSecurity_prefersJobOverrideOverGlobal() throws Exception {
        ItemSpecificSecurity defaults = matrixWith(Item.READ, "alice");
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withDefaultJobsSecurity(defaults)
                .configure();
        ItemSpecificSecurity local = matrixWith(Item.BUILD, "bob");
        JobOwnerHelper.setProjectSpecificSecurity(project, local);

        assertTrue(action().hasItemSpecificSecurity());
        assertThat(action().getItemSpecificSecurity(), sameInstance(local));
    }

    @Test
    void actionIsAvailable_requiresItemRead() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(r).configure();
        assertTrue(action().actionIsAvailable());

        MockAuthorizationStrategy mas = new MockAuthorizationStrategy();
        mas.grant(Jenkins.ADMINISTER).everywhere().to("admin");
        r.jenkins.setAuthorizationStrategy(mas);

        User nobody = User.getById("nobody", true);
        try (hudson.security.ACLContext ignored = ACL.as(nobody)) {
            assertThat(action().actionIsAvailable(), is(false));
        }
    }

    private JobOwnerJobAction action() {
        JobOwnerJobAction action = project.getAction(JobOwnerJobAction.class);
        assertThat(action, notNullValue());
        return action;
    }

    private static ItemSpecificSecurity matrixWith(Permission permission, String sid) {
        Map<Permission, Set<String>> permissions = new HashMap<>();
        permissions.put(permission, Collections.singleton(sid));
        return new ItemSpecificSecurity(new AuthorizationMatrixProperty(permissions));
    }
}
