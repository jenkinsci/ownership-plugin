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

import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipItem;
import com.synopsys.arc.jenkins.plugins.ownership.Messages;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.util.UserCollectionFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.AccessRightsFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.IUserFilter;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.User;
import java.util.Collection;

/**
 * Ownership job property.
 * @todo Implement generic approaches from 0.0.3
 * @author Oleg Nenashev <nenashev@synopsys.com>
 * @since 0.0.1
 */
public class JobOwnerJobProperty extends JobProperty<Job<?, ?>> 
    implements IOwnershipItem<Job<?, ?>>
{
    OwnershipDescription ownership;
    
    @DataBoundConstructor
    public JobOwnerJobProperty( OwnershipDescription ownershipDescription) {
            this.ownership = ownershipDescription;
    }

    @Override
    public OwnershipDescription getOwnership() {
        return (ownership!=null) ? ownership : OwnershipDescription.DISABLED_DESCR;
    }
    
    public String getDisplayName(User usr) {
        return  JobOwnerHelper.Instance.getDisplayName(usr);
    }
      
    public Collection<User> getUsers()
    {     
        //TODO: Sort users
        IUserFilter filter = new AccessRightsFilter(owner, Job.CONFIGURE);
        Collection<User> res = UserCollectionFilter.filterUsers(User.getAll(), true, filter);
        return res;
    }

    @Override
    public IOwnershipHelper<Job<?, ?>> helper() {
        return JobOwnerHelper.Instance;
    }

    @Override
    public Job<?, ?> getDescribedItem() {
        return owner;   
    }
 
    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public JobProperty<?> newInstance( StaplerRequest req, JSONObject formData ) throws FormException {
            if (formData.containsKey("jobOwnership")) {
                JSONObject ownership = (JSONObject)formData.getJSONObject("jobOwnership");
                OwnershipDescription descr = OwnershipDescription.Parse(ownership);
                return new JobOwnerJobProperty(descr);
            }
            return null;  
        }

        @Override
        public String getDisplayName() {
            return Messages.JobOwnership_Config_SectionTitle();
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }        
    }

    @Override
    public String toString() {
            return ownership.toString();
    }

   //TODO: Restore, when action is ready
   // Ownership action is disabled
   /* @Override
    public Collection<? extends Action> getJobActions(Job<?, ?> job) {
        Collection<OwnershipAction> col = new ArrayList<OwnershipAction>();
         
        ownershipIsEnabled = true;
       
        if (ownershipIsEnabled) 
        {
            col.add(new JobOwnerJobAction(job));
        }
        return col;
    } */
}
