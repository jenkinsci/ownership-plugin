/*
 * The MIT License
 *
 * Copyright (c) 2026 Ownership Plugin Contributors.
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
package org.jenkinsci.plugins.ownership.ui;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.User;
import java.util.Collections;
import net.sf.json.JSONObject;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlDivision;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.jenkinsci.plugins.ownership.config.DisplayOptions;
import org.jenkinsci.plugins.ownership.model.folders.FolderOwnershipHelper;
import org.jenkinsci.plugins.ownership.model.runs.RunOwnershipHelper;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * UI integration and flow tests for Design Library migration.
 */
public class OwnershipUiDesignLibraryTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(j).configure();
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        User.getById("alice", true);
        User.getById("bob", true);
    }

    @Test
    public void jobHubUsesDesignLibraryCards() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("hub-job");
        HtmlPage page = j.createWebClient().goTo(project.getUrl() + "ownership/");

        HtmlElement hub = page.getFirstByXPath("//*[@data-testid='ownership-hub']");
        assertThat("Ownership hub container should be present", hub, notNullValue());
        assertThat(page.getFirstByXPath("//div[contains(@class,'jenkins-card')]"), notNullValue());
        assertThat(page.getAnchorByHref("manage-owners"), notNullValue());
        assertThat(page.getAnchorByHref("configure-project-specifics"), notNullValue());
        assertThat(page.asNormalizedText(), containsString(project.getAction(
                com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerJobAction.class).getDisplayName()));
    }

    @Test
    public void manageOwnersPageShowsWarningAlertAndForm() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("manage-job");
        HtmlPage page = j.createWebClient().goTo(project.getUrl() + "ownership/manage-owners");

        assertThat(page.getFirstByXPath("//*[@data-testid='ownership-manage-owners']"), notNullValue());
        assertThat(page.getFirstByXPath("//div[contains(@class,'jenkins-alert-warning')]"), notNullValue());
        assertThat(page.getFirstByXPath("//div[contains(@class,'jenkins-card')]"), notNullValue());

        HtmlForm form = page.getFormByName("config");
        assertThat(form, notNullValue());
        assertThat(form.getActionAttribute(), containsString("ownersSubmit"));
    }

    @Test
    public void jobOwnershipFlowUpdatesSummaryCard() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("flow-job");
        JenkinsRule.WebClient wc = j.createWebClient();

        HtmlPage managePage = wc.goTo(project.getUrl() + "ownership/manage-owners");
        assertThat(managePage.getFirstByXPath("//div[contains(@class,'jenkins-alert-warning')]"), notNullValue());

        submitOwners(wc, project.getUrl(), "alice", "bob");

        assertThat(JobOwnerHelper.Instance.getOwnershipDescription(project).getPrimaryOwnerId(), is("alice"));

        HtmlPage jobPage = wc.goTo(project.getUrl());
        HtmlDivision summary = jobPage.getFirstByXPath("//*[@data-testid='ownership-summary']");
        assertThat(summary, notNullValue());
        assertThat(summary.getTextContent(), containsString("alice"));

        HtmlPage hub = wc.goTo(project.getUrl() + "ownership/");
        assertThat(hub.getFirstByXPath("//*[@data-testid='ownership-hub']"), notNullValue());
        assertThat(hub.getAnchorByHref("manage-owners"), notNullValue());
    }

    @Test
    public void folderOwnershipFlowShowsSummaryCard() throws Exception {
        Folder folder = j.jenkins.createProject(Folder.class, "owned-folder");
        FolderOwnershipHelper.setOwnership(folder, new OwnershipDescription(true, "alice", Collections.singletonList("bob")));

        HtmlPage page = j.createWebClient().goTo(folder.getUrl());
        HtmlDivision summary = page.getFirstByXPath("//*[@data-testid='ownership-summary']");
        assertThat(summary, notNullValue());
        assertThat(summary.getTextContent(), containsString("alice"));

        HtmlPage hub = j.createWebClient().goTo(folder.getUrl() + "ownership/");
        assertThat(hub.getFirstByXPath("//*[@data-testid='ownership-hub']"), notNullValue());
        assertThat(hub.getAnchorByHref("manage-owners"), notNullValue());
    }

    @Test
    public void nodeOwnershipHubAndSummarySmoke() throws Exception {
        Node slave = j.createSlave("agent-1", null, null);
        com.synopsys.arc.jenkins.plugins.ownership.nodes.NodeOwnerHelper.setOwnership(
                slave, new OwnershipDescription(true, "alice", Collections.emptyList()));

        String computerUrl = slave.toComputer().getUrl();
        HtmlPage hub = j.createWebClient().goTo(computerUrl + "ownership/");
        assertThat(hub.getFirstByXPath("//*[@data-testid='ownership-hub']"), notNullValue());
        assertThat(hub.getAnchorByHref("manage-owners"), notNullValue());

        HtmlPage manage = j.createWebClient().goTo(computerUrl + "ownership/manage-owners");
        assertThat(manage.getFirstByXPath("//*[@data-testid='ownership-manage-owners']"), notNullValue());
        assertThat(manage.getFirstByXPath("//div[contains(@class,'jenkins-alert-warning')]"), notNullValue());
    }

    @Test
    public void runSummaryUsesDesignLibraryCard() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("run-card");
        JobOwnerHelper.setOwnership(project, new OwnershipDescription(true, "alice", Collections.emptyList()));
        FreeStyleBuild build = j.buildAndAssertSuccess(project);

        HtmlPage page = j.createWebClient().goTo(build.getUrl());
        HtmlDivision summary = page.getFirstByXPath("//*[@data-testid='ownership-summary']");
        assertThat(summary, notNullValue());
        assertThat(summary.getFirstByXPath(".//div[contains(@class,'jenkins-card')]"), notNullValue());
        assertThat(page.getFirstByXPath("//*[@data-testid='ownership-user-info']"), notNullValue());
        assertThat(summary.getTextContent(), containsString("alice"));
    }

    @Test
    public void hideOptionsRemovesRunSummaryCard() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(j)
                .withDisplayOptions(new DisplayOptions(true, false))
                .configure();

        FreeStyleProject project = j.createFreeStyleProject("hidden-run");
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        assertThat(RunOwnershipHelper.getInstance().isDisplayOwnershipSummaryBox(build), is(false));

        HtmlPage page = j.createWebClient().goTo(build.getUrl());
        assertThat(page.getFirstByXPath("//*[@data-testid='ownership-summary']"), nullValue());
    }

    @Test
    public void globalConfigSectionRenders() throws Exception {
        HtmlPage page = j.createWebClient().goTo("manage/configure");
        assertThat(page.getFirstByXPath("//*[@data-testid='ownership-global-config']"), notNullValue());
        assertThat(page.getFirstByXPath("//div[contains(@class,'jenkins-card')]"), notNullValue());
    }

    private static void submitOwners(JenkinsRule.WebClient wc, String itemUrl, String primary, String secondary) throws Exception {
        JSONObject ownersJson = new JSONObject();
        ownersJson.put("primaryOwner", primary);
        if (secondary != null) {
            JSONObject coOwner = new JSONObject();
            coOwner.put("coOwner", secondary);
            ownersJson.put("coOwners", Collections.singletonList(coOwner));
        }
        JSONObject formData = new JSONObject();
        formData.put("owners", ownersJson);

        WebRequest req = new WebRequest(wc.createCrumbedUrl(itemUrl + "ownership/ownersSubmit"), HttpMethod.POST);
        req.setAdditionalHeader("Content-Type", "application/x-www-form-urlencoded");
        req.setRequestBody("json=" + formData);
        wc.getPage(req);
    }
}
