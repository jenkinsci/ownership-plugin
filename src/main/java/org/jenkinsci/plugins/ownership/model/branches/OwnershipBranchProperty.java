package org.jenkinsci.plugins.ownership.model.branches;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import hudson.Extension;
import hudson.model.*;
import hudson.util.FormValidation;
import jenkins.branch.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;

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
                    String owner = prospectiveOwner != null ? prospectiveOwner : fallbackOwner;
                    
                    OwnershipDescription ownershipDescription = new OwnershipDescription(true, owner, null);
                    try {
                        JobOwnerHelper.setOwnership(project, ownershipDescription);
                    } catch (IOException ioe) {
                        // TODO: handle somehow
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
            return "Set branch job ownership";
        }
        
        public FormValidation doCheckFallbackOwner(@QueryParameter String value) {
            User user = User.get(value, false, Collections.emptyMap());
            return user != null ? FormValidation.ok() : FormValidation.error("User '%s' is not registered in Jenkins", value);
        }
    }
}
