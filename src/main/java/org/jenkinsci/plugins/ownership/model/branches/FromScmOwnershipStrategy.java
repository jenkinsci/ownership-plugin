package org.jenkinsci.plugins.ownership.model.branches;


import hudson.Extension;
import jenkins.branch.Branch;
import jenkins.scm.api.metadata.ContributorMetadataAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FromScmOwnershipStrategy extends BranchOwnershipStrategy {
    
    
    @Nullable
    @Override
    public String determineOwner(Branch branch) {
        ContributorMetadataAction contributorMetadataAction = branch.getAction(ContributorMetadataAction.class);
        return contributorMetadataAction != null ? contributorMetadataAction.getContributor() : null;
    }
    
    @Extension
    public static class DescriptorImpl extends BranchOwnershipStrategy.BranchOwnershipStrategyDescriptor {
        @Override
        @Nonnull
        public String getDisplayName() {
            return "From scm";
        }
    }
}
