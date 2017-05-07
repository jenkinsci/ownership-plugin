package org.jenkinsci.plugins.ownership.model.branches;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkins.branch.Branch;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BranchOwnershipStrategy extends AbstractDescribableImpl<BranchOwnershipStrategy> implements ExtensionPoint {
    
    /**
     * Determine the owner for the given branch using the implemented strategy.
     *
     * @param branch The branch
     * @return The prospective owner's user ID or full name. {@code null} if the owner cannot be determined.
     */
    @Nullable
    public abstract String determineOwner(Branch branch);
    
    @Nonnull
    @SuppressWarnings("unchecked")
    public BranchOwnershipStrategyDescriptor getDescriptor() {
        return (BranchOwnershipStrategyDescriptor) super.getDescriptor();
    }
    
    static abstract class BranchOwnershipStrategyDescriptor extends Descriptor<BranchOwnershipStrategy> {
        
    }
}
