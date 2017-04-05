/*
 * The MIT License
 *
 * Copyright 2017 Ksenia Nenasheva <ks.nenasheva@gmail.com>.
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
package org.jenkinsci.plugins.ownership.integrations.securityinspector;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.JobProperty;
import hudson.model.TopLevelItem;
import hudson.model.User;
import hudson.security.AuthorizationMatrixProperty;
import hudson.security.Permission;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jenkins.model.Jenkins;
import static org.hamcrest.MatcherAssert.assertThat;
import org.jenkinsci.plugins.ownership.model.folders.FolderOwnershipHelper;
import static org.junit.Assert.assertNotNull;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 *
 * @author Ksenia Nenasheva <ks.nenasheva@gmail.com>
 */
public class PermissionsForOwnerReportBuilderTest extends PermissionsForOwnerReportBuilder {
    
    @Rule
    public final JenkinsRule j = new JenkinsRule();
    
    protected void initializeDefaultMatrixAuthSecurity() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        
        // Create users
        User.get("admin");
        User.get("user1");
        User.get("user2");
        User user = User.get("user3");
        j.jenkins.save();
        user.save();
        
        // Create items (jobs & folder)
        FreeStyleProject project1 = j.createFreeStyleProject("project1");
        FreeStyleProject project2 = j.createFreeStyleProject("project2");
        final Folder f = j.createProject(Folder.class, "folder");
        FreeStyleProject projectInFolder = f.createProject(FreeStyleProject.class, "projectInFolder");
                
        // Initialize global security
        final ProjectMatrixAuthorizationStrategy strategy = new ProjectMatrixAuthorizationStrategy();
        strategy.add(Jenkins.ADMINISTER, "admin");
        strategy.add(Jenkins.READ, "user1");
        strategy.add(Jenkins.READ, "user2");
        strategy.add(Jenkins.READ, "user3");
        strategy.add(Item.READ, "user1");
        strategy.add(Item.READ, "user2");
        strategy.add(Item.READ, "user3");
        strategy.add(Computer.BUILD, "user1");
        strategy.add(Computer.CONFIGURE, "user1");
        strategy.add(Computer.CONNECT, "user2");
        strategy.add(Computer.CREATE, "user2");
        j.jenkins.setAuthorizationStrategy(strategy);
        
        JobOwnerHelper.Instance.setOwnership(project1, new OwnershipDescription(true, "user1", Arrays.asList("admin")));
        JobOwnerHelper.Instance.setOwnership(project2, new OwnershipDescription(true, "user1", Arrays.asList("user2", "admin")));
        JobOwnerHelper.Instance.setOwnership(projectInFolder, new OwnershipDescription(true, "user2", Arrays.asList("admin")));
        FolderOwnershipHelper.getInstance().setOwnership(f, new OwnershipDescription(true, "user2", Arrays.asList("admin", "user1")));
        
        j.jenkins.save();
        
        // Setup local security for project 1
        {
            final Set<String> user1 = Collections.singleton("user1");
            final Map<Permission, Set<String>> permissions = new HashMap<>();
            permissions.put(Item.BUILD, user1);
            permissions.put(Item.CONFIGURE, user1);
            final JobProperty prop = new AuthorizationMatrixProperty(permissions);
            project1.addProperty(prop);
        }
        
        // Setup local security for project 2
        {
            final Set<String> user2 = Collections.singleton("user2");
            final Map<Permission, Set<String>> permissions = new HashMap<>();
            permissions.put(Item.BUILD, user2);
            permissions.put(Item.DELETE, user2);
            final JobProperty prop = new AuthorizationMatrixProperty(permissions);
            project2.addProperty(prop);
        }
        
