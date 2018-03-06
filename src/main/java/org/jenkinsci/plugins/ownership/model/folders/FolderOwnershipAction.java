/*
 * The MIT License
 *
 * Copyright (c) 2015-2017 Oleg Nenashev.
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
package org.jenkinsci.plugins.ownership.model.folders;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.ItemOwnershipAction;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.util.ui.OwnershipLayoutFormatter;
import org.jenkinsci.plugins.ownership.security.folderspecific.FolderSpecificSecurity;
import hudson.model.Descriptor;
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
 * Allows managing actions for {@link AbstractFolder}s.
 * @author Oleg Nenashev
 * @since 0.9
 */
public class FolderOwnershipAction extends ItemOwnershipAction<AbstractFolder<?>> {

    public FolderOwnershipAction(@Nonnull AbstractFolder<?> folder) {
        super(folder);
    }

    @Nonnull
    @Override
    public IOwnershipHelper<AbstractFolder<?>> helper() {
        return FolderOwnershipHelper.Instance;
    }
    
    public OwnershipLayoutFormatter<AbstractFolder<?>> getLayoutFormatter() {
        return FolderOwnershipHelper.Instance.getLayoutFormatter();
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
        return FolderOwnershipHelper.Instance.getOwnershipDescription(getDescribedItem());
    }
    
    @CheckForNull
    public FolderSpecificSecurity getItemSpecificSecurity() {
        FolderOwnershipProperty prop = FolderOwnershipHelper.getOwnerProperty(getDescribedItem());
        if (prop != null && prop.hasItemSpecificSecurity()) {
            return prop.getItemSpecificSecurity();
        }
        return OwnershipPlugin.getInstance().getDefaultFoldersSecurity();
    }
    
    /**
     * Checks if the described item has a folder-specific security defined.
     * @return true if the item has a folder-specific security
     * @since 0.3.1
     */
    public boolean hasItemSpecificSecurity() {
        FolderOwnershipProperty prop = FolderOwnershipHelper.getOwnerProperty(getDescribedItem());
        return prop != null && prop.hasItemSpecificSecurity();
    }
    
    /**
     * Gets descriptor of folder-specific security page.
     * This method is being used by UI.
     * @return A descriptor of {@link FolderSpecificSecurity}
     */
    public FolderSpecificSecurity.FolderSpecificDescriptor getItemSpecificDescriptor() {
        return FolderSpecificSecurity.DESCRIPTOR;
    }
    
    @Override
    public boolean actionIsAvailable() {
        return getDescribedItem().hasPermission(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP);
    }
    
    public HttpResponse doOwnersSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, UnsupportedEncodingException, ServletException, Descriptor.FormException {
        getDescribedItem().checkPermission(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP);
        
        JSONObject jsonOwnership = req.getSubmittedForm().getJSONObject("owners");
        OwnershipDescription descr = OwnershipDescription.parseJSON(jsonOwnership);
        FolderOwnershipHelper.setOwnership(getDescribedItem(), descr);
        
        return HttpResponses.redirectViaContextPath(getDescribedItem().getUrl());
    }
    
    public HttpResponse doProjectSpecificSecuritySubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, Descriptor.FormException {
        getDescribedItem().checkPermission(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP);
        JSONObject form = req.getSubmittedForm();
        
        if (form.containsKey("itemSpecificSecurity")) {
            JSONObject jsonSpecificSecurity = req.getSubmittedForm().getJSONObject("itemSpecificSecurity");
            FolderSpecificSecurity specific = FolderSpecificSecurity.DESCRIPTOR.newInstance(req, jsonSpecificSecurity);
            FolderOwnershipHelper.setProjectSpecificSecurity(getDescribedItem(), specific);
        } else { // drop security
            FolderOwnershipHelper.setProjectSpecificSecurity(getDescribedItem(), null);
        }
        
        return HttpResponses.redirectViaContextPath(getDescribedItem().getUrl());
    }
    
    public HttpResponse doRestoreDefaultSpecificSecuritySubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, Descriptor.FormException {
        getDescribedItem().checkPermission(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP);
        // Get default security
        FolderSpecificSecurity defaultFoldersSecurity = OwnershipPlugin.getInstance().getDefaultFoldersSecurity();
        FolderSpecificSecurity val = defaultFoldersSecurity != null ? defaultFoldersSecurity.clone() : null;
        
        FolderOwnershipHelper.setProjectSpecificSecurity(getDescribedItem(), val);
        return HttpResponses.redirectViaContextPath(getDescribedItem().getUrl());
    }
}
