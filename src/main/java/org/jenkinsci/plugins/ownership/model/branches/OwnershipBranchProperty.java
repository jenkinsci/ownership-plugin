/*
 * The MIT License
 *
 * Copyright (c) 2017 Jordan Coll
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
package org.jenkinsci.plugins.ownership.model.branches;

import com.synopsys.arc.jenkins.plugins.ownership.Messages;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.model.User;
import hudson.util.FormValidation;
import jenkins.branch.Branch;
import jenkins.branch.BranchProperty;
import jenkins.branch.BranchPropertyDescriptor;
import jenkins.branch.JobDecorator;
import jenkins.branch.MultiBranchProject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;

public class OwnershipBranchProperty extends BranchProperty {
    
    private BranchOwnershipStrategy strategy;
    private String fallbackOwner;
    
    @DataBoundConstructor
    public OwnershipBranchProperty(@Nonnull String fallbackOwner, @Nonnull BranchOwnershipStrategy strategy) {
        this.strategy = strategy;
        this.fallbackOwner = fallbackOwner;
    }
    
    public BranchOwnershipStrategy getStrategy() {
        return strategy;
    }
    
    public String getFallbackOwner() {
        return fallbackOwner;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <P extends Job<P, B>, B extends Run<P, B>> JobDecorator<P, B> jobDecorator(final Class<P> clazz) {
        return new JobDecorator<P, B>() {
            @Nonnull
            public P project(@Nonnull P project) {
                if (project.getParent() instanceof MultiBranchProject && TopLevelItem.class.isAssignableFrom(clazz)) {
                    MultiBranchProject multiBranchProject = (MultiBranchProject) project.getParent();
                    Branch branch = multiBranchProject.getProjectFactory().getBranch(project);
                    
                    String prospectiveOwner = strategy.determineOwner(branch);
                    String owner = prospectiveOwner != null ? prospectiveOwner : getFallbackOwner();
                    
                    OwnershipDescription ownershipDescription = new OwnershipDescription(true, owner, null);
                    try {
                        JobOwnerHelper.setOwnership(project, ownershipDescription);
                    } catch (IOException ioe) {
                        // TODO: handle somehow
                        String msg = String.format("Failed setting owner for branch %s in project %s",
                                branch.getName(), multiBranchProject.getFullName());
                        OwnershipPlugin.getLogger().log(Level.SEVERE, msg, ioe);
                    }
                }
                return project;
            }
        };
    }
    
    @Extension
    public static class DescriptorImpl extends BranchPropertyDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.BranchOwnership_BranchProperty_DisplayName();
        }
        
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckFallbackOwner(@QueryParameter String value) {
            User user = User.get(value, false, Collections.emptyMap());
            return user != null ? FormValidation.ok() : FormValidation.error(Messages.BranchOwnership_BranchProperty_UnknownUserError(), value);
        }
    }
}
