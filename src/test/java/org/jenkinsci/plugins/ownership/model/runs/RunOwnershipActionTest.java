/*
 * The MIT License
 *
 * Copyright (c) 2015 Oleg Nenashev.
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
package org.jenkinsci.plugins.ownership.model.runs;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.User;
import java.util.Arrays;
import java.util.Collections;
import org.jenkinsci.plugins.ownership.config.DisplayOptions;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;
import org.jenkinsci.plugins.ownership.model.folders.FolderOwnershipHelper;
import org.jenkinsci.plugins.ownership.model.OwnershipInfo;
import static org.junit.Assert.assertThat;
import org.jvnet.hudson.test.Issue;

/**
 * Tests for {@link RunOwnershipAction}.
 * @author Oleg Nenashev
 */
public class RunOwnershipActionTest {
    
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    
    @Test
    @Issue("JENKINS-28881")
    public void shouldInheritOwnershipInfoFromFolders() throws Exception {
        Folder folder = jenkinsRule.jenkins.createProject(Folder.class, "folder");
        FreeStyleProject project = folder.createProject(FreeStyleProject.class, "projectInFolder");
        
        // Set ownership via API
        OwnershipDescription original = new OwnershipDescription(true, "ownerId", 
            Arrays.asList("coowner1, coowner2"));
        FolderOwnershipHelper.setOwnership(folder, original);
        
        // Run project
        FreeStyleBuild build = jenkinsRule.buildAndAssertSuccess(project);
        
        OwnershipInfo ownershipInfo = RunOwnershipHelper.getInstance().getOwnershipInfo(build);
        assertThat("Folder ownership helper should return the inherited value after the reload",
                ownershipInfo.getDescription(), equalTo(original));
        assertThat("OwnershipInfo should return the right reference", 
                ownershipInfo.getSource().getItem(), equalTo((Object)jenkinsRule.jenkins.getItemByFullName("folder")));
    }
    
    @Test
    public void shouldDisplayStubSummaryBoxIfNoOwnership() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        FreeStyleBuild build = jenkinsRule.buildAndAssertSuccess(project);
        
        assertThat("Run Ownership Box should be enabled in configs", 
                RunOwnershipHelper.getInstance().isDisplayOwnershipSummaryBox(build), is(true));
        
        // Check the Ownership summary box for the Run
        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();
        HtmlPage res = webClient.goTo(build.getUrl());
        HtmlDivision summaryBox = res.<HtmlDivision>getFirstByXPath("//div[@class='ownership-summary-box']");
        assertThat("On the page there should an ownership box", summaryBox, notNullValue());
        assertThat("Ownership box should contain no info about the owner", summaryBox.getTextContent(), 
                stringContainsInOrder(Arrays.asList("Ownership is not configured for this Run")));
    }
    
    @Test
    public void shouldDisplayRunOwnershipByDefault() throws Exception {
        
        jenkinsRule.jenkins.setSecurityRealm(jenkinsRule.createDummySecurityRealm());
        User user = User.get("testUser");
        
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        JobOwnerHelper.setOwnership(project, new OwnershipDescription(true, user.getId(), Collections.<String>emptyList()));
        FreeStyleBuild build = jenkinsRule.buildAndAssertSuccess(project);
        
        assertThat("Run Ownership Box should be enabled in configs", 
                RunOwnershipHelper.getInstance().isDisplayOwnershipSummaryBox(build), is(true));
        
        // Check the Ownership summary box for the Run
        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();
        HtmlPage res = webClient.goTo(build.getUrl());
        HtmlDivision summaryBox = res.<HtmlDivision>getFirstByXPath("//div[@class='ownership-summary-box']");
        assertThat("On the page there should an ownership box", summaryBox, notNullValue());
        HtmlDivision ownerInfo = summaryBox.<HtmlDivision>getFirstByXPath("//div[@class='ownership-user-info']");
        assertThat("Ownership Summary Box should contain the owner info", summaryBox, notNullValue());
        assertThat("Owner info should mention user " + user, ownerInfo.getTextContent(), 
                stringContainsInOrder(Arrays.asList(user.getId())));
    }
    
    @Test
    @Issue("JENKINS-28714")
    public void shouldHideRunOwnershipIfRequested() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(jenkinsRule)
                .withDisplayOptions(new DisplayOptions(true, false))
                .configure();
        
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        FreeStyleBuild build = jenkinsRule.buildAndAssertSuccess(project);
        
        assertThat("Run Ownership Box should be disabled in configs", 
                RunOwnershipHelper.getInstance().isDisplayOwnershipSummaryBox(build), is(false));
          
        // Check the Ownership summary box for the Run
        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();
        HtmlPage res = webClient.goTo(build.getUrl());
        assertThat("On the page there should not be ownership box", 
                res.getFirstByXPath("//div[@class='ownership-summary-box']"), nullValue());
    }
    
    @Test
    @Issue("JENKINS-28712")
    public void shouldHideBoxesForNonConfiguredOwnershipIfConfigured() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(jenkinsRule)
                .withDisplayOptions(new DisplayOptions(false, true))
                .configure();
        
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        FreeStyleBuild build = jenkinsRule.buildAndAssertSuccess(project);
        
        assertThat("Run Ownership Box should be disabled in configs", 
                RunOwnershipHelper.getInstance().isDisplayOwnershipSummaryBox(build), is(false));
          
        // Check the Ownership summary box for the Run
        JenkinsRule.WebClient webClient = jenkinsRule.createWebClient();
        HtmlPage res = webClient.goTo(build.getUrl());
        assertThat("On the page there should not be ownership box", 
                res.getFirstByXPath("//div[@class='ownership-summary-box']"), nullValue());
    }
}
