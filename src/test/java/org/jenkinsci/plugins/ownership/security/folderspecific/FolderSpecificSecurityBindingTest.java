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
package org.jenkinsci.plugins.ownership.security.folderspecific;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import hudson.model.Item;
import java.util.List;
import java.util.Set;
import jenkins.model.Jenkins;
import org.htmlunit.html.HtmlCheckBoxInput;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTableRow;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import org.jenkinsci.plugins.ownership.model.folders.FolderOwnershipHelper;
import org.jenkinsci.plugins.ownership.model.folders.FolderOwnershipProperty;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Verifies FolderSpecificSecurity form binding goes through matrix-auth's
 * StaplerRequest2 / createNewInstance path (data format), not bindJSON(entries).
 */
@WithJenkins
class FolderSpecificSecurityBindingTest {

    private JenkinsRule r;

    private Folder folder;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        r = rule;
        OwnershipPluginConfigurer.forJenkinsRule(r).configure();

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
    void projectSpecificSecuritySubmit_bindsMatrixViaStaplerRequest2() throws Exception {
        JenkinsRule.WebClient wc = r.createWebClient();
        wc.login("admin", "admin");

        HtmlPage page = wc.goTo(folder.getUrl() + "ownership/configure-project-specifics");
        HtmlForm form = page.getFormByName("config");
        assertThat(form.getActionAttribute(), containsString("projectSpecificSecuritySubmit"));

        enableOptionalBlock(form, "itemSpecificSecurity");
        enableOptionalBlock(form, "useProjectSecurity");
        grantAnonymousRead(form);

        r.submit(form);

        FolderOwnershipProperty prop = FolderOwnershipHelper.getOwnerProperty(folder);
        assertThat(prop, notNullValue());
        assertTrue(prop.hasItemSpecificSecurity());

        AuthorizationMatrixProperty matrix = prop.getItemSpecificSecurity().getPermissionsMatrix();
        assertThat(matrix, notNullValue());
        Set<PermissionEntry> entries = matrix.getGrantedPermissionEntries().get(Item.READ);
        assertThat(entries, notNullValue());
        assertThat(entries, hasItem(PermissionEntry.user("anonymous")));
    }

    @Test
    void globalConfigure_bindsDefaultFoldersSecurityViaStaplerRequest2() throws Exception {
        JenkinsRule.WebClient wc = r.createWebClient();
        wc.login("admin", "admin");

        HtmlPage page = wc.goTo("configure");
        HtmlForm form = page.getFormByName("config");

        enableOptionalBlock(form, "defaultFoldersSecurity");
        enableLastMatchingCheckbox(form, "useProjectSecurity");
        grantAnonymousReadOnLastRow(form);

        r.submit(form);

        FolderSpecificSecurity defaults = OwnershipPlugin.getInstance().getDefaultFoldersSecurity();
        assertThat(defaults, notNullValue());
        AuthorizationMatrixProperty matrix = defaults.getPermissionsMatrix();
        assertThat(matrix, notNullValue());
        Set<PermissionEntry> entries = matrix.getGrantedPermissionEntries().get(Item.READ);
        assertThat(entries, notNullValue());
        assertThat(entries, hasItem(PermissionEntry.user("anonymous")));
    }

    @SuppressWarnings("unchecked")
    private static void enableLastMatchingCheckbox(HtmlForm form, String name) {
        List<HtmlCheckBoxInput> boxes = form.getByXPath(".//input[@type='checkbox' and @name='" + name + "']");
        if (boxes.isEmpty()) {
            return;
        }
        HtmlCheckBoxInput checkbox = boxes.get(boxes.size() - 1);
        if (!checkbox.isChecked()) {
            checkbox.setChecked(true);
        }
    }

    @SuppressWarnings("unchecked")
    private static void grantAnonymousReadOnLastRow(HtmlForm form) {
        List<HtmlTableRow> rows = form.getByXPath(".//tr[@name='[USER:anonymous]']");
        assertThat("anonymous permission rows", rows, notNullValue());
        assertThat("expected matrix rows on configure page", rows.isEmpty(), is(false));
        HtmlTableRow row = rows.get(rows.size() - 1);
        HtmlCheckBoxInput read =
                row.getFirstByXPath(".//input[@type='checkbox' and @name='[" + Item.READ.getId() + "]']");
        assertThat("Item.Read checkbox for anonymous", read, notNullValue());
        if (!read.isChecked()) {
            read.setChecked(true);
        }
    }

    private static void enableOptionalBlock(HtmlForm form, String name) {
        HtmlCheckBoxInput checkbox = form.getInputByName(name);
        assertThat("optionalBlock checkbox " + name, checkbox, notNullValue());
        if (!checkbox.isChecked()) {
            checkbox.setChecked(true);
        }
    }

    private static void grantAnonymousRead(HtmlForm form) {
        HtmlTableRow row = form.getFirstByXPath(".//tr[@name='[USER:anonymous]']");
        assertThat("anonymous permission row", row, notNullValue());
        HtmlCheckBoxInput read =
                row.getFirstByXPath(".//input[@type='checkbox' and @name='[" + Item.READ.getId() + "]']");
        assertThat("Item.Read checkbox for anonymous", read, notNullValue());
        if (!read.isChecked()) {
            read.setChecked(true);
        }
    }
}
