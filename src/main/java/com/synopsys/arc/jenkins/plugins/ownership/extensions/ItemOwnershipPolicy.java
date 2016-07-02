/*
 * The MIT License
 *
 * Copyright 2014 Oleg Nenashev, Synopsys Inc.
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

package com.synopsys.arc.jenkins.plugins.ownership.extensions;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Item;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;

/**
 * Implements an ownership changes policy for {@link Item}s. 
 * This policy defines actions to be implemented if specific changes in jobs occur.
 * @author Oleg Nenashev
 */
public abstract class ItemOwnershipPolicy 
        implements ExtensionPoint, Describable<ItemOwnershipPolicy> {
    
    /**
     * A handler for newly created items.
     * By default, the ownership won't be set.
     * @param item A newly created item, for which the ownership should be set
     * @return An ownership description to be assigned. Return null to 
     * leave the ownership unassigned.
     */
    public @CheckForNull OwnershipDescription onCreated(@Nonnull Item item) {
        return null; // Do not assign ownership
    }
    
    /**
     * A handler for copied items.
     * By default, the {@link #onCreated(hudson.model.Item)} handler will be called;
     * @param src A source item
     * @param item A newly created item, for which the ownership should be set
     * @return An ownership description to be assigned. Return null to 
     * leave the ownership unassigned.
     */
    public @CheckForNull OwnershipDescription onCopied(@Nonnull Item src, @Nonnull Item item) {
        return onCreated(item);
    }
       
    @Override
    public ItemOwnershipPolicyDescriptor getDescriptor() {
        return (ItemOwnershipPolicyDescriptor) Jenkins.getActiveInstance()
                .getDescriptorOrDie(ItemOwnershipPolicy.class);
    }
    
    /**
     * Get list of all registered {@link ItemOwnershipPolicy}s.
     * @return List of {@link ItemOwnershipPolicy}s.
     */    
    public static DescriptorExtensionList<ItemOwnershipPolicy,ItemOwnershipPolicyDescriptor> all() {
        //TODO: rework to Extension list Lookup
        return Jenkins.getActiveInstance().<ItemOwnershipPolicy,ItemOwnershipPolicyDescriptor>
                getDescriptorList(ItemOwnershipPolicy.class);
    }
    
    /**
     * Returns list of JobRestriction descriptors.
     * @return List of available descriptors.
     * @since 0.2
     */
    public static List<ItemOwnershipPolicyDescriptor> allDescriptors() {
        return all().reverseView();
    }
}
