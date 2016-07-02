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

import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipItem;
import com.synopsys.arc.jenkins.plugins.ownership.Messages;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.security.itemspecific.ItemSpecificSecurity;
import com.synopsys.arc.jenkins.plugins.ownership.util.UserCollectionFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.ui.OwnershipLayoutFormatter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.AccessRightsFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.IUserFilter;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.User;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Ownership job property.
 * TODO: Implement generic approaches from 0.0.3
 * @author Oleg Nenashev
 * @since 0.0.1
 */
public class JobOwnerJobProperty extends JobProperty<Job<?, ?>> 
    implements IOwnershipItem<Job<?, ?>>
{
    @CheckForNull
    OwnershipDescription ownership;

    /**
     * Additional matrix with project security
     */
    @CheckForNull
    ItemSpecificSecurity itemSpecificSecurity;
        
    @DataBoundConstructor
    public JobOwnerJobProperty( OwnershipDescription ownershipDescription, ItemSpecificSecurity security) {
        this.ownership = ownershipDescription;
        this.itemSpecificSecurity = security;
    }

    @Override
    public OwnershipDescription getOwnership() {
        return (ownership!=null) ? ownership : OwnershipDescription.DISABLED_DESCR;
    }

    /**
     * Gets current configuration of item-specific security.
     * The function returns a default configuration if security is not
     * configured. Use {@link #hasItemSpecificSecurity() hasItemSpecificSecurity} 
     * to check an origin of permissions.
     * @return ItemSpecific security or {@code null} if it is not configured
     * @since 0.3
     */
    @CheckForNull
    public ItemSpecificSecurity getItemSpecificSecurity() {
        return itemSpecificSecurity != null 
                ? itemSpecificSecurity 
                : OwnershipPlugin.getInstance().getDefaultJobsSecurity();
    }
    
    /**
     * Checks if job-specific security is configured.
     * @return true if job-specific security is configured
     * @since 0.3.1
     */
    public boolean hasItemSpecificSecurity() {
        return itemSpecificSecurity != null;
    }
    
    public String getDisplayName(User usr) {
        return JobOwnerHelper.Instance.getDisplayName(usr);
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

    @Override
    public JobProperty<?> reconfigure(StaplerRequest req, JSONObject form) throws Descriptor.FormException {
        return new JobOwnerJobProperty(ownership, itemSpecificSecurity);
    }
    
    public OwnershipLayoutFormatter<Job<?, ?>> getLayoutFormatter() {
        return OwnershipPlugin.getInstance().getOwnershipLayoutFormatterProvider().getLayoutFormatter(getDescribedItem());
    }   
    
    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {
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
        StringBuilder bldr = new StringBuilder();
        bldr.append(ownership != null ? ownership.toString() : "ownership not set");
        bldr.append(" ");
        bldr.append(itemSpecificSecurity != null ? "with specific permissions" : "without specific permissions");
        return bldr.toString();
    }
    
    public void doOwnersSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, UnsupportedEncodingException, ServletException, Descriptor.FormException {
        JSONObject formData = req.getSubmittedForm();
        JSONObject jsonOwnership = (JSONObject) formData.getJSONObject("owners");
        setOwnershipDescription(OwnershipDescription.parseJSON(jsonOwnership));
    }
    
    public void setOwnershipDescription(@CheckForNull OwnershipDescription descr) throws IOException {
        ownership = descr;
        owner.save();
    }
    
    public void setItemSpecificSecurity(@CheckForNull ItemSpecificSecurity security) throws IOException {
        itemSpecificSecurity = security;
        owner.save();
    }
}
