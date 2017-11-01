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

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkins.branch.Branch;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public abstract class BranchOwnershipStrategy extends AbstractDescribableImpl<BranchOwnershipStrategy> implements ExtensionPoint {
    
    /**
     * Determine the owner for the given branch using the implemented strategy.
     *
     * @param branch The branch
     * @return The prospective owner's user ID or full name. {@code null} if the owner cannot be determined.
     */
    @CheckForNull
    public abstract String determineOwner(Branch branch);
    
    @Nonnull
    @SuppressWarnings("unchecked")
    public BranchOwnershipStrategyDescriptor getDescriptor() {
        return (BranchOwnershipStrategyDescriptor) super.getDescriptor();
    }
    
    static abstract class BranchOwnershipStrategyDescriptor extends Descriptor<BranchOwnershipStrategy> {
        
    }
}
