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
package com.synopsys.arc.jenkins.plugins.ownership.jobs;

import com.synopsys.arc.jenkins.plugins.ownership.Messages;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
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
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Filters owner's and co-owners.
 * @author Oleg Nenashev <nenashev@synopsys.com>
 */
public class OwnershipJobFilter extends ViewJobFilter {
    /**
     * Macro, which allows to select currently logged user for "My owned jobs" filter
     */
    private static final String MACRO_ME = "@Me";
    
    String ownerId;
    boolean acceptsCoowners;
    
    public String getOwnerName() {
        return ownerId;
    }
    
    public boolean isAcceptsCoowners() {
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
        ArrayList<TopLevelItem> newList = new ArrayList<TopLevelItem>();     
        UserWrapper wuserWrapper = new UserWrapper(ownerId);
            
        for(TopLevelItem item : added) {
            // Convert to project
            AbstractProject project = (AbstractProject)item;
            JobOwnerJobProperty prop = JobOwnerHelper.getOwnerProperty(project);
                  
            if (prop != null) {
                boolean matches = false;
                OwnershipDescription ownership = prop.getOwnership();    
                if (!ownership.isOwnershipEnabled()) {
                    continue;
                }
                
                if (wuserWrapper.meetsMacro(ownership.getPrimaryOwnerId())) {
                    matches = true;
                }            
                if (acceptsCoowners && !matches) {
                    for (String coOwnerId : ownership.getCoownersIds()) {
                        if (wuserWrapper.meetsMacro(coOwnerId)) {
                            matches = true;
                            break;
                        }
                    }
                }
                
                if (matches) {
                    newList.add(item);
                }
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
                String jobOwner = formData.getString( "jobOwner" );
                boolean acceptCoowners = formData.getBoolean("acceptsCoowners" );
                OwnershipJobFilter instance = new OwnershipJobFilter( jobOwner, acceptCoowners );
                return instance;
        } 
    }
    
    /**
    * Get list of users for the selector.
    * @return Collection of all registered users 
    */
    public static Collection<UserWrapper> getAvailableUsers() {                    
        // Sort users
        UserComparator comparator = new UserComparator();
        LinkedList<User> userList = new LinkedList<User>(User.getAll());                     
        Collections.sort(userList, comparator);

        // Prepare new list
        Collection<UserWrapper> res = new ArrayList<UserWrapper>(userList.size()+1);
        res.add(new UserWrapper(MACRO_ME));
        for (User user : userList) {
            res.add(new UserWrapper(user));
        }
        return res;
    } 
}

