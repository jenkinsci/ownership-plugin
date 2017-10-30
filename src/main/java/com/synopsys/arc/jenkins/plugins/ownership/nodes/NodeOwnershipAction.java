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
package com.synopsys.arc.jenkins.plugins.ownership.nodes;

import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.ItemOwnershipAction;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;

import hudson.Util;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.security.Permission;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Node ownership action.
 * @author Oleg Nenashev
 * @since 0.2
 */
public class NodeOwnershipAction extends ItemOwnershipAction<Computer> {
    
    public NodeOwnershipAction(Computer owner) {
        super(owner);
    }

    @Override
    public boolean actionIsAvailable() {
        return getDescribedItem().hasPermission(OwnershipPlugin.MANAGE_SLAVES_OWNERSHIP);
    }   

    @Override
    public Permission getOwnerPermission() {
        return OwnershipPlugin.MANAGE_SLAVES_OWNERSHIP;
    }

    @Override
    public Permission getProjectSpecificPermission() {
        return OwnershipPlugin.MANAGE_SLAVES_OWNERSHIP;
    }

    @Override
    public IOwnershipHelper<Computer> helper() {
        return ComputerOwnerHelper.INSTANCE;
    }

    @Override
    public OwnershipDescription getOwnership() {
        return helper().getOwnershipDescription(getDescribedItem());
    }
    
    /**
     * Get absolute URL of the computer.
     * This method is just a copy of getAbsoluteUrl from AbstractItem.
     * @param computer
     * @return 
     */
    public static String getAbsoluteUrl(@Nonnull Computer computer) {
        String r = Jenkins.getActiveInstance().getRootUrl();
        if(r==null) {
            throw new IllegalStateException("Root URL isn't configured yet. Cannot compute absolute URL.");
        }
        return Util.encode(r+computer.getUrl());
    }
    
    public HttpResponse doOwnersSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, UnsupportedEncodingException, ServletException, Descriptor.FormException {
        getDescribedItem().checkPermission(OwnershipPlugin.MANAGE_SLAVES_OWNERSHIP);
        
        JSONObject jsonOwnership = req.getSubmittedForm().getJSONObject("owners");
        OwnershipDescription descr = OwnershipDescription.parseJSON(jsonOwnership);
        ComputerOwnerHelper.setOwnership(getDescribedItem(), descr);
        
        return HttpResponses.redirectViaContextPath(getDescribedItem().getUrl());
    }
}
