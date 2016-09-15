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
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.UserComparator;
import com.synopsys.arc.jenkins.plugins.ownership.util.UserWrapper;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.TopLevelItem;
import hudson.model.User;
import hudson.model.View;
import hudson.views.ViewJobFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.ownership.model.OwnershipHelperLocator;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Filters jobs by primary and secondary owners.
 * @author Oleg Nenashev
 * @since 0.1
 */
public class OwnershipJobFilter extends ViewJobFilter {

    /**
     * Macro, which allows to select currently logged user for the filter.
     */
    private static final String MACRO_ME = "@Me";

    String ownerId;
    boolean acceptsCoowners;

    public String getOwnerName() {
        return ownerId;
    }

    /**
     * @deprecated use {@link #isAcceptSecondaryOwners()}
     */
    @Deprecated
    public boolean isAcceptsCoowners() {
        return acceptsCoowners;
    }
    
    /**
     * Enables checking of secondary owners
     * @return 
     * @since TODO
     */
    public boolean isAcceptSecondaryOwners() {
        return acceptsCoowners;
    }

    public boolean isSelected(UserWrapper usr) {
        return ownerId.equals(usr.getId());
    }

    @DataBoundConstructor
    public OwnershipJobFilter(String ownerName, boolean acceptCoowners) {
        this.ownerId = ownerName;
        this.acceptsCoowners = acceptCoowners;
    }

    @Override
    public List<TopLevelItem> filter(List<TopLevelItem> added, List<TopLevelItem> all, View filteringView) {
        final ArrayList<TopLevelItem> newList = new ArrayList<TopLevelItem>();
        final UserWrapper userWrapper = new UserWrapper(ownerId);

        for (TopLevelItem item : added) {
            AbstractOwnershipHelper<TopLevelItem> helper = OwnershipHelperLocator.locate(item);
            if (helper == null) {
                // We cannot retrieve helper for the object => keep moving
                continue;
            }
            
            OwnershipDescription ownership = helper.getOwnershipDescription(item);
            if (!ownership.isOwnershipEnabled()) {
                continue;
            }

            boolean matches = false; // Check owner
            if (userWrapper.meetsMacro(ownership.getPrimaryOwnerId())) {
                matches = true;
            }
            if (acceptsCoowners && !matches) { // Check secondary owners
                for (String coOwnerId : ownership.getSecondaryOwnerIds()) {
                    if (userWrapper.meetsMacro(coOwnerId)) {
                        matches = true;
                        break;
                    }
                }
            }
            if (matches) {
                newList.add(item);
            }
        }

        return newList;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ViewJobFilter> {

        @Override
        public String getDisplayName() {
            return Messages.JobOwnership_Filter_DisplayName();
        }

        @Override
        public ViewJobFilter newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            String jobOwner = formData.getString("jobOwner");
            boolean acceptCoowners = formData.getBoolean("acceptsCoowners");
            OwnershipJobFilter instance = new OwnershipJobFilter(jobOwner, acceptCoowners);
            return instance;
        }
    }

    /**
     * Get list of users for the selector.
     *
     * @return Collection of all registered users
     */
    @Nonnull
    public static Collection<UserWrapper> getAvailableUsers() {
        // Sort users
        UserComparator comparator = new UserComparator();
        LinkedList<User> userList = new LinkedList<User>(User.getAll());
        Collections.sort(userList, comparator);

        // Prepare new list
        Collection<UserWrapper> res = new ArrayList<UserWrapper>(userList.size() + 1);
        res.add(new UserWrapper(MACRO_ME));
        for (User user : userList) {
            res.add(new UserWrapper(user));
        }
        return res;
    }
}
