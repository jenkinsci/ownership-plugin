/*
 * The MIT License
 *
 * Copyright (c) 2015-2017 Oleg Nenashev.
 * Copyright 2026 Rustam Atai (atai)
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
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.Permission;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.ownership.security.folderspecific.FolderSpecificSecurity;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Allows managing actions for {@link AbstractFolder}s.
 * @author Oleg Nenashev
 * @author Rustam Atai
 * @since 0.9
 */
public class FolderOwnershipAction extends ItemOwnershipAction<AbstractFolder<?>> {

    // TODO: May become a problem once we need to make it flexible (not implemented). Move to helper?
    private static final OwnershipLayoutFormatter<AbstractFolder<?>> DEFAULT_FOLDER_FORMATTER =
            new OwnershipLayoutFormatter.DefaultJobFormatter<>();

    public FolderOwnershipAction(@Nonnull AbstractFolder<?> folder) {
        super(folder);
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
    public boolean actionIsAvailable() {
        // Action should be available to anyone with READ permission on the item
        // Individual methods will check MANAGE_ITEMS_OWNERSHIP and return 403 if needed
        return getDescribedItem().hasPermission(Item.READ);
    }

    @Override
    public IOwnershipHelper<AbstractFolder<?>> helper() {
        return FolderOwnershipHelper.getInstance();
    }

    public OwnershipLayoutFormatter<AbstractFolder<?>> getLayoutFormatter() {
        return DEFAULT_FOLDER_FORMATTER;
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
     * Checks if the described item has folder-specific security defined locally.
     *
     * @return true if the item has folder-specific security
     */
    public boolean hasItemSpecificSecurity() {
        FolderOwnershipProperty prop = FolderOwnershipHelper.getOwnerProperty(getDescribedItem());
        return prop != null && prop.hasItemSpecificSecurity();
    }

    /**
     * Gets descriptor of folder-specific security page.
     * This method is being used by UI.
     *
     * @return A descriptor of {@link FolderSpecificSecurity}
     */
    public FolderSpecificSecurity.FolderSpecificDescriptor getItemSpecificDescriptor() {
        return FolderSpecificSecurity.DESCRIPTOR;
    }

    @RequirePOST
    public HttpResponse doOwnersSubmit(StaplerRequest2 req, StaplerResponse2 rsp)
            throws IOException, UnsupportedEncodingException, ServletException, Descriptor.FormException {
        getDescribedItem().checkPermission(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP);

        JSONObject jsonOwnership = req.getSubmittedForm().getJSONObject("owners");
        OwnershipDescription descr = OwnershipDescription.parseJSON(jsonOwnership);
        FolderOwnershipHelper.setOwnership(getDescribedItem(), descr);

        return HttpResponses.redirectViaContextPath(getDescribedItem().getUrl());
    }

    @RequirePOST
    public HttpResponse doProjectSpecificSecuritySubmit(StaplerRequest2 req, StaplerResponse2 rsp)
            throws IOException, ServletException, Descriptor.FormException {
        getDescribedItem().checkPermission(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP);
        JSONObject form = req.getSubmittedForm();

        if (form.containsKey("itemSpecificSecurity")) {
            JSONObject jsonSpecificSecurity = form.getJSONObject("itemSpecificSecurity");
            FolderSpecificSecurity specific = FolderSpecificSecurity.DESCRIPTOR.newInstance(req, jsonSpecificSecurity);
            FolderOwnershipHelper.setProjectSpecificSecurity(getDescribedItem(), specific);
        } else {
            FolderOwnershipHelper.setProjectSpecificSecurity(getDescribedItem(), null);
        }

        return HttpResponses.redirectViaContextPath(getDescribedItem().getUrl());
    }

    @RequirePOST
    public HttpResponse doRestoreDefaultSpecificSecuritySubmit(StaplerRequest2 req, StaplerResponse2 rsp)
            throws IOException, ServletException, Descriptor.FormException {
        getDescribedItem().checkPermission(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP);
        FolderSpecificSecurity defaultFoldersSecurity =
                OwnershipPlugin.getInstance().getDefaultFoldersSecurity();
        FolderSpecificSecurity val = defaultFoldersSecurity != null ? defaultFoldersSecurity.clone() : null;
        FolderOwnershipHelper.setProjectSpecificSecurity(getDescribedItem(), val);
        return HttpResponses.redirectViaContextPath(getDescribedItem().getUrl());
    }
}
