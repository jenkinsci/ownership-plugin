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

import com.synopsys.arc.jenkins.plugins.ownership.ItemOwnershipAction;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.security.itemspecific.ItemSpecificSecurity;

import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.security.Permission;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Ownership action for jobs.
 * The action displays "Manage Ownership" action on the left panel.
 * Actually, this action injects {@link JobOwnerJobProperty} into the project.
 * @author Oleg Nenashev
 */
public class JobOwnerJobAction extends ItemOwnershipAction<Job<?,?>> {
     
    public JobOwnerJobAction(Job<?, ?> job) {
      super(job);
    }

    @Nonnull
    @Override
    public JobOwnerHelper helper() {
        return JobOwnerHelper.Instance;
    }
       
    /** 
     * Gets described job.
     * @deprecated Just for compatibility with 0.0.1
     */
    @Deprecated
    public Job<?, ?> getJob() {
        return getDescribedItem();
    }
    
    @Override
    public Permission getOwnerPermission() {
        return OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP;
    }
    
    @Override
    public Permission getProjectSpecificPermission() {
        return OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP;
    }

    @Override
    public OwnershipDescription getOwnership() {
        return helper().getOwnershipDescription(getDescribedItem());
    } 
    
    @CheckForNull
    public ItemSpecificSecurity getItemSpecificSecurity() {
        JobOwnerJobProperty prop = JobOwnerHelper.getOwnerProperty(getDescribedItem());
        if (prop != null && prop.hasItemSpecificSecurity()) {
             return prop.getItemSpecificSecurity();
        }        
        return getGlobalItemSpecificSecurity();
    }
    
    @CheckForNull
    private static ItemSpecificSecurity getGlobalItemSpecificSecurity() {
        return OwnershipPlugin.getInstance().getDefaultJobsSecurity();
    }
    
    /**
     * Checks if the described item has a job-specific security defined.
     * @return true if the item has a job-specific security
     * @since 0.3.1
     */
    public boolean hasItemSpecificSecurity() {
        JobOwnerJobProperty prop = JobOwnerHelper.getOwnerProperty(getDescribedItem());
        return prop != null && prop.hasItemSpecificSecurity();
    }

    /**
     * Gets descriptor of item-specific security page. 
     * This method is being used by UI.
     * @return A descriptor of {@link ItemSpecificSecurity}
     */
    public ItemSpecificSecurity.ItemSpecificDescriptor getItemSpecificDescriptor() {
        return ItemSpecificSecurity.DESCRIPTOR;
    }

    @Override
    public boolean actionIsAvailable() {
        return getDescribedItem().hasPermission(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP);
    }
    
    public HttpResponse doOwnersSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, UnsupportedEncodingException, ServletException, Descriptor.FormException {
        getDescribedItem().checkPermission(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP);
        
        JSONObject jsonOwnership = req.getSubmittedForm().getJSONObject("owners");
        OwnershipDescription descr = OwnershipDescription.parseJSON(jsonOwnership);
        JobOwnerHelper.setOwnership(getDescribedItem(), descr);
        
        return HttpResponses.redirectViaContextPath(getDescribedItem().getUrl());
    }
    
    public HttpResponse doProjectSpecificSecuritySubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, Descriptor.FormException {
        getDescribedItem().checkPermission(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP);
        JSONObject form = req.getSubmittedForm();
        
        if (form.containsKey("itemSpecificSecurity")) {
            JSONObject jsonSpecificSecurity = req.getSubmittedForm().getJSONObject("itemSpecificSecurity");
            ItemSpecificSecurity specific = ItemSpecificSecurity.DESCRIPTOR.newInstance(req, jsonSpecificSecurity);
            JobOwnerHelper.setProjectSpecificSecurity(getDescribedItem(), specific);
        } else { // drop security
            JobOwnerHelper.setProjectSpecificSecurity(getDescribedItem(), null);
        }

        return HttpResponses.redirectViaContextPath(getDescribedItem().getUrl());
    }
    
    public HttpResponse doRestoreDefaultSpecificSecuritySubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, Descriptor.FormException {
        getDescribedItem().checkPermission(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP);
        // Get default security
        ItemSpecificSecurity defaultJobsSecurity = OwnershipPlugin.getInstance().getDefaultJobsSecurity();
        ItemSpecificSecurity val = defaultJobsSecurity != null ? defaultJobsSecurity.clone() : null;
        
        JobOwnerHelper.setProjectSpecificSecurity(getDescribedItem(), val);
        return HttpResponses.redirectViaContextPath(getDescribedItem().getUrl());
    }
}
