/*
 * The MIT License
 *
 * Copyright 2013 Oleg Nenashev, Synopsys Inc.
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
package com.synopsys.arc.jenkins.plugins.ownership.jobs;

import com.synopsys.arc.jenkins.plugins.ownership.Messages;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.util.AbstractOwnershipHelper;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.User;
import hudson.views.ListViewColumnDescriptor;
import hudson.views.ListViewColumn;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.ownership.model.OwnershipHelperLocator;

/**
 * Provides Ownership column for the list view.
 * @author Oleg Nenashev
 * @since 0.1
 */
public class JobOwnerColumn extends ListViewColumn {

    @DataBoundConstructor
    public JobOwnerColumn() {
        super();
    }

    @Nonnull
    public String getJobOwner(Item item) {
        final OwnershipDescription description = getDescription(item);
        return description != null ? description.getPrimaryOwnerId() : User.getUnknown().getId();
    }
    
    public boolean isOwnerExists(Item item) {
        final OwnershipDescription description = getDescription(item);
        return description != null ? description.hasPrimaryOwner(): false;
    }
    
    @CheckForNull
    private OwnershipDescription getDescription(Item item) {
        AbstractOwnershipHelper<Item> helper = OwnershipHelperLocator.locate(item);
        if (helper == null) {
            // We cannot retrieve helper for the object => keep moving
            return null;
        }
        return helper.getOwnershipDescription(item);
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {

        @Override
        public boolean shownByDefault() {
            return false;
        }

        @Override
        public String getDisplayName() {
            return Messages.JobOwnership_Column_Title();
        }
    }
}
