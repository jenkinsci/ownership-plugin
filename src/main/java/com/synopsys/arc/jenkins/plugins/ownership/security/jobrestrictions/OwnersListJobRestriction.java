/*
 * The MIT License
 *
 * Copyright 2013 Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
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
package com.synopsys.arc.jenkins.plugins.ownership.security.jobrestrictions;

import com.synopsys.arc.jenkins.plugins.ownership.Messages;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.util.ui.UserSelector;
import com.synopsys.arc.jenkinsci.plugins.jobrestrictions.restrictions.JobRestriction;
import com.synopsys.arc.jenkinsci.plugins.jobrestrictions.restrictions.JobRestrictionDescriptor;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Run;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 */
public class OwnersListJobRestriction extends JobRestriction {
    private static final JobOwnerHelper helper = new JobOwnerHelper();
    private List<UserSelector> usersList;
    private boolean acceptsCoOwners;
    transient private Set<String> usersMap;

    @DataBoundConstructor
    public OwnersListJobRestriction(List<UserSelector> usersList, boolean acceptsCoOwners) {
        this.usersList = usersList;
        this.acceptsCoOwners = acceptsCoOwners;
        updateUsersMap();
    }
    
    protected synchronized final void updateUsersMap() {
        if (usersMap == null) {
            // Update users map
            usersMap = new TreeSet<String>();
            for (UserSelector selector : usersList) {
                String userId = hudson.Util.fixEmptyAndTrim(selector.getSelectedUserId());
                if (userId != null && !usersMap.contains(userId)) {
                    usersMap.add(userId);
                }
            }
        }
    }

    public List<UserSelector> getUsersList() {
        return usersList;
    }

    public boolean isAcceptsCoOwners() {
        return acceptsCoOwners;
    }
    
    @Override
    public boolean canTake(Queue.BuildableItem item) {
        if (item.task instanceof Job) {
            Job job = (Job)item.task;
            OwnershipDescription descr = helper.getOwnershipDescription(job);
            return canTake(descr);
        }
        
        // Plugin covers only jobs
        return true;
    }

    @Override
    public boolean canTake(Run run) {
        OwnershipDescription descr = helper.getOwnershipDescription(run.getParent());
        return canTake(descr);
    }
    
    private boolean canTake(OwnershipDescription descr) {
        if (!descr.isOwnershipEnabled()) {
            return false;
        }
        
        updateUsersMap();
        if (usersMap.contains(descr.getPrimaryOwnerId())) {
            return true;
        }
        
        // Handle co-owners if required
        Set<String> itemCoOwners = descr.getCoownersIds();
        if (acceptsCoOwners && itemCoOwners!=null && !itemCoOwners.isEmpty()) {
            for (String userID : usersMap) {
                if (itemCoOwners.contains(userID)) {
                    return true;
                }
            }
        }
        
        // Default fallback - user is not owner or co-owner
        return false;
    }


    @Extension
    public static class DescriptorImpl extends JobRestrictionDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.Security_JobRestrictions_OwnershipRestriction_DisplayName();
        }       
    }
}
