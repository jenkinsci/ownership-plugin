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

import com.synopsys.arc.jenkins.plugins.ownership.ItemOwnershipAction;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.security.Permission;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Ownership action for jobs.
 * @author Oleg Nenashev <nenashev@synopsys.com>
 */
public class JobOwnerJobAction extends ItemOwnershipAction<Job<?,?>> {
    
    public JobOwnerJobAction(Job<?, ?> job) {
      super(job);
    }

    @Override
    public JobOwnerHelper helper() {
        return JobOwnerHelper.Instance;
    }
       
    /** 
     * Gets described job.
     * @deprecated Just for compatibility with 0.0.1
     */
    public Job<?, ?> getJob() {
        return getDescribedItem();
    }
    
    public Permission getOwnerPermission() {
        return OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP;
    }
    
    public Permission getProjectSpecificPermission() {
        return OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP;
    }

    @Override
    public OwnershipDescription getOwnership() {
        return helper().getOwnershipDescription(getDescribedItem());
    } 

    @Override
    public boolean actionIsAvailable() {
        return getDescribedItem().hasPermission(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP);
    }
      
    public void doOwnersSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, UnsupportedEncodingException, ServletException, Descriptor.FormException {
        getDescribedItem().hasPermission(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP);
        
        JSONObject jsonOwnership = (JSONObject) req.getSubmittedForm().getJSONObject("owners");
        OwnershipDescription descr = OwnershipDescription.Parse(jsonOwnership);
        JobOwnerHelper.setOwnership(getDescribedItem(), descr);
        
        rsp.sendRedirect(getDescribedItem().getAbsoluteUrl());
    }
}