        // Setup local security for projectInFolder
        {
            final Set<String> user3 = Collections.singleton("user3");
            final Map<Permission, Set<String>> permissions = new HashMap<>();
            permissions.put(Item.BUILD, user3);
            permissions.put(Item.CANCEL, user3);
            permissions.put(Item.CONFIGURE, user3);
            permissions.put(Item.CREATE, user3);
            permissions.put(Item.DELETE, user3);
            final JobProperty prop = new AuthorizationMatrixProperty(permissions);
            projectInFolder.addProperty(prop);
        }
    }
    
    @Test
    public void shouldReportAdminProperly() throws Exception {
        
        initializeDefaultMatrixAuthSecurity();
        User usr = j.jenkins.getUser("admin");
        final PermissionsForOwnerReportBuilder.ReportImpl report = 
                new PermissionsForOwnerReportBuilder.ReportImpl(usr);
        assertNotNull(report);

        final OwnerFilter filter = new OwnerFilter();
        final Set<TopLevelItem> items4Report =new HashSet<>(filter.doFilter(usr));
        assertNotNull(items4Report);
        report.generateReport(items4Report);
        
        // Items:
        TopLevelItem project1 = j.jenkins.getItem("project1");
        TopLevelItem project2 = j.jenkins.getItem("project2");
        TopLevelItem folder = j.jenkins.getItem("folder");
        TopLevelItem projectInFolder = (TopLevelItem) j.jenkins.getItemByFullName("folder/projectInFolder", TopLevelItem.class);
        
        PermissionReportAssert.assertHasRow(report, project1);
        PermissionReportAssert.assertHasRow(report, project2);
        PermissionReportAssert.assertHasRow(report, folder);
        PermissionReportAssert.assertHasRow(report, projectInFolder);
        
        PermissionReportAssert.assertHasPermissions(report, project1, 
                Item.BUILD, Item.CANCEL, Item.CONFIGURE, Item.CREATE, Item.DELETE, 
                Item.DISCOVER, Item.READ, Item.WORKSPACE);
        
        PermissionReportAssert.assertHasPermissions(report, project2, 
                Item.BUILD, Item.CANCEL, Item.CONFIGURE, Item.CREATE, Item.DELETE, 
                Item.DISCOVER, Item.READ, Item.WORKSPACE);
        
        PermissionReportAssert.assertHasPermissions(report, folder, 
                Item.BUILD, Item.CANCEL, Item.CONFIGURE, Item.CREATE, Item.DELETE, 
                Item.DISCOVER, Item.READ, Item.WORKSPACE);
        
        PermissionReportAssert.assertHasPermissions(report, projectInFolder, 
                Item.BUILD, Item.CANCEL, Item.CONFIGURE, Item.CREATE, Item.DELETE, 
                Item.DISCOVER, Item.READ, Item.WORKSPACE);
    }
    
    @Test
    public void shouldReportUser1Properly() throws Exception {
        
        initializeDefaultMatrixAuthSecurity();
        
        User usr = User.get("user1");
        final PermissionsForOwnerReportBuilder.ReportImpl report = 
                new PermissionsForOwnerReportBuilder.ReportImpl(usr);
        assertNotNull(report);
        
        final OwnerFilter filter = new OwnerFilter();
        final Set<TopLevelItem> items4Report =new HashSet<>(filter.doFilter(usr));
        assertNotNull(items4Report);
        report.generateReport(items4Report);
        
        // Items:
        TopLevelItem project1 = j.jenkins.getItem("project1");
        TopLevelItem project2 = j.jenkins.getItem("project2");
        TopLevelItem folder = j.jenkins.getItem("folder");
        TopLevelItem projectInFolder = (TopLevelItem) j.jenkins.getItemByFullName("folder/projectInFolder", TopLevelItem.class);
       
        PermissionReportAssert.assertHasRow(report, project1);
        PermissionReportAssert.assertHasRow(report, project2);
        PermissionReportAssert.assertHasRow(report, j.jenkins.getItem("folder"));
        PermissionReportAssert.assertHasNotRow(report, projectInFolder);

        PermissionReportAssert.assertHasPermissions(report, project1, 
                Item.READ, Item.CONFIGURE, Item.BUILD, Item.CANCEL, Item.DISCOVER);
        PermissionReportAssert.assertHasNotPermissions(report, project1, 
                Item.CREATE, Item.DELETE, Item.WORKSPACE);
        
        PermissionReportAssert.assertHasPermissions(report, project2, 
                Item.READ, Item.DISCOVER);
        PermissionReportAssert.assertHasNotPermissions(report, project2, 
                Item.CONFIGURE, Item.CREATE, Item.DELETE, Item.BUILD, Item.CANCEL, Item.WORKSPACE);
        
        PermissionReportAssert.assertHasPermissions(report, folder, 
                Item.READ, Item.DISCOVER);
        PermissionReportAssert.assertHasNotPermissions(report, folder, 
                Item.CONFIGURE, Item.CREATE, Item.DELETE, Item.BUILD, Item.CANCEL, Item.WORKSPACE);
    }
    
    @Test
    public void shouldReportUser2Properly() throws Exception {
        
        initializeDefaultMatrixAuthSecurity();
        User usr = User.get("user2");
        final PermissionsForOwnerReportBuilder.ReportImpl report = 
                new PermissionsForOwnerReportBuilder.ReportImpl(usr);
        assertNotNull(report);
        
        final OwnerFilter filter = new OwnerFilter();
        final Set<TopLevelItem> items4Report =new HashSet<>(filter.doFilter(usr));
        assertNotNull(items4Report);
        report.generateReport(items4Report);
        
        // Items:
        TopLevelItem project1 = j.jenkins.getItem("project1");
        TopLevelItem project2 = j.jenkins.getItem("project2");
        TopLevelItem folder = j.jenkins.getItem("folder");
        TopLevelItem projectInFolder = (TopLevelItem) j.jenkins.getItemByFullName("folder/projectInFolder", TopLevelItem.class);
        
        PermissionReportAssert.assertHasNotRow(report, project1);
        PermissionReportAssert.assertHasRow(report, project2);
        PermissionReportAssert.assertHasRow(report, folder);
        PermissionReportAssert.assertHasRow(report, projectInFolder);
        
        PermissionReportAssert.assertHasPermissions(report, project2, 
                Item.READ, Item.DELETE, Item.BUILD, Item.CANCEL, Item.DISCOVER);
        PermissionReportAssert.assertHasNotPermissions(report, project2, 
                Item.CREATE, Item.CONFIGURE, Item.WORKSPACE);
        
        PermissionReportAssert.assertHasPermissions(report, folder, 
                Item.READ, Item.DISCOVER);
        PermissionReportAssert.assertHasNotPermissions(report, folder, 
                Item.CONFIGURE, Item.CREATE, Item.DELETE, Item.BUILD, Item.CANCEL, Item.WORKSPACE);
        
        PermissionReportAssert.assertHasPermissions(report, projectInFolder, 
                Item.READ, Item.DISCOVER);
        PermissionReportAssert.assertHasNotPermissions(report, projectInFolder, 
                Item.CONFIGURE, Item.CREATE, Item.DELETE, Item.BUILD, Item.CANCEL, Item.WORKSPACE);
    }
    
    @Test
    public void shouldReportUser3Properly() throws Exception {
        
        initializeDefaultMatrixAuthSecurity();
        User usr = User.get("user3");
        final PermissionsForOwnerReportBuilder.ReportImpl report = 
                new PermissionsForOwnerReportBuilder.ReportImpl(usr);
        assertNotNull(report);
        
        final OwnerFilter filter = new OwnerFilter();
        final Set<TopLevelItem> items4Report =new HashSet<>(filter.doFilter(usr));
        assertNotNull(items4Report);
        report.generateReport(items4Report);
        
        // Items:
        TopLevelItem project1 = j.jenkins.getItem("project1");
        TopLevelItem project2 = j.jenkins.getItem("project2");
        TopLevelItem folder = j.jenkins.getItem("folder");
        TopLevelItem projectInFolder = (TopLevelItem) j.jenkins.getItemByFullName("folder/projectInFolder", TopLevelItem.class);
        
        PermissionReportAssert.assertHasNotRow(report, project1);
        PermissionReportAssert.assertHasNotRow(report, project2);
        PermissionReportAssert.assertHasNotRow(report, folder);
        PermissionReportAssert.assertHasNotRow(report, projectInFolder);
    }
    
    @Test
    public void shouldDownloadReport4User1() throws Exception {
        
        initializeDefaultMatrixAuthSecurity();
        
        User usr = User.get("user1");
        final PermissionsForOwnerReportBuilder.ReportImpl report = 
                new PermissionsForOwnerReportBuilder.ReportImpl(usr);
        assertNotNull(report);
        
        final OwnerFilter filter = new OwnerFilter();
        final Set<TopLevelItem> items4Report =new HashSet<>(filter.doFilter(usr));
        assertNotNull(items4Report);
        report.generateReport(items4Report);
        
        assertThat("Report target name must be equal to 'user1'", report.getReportTargetName().equals("user1")); 

        String reportInCSV = report.getReportInCSV();
        assertNotNull(reportInCSV);
        
        for (TopLevelItem item : items4Report) {
            assertThat("CSV Report must had row " + item, reportInCSV.contains(item.getFullDisplayName()));
        }
    }
}
