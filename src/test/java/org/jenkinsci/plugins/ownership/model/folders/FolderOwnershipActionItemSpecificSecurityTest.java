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

import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlCheckBoxInput;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import hudson.model.Item;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.Permission;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.ownership.security.folderspecific.FolderSpecificSecurity;
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
 * Functional coverage for FolderOwnershipAction item-specific security flows.
 */
@WithJenkins
class FolderOwnershipActionItemSpecificSecurityTest {

    private JenkinsRule r;

    private Folder folder;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        r = rule;
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        folder = r.jenkins.createProject(Folder.class, "owned-folder");
        FolderOwnershipHelper.setOwnership(folder, new OwnershipDescription(true, "admin", null));

        MockAuthorizationStrategy mas = new MockAuthorizationStrategy();
        mas.grant(Jenkins.ADMINISTER).everywhere().to("admin");
        mas.grant(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP).onItems(folder).to("admin");
        mas.grant(Item.READ, Item.CONFIGURE).onItems(folder).to("admin");
        r.jenkins.setAuthorizationStrategy(mas);
    }

    @Test
    void restoreDefault_clonesGlobalDefaultsOntoFolder() throws Exception {
        FolderSpecificSecurity defaults = matrixWith(Item.READ, "alice");
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withDefaultFoldersSecurity(defaults)
                .configure();

        FolderOwnershipHelper.setProjectSpecificSecurity(folder, matrixWith(Item.CONFIGURE, "bob"));
        assertTrue(action().hasItemSpecificSecurity());

        JenkinsRule.WebClient wc = r.createWebClient();
        wc.login("admin", "admin");
        WebRequest req = new WebRequest(
                wc.createCrumbedUrl(folder.getUrl() + "ownership/restoreDefaultSpecificSecuritySubmit"),
                HttpMethod.POST);
        wc.getPage(req);

        FolderOwnershipProperty prop = FolderOwnershipHelper.getOwnerProperty(folder);
        assertThat(prop, notNullValue());
        assertTrue(prop.hasItemSpecificSecurity());
        assertTrue(prop.getItemSpecificSecurity().getPermissionsMatrix()
                .getGrantedPermissionEntries().get(Item.READ)
                .stream().anyMatch(e -> "alice".equals(e.getSid())));
        assertThat(prop.getItemSpecificSecurity(), not(sameInstance(defaults)));
    }

    @Test
    void restoreDefault_whenNoGlobalDefaults_clearsFolderSecurity() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(r).configure();
        FolderOwnershipHelper.setProjectSpecificSecurity(folder, matrixWith(Item.READ, "alice"));
        assertTrue(action().hasItemSpecificSecurity());

        JenkinsRule.WebClient wc = r.createWebClient();
        wc.login("admin", "admin");
        WebRequest req = new WebRequest(
                wc.createCrumbedUrl(folder.getUrl() + "ownership/restoreDefaultSpecificSecuritySubmit"),
                HttpMethod.POST);
        wc.getPage(req);

        assertFalse(action().hasItemSpecificSecurity());
        assertThat(FolderOwnershipHelper.getOwnerProperty(folder).getItemSpecificSecurity(), nullValue());
    }

    @Test
    void projectSpecificSecuritySubmit_withoutOptionalBlock_clearsSecurity() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(r).configure();
        FolderOwnershipHelper.setProjectSpecificSecurity(folder, matrixWith(Item.READ, "alice"));
        assertTrue(action().hasItemSpecificSecurity());

        JenkinsRule.WebClient wc = r.createWebClient();
        wc.login("admin", "admin");
        HtmlPage page = wc.goTo(folder.getUrl() + "ownership/configure-project-specifics");
        HtmlForm form = page.getFormByName("config");
        HtmlCheckBoxInput itemSpecific = form.getInputByName("itemSpecificSecurity");
        assertThat(itemSpecific, notNullValue());
        if (itemSpecific.isChecked()) {
            itemSpecific.setChecked(false);
        }
        r.submit(form);

        assertFalse(action().hasItemSpecificSecurity());
        assertThat(FolderOwnershipHelper.getOwnerProperty(folder).getItemSpecificSecurity(), nullValue());
    }

    @Test
    void getItemSpecificSecurity_fallsBackToGlobalDefaults() throws Exception {
        FolderSpecificSecurity defaults = matrixWith(Item.READ, "alice");
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withDefaultFoldersSecurity(defaults)
                .configure();

        assertFalse(action().hasItemSpecificSecurity());
        FolderSpecificSecurity resolved = action().getItemSpecificSecurity();
        assertThat(resolved, sameInstance(defaults));
        assertTrue(resolved.getPermissionsMatrix().getGrantedPermissionEntries().get(Item.READ)
                .stream().anyMatch(e -> "alice".equals(e.getSid())));
    }

    @Test
    void getItemSpecificSecurity_prefersFolderOverrideOverGlobal() throws Exception {
        FolderSpecificSecurity defaults = matrixWith(Item.READ, "alice");
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withDefaultFoldersSecurity(defaults)
                .configure();
        FolderSpecificSecurity local = matrixWith(Item.CONFIGURE, "bob");
        FolderOwnershipHelper.setProjectSpecificSecurity(folder, local);

        assertTrue(action().hasItemSpecificSecurity());
        assertThat(action().getItemSpecificSecurity(), sameInstance(local));
    }

    @Test
    void hasItemSpecificPermission_respectsMatrix() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(r).configure();
        FolderOwnershipHelper.setProjectSpecificSecurity(folder, matrixWith(Item.READ, "alice"));

        assertTrue(FolderOwnershipHelper.getInstance().hasItemSpecificPermission(folder, "alice", Item.READ));
        assertFalse(FolderOwnershipHelper.getInstance().hasItemSpecificPermission(folder, "bob", Item.READ));
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

    private FolderOwnershipAction action() {
        FolderOwnershipAction action = folder.getAction(FolderOwnershipAction.class);
        assertThat(action, notNullValue());
        return action;
    }

    private static FolderSpecificSecurity matrixWith(Permission permission, String sid) {
        Map<Permission, Set<String>> permissions = new HashMap<>();
        permissions.put(permission, Collections.singleton(sid));
        return new FolderSpecificSecurity(new AuthorizationMatrixProperty(permissions));
    }
}
