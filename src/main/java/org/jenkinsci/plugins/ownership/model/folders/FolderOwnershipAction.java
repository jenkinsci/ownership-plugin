/*
 * The MIT License
 *
 * Copyright (c) 2015 Oleg Nenashev.
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
import com.cloudbees.hudson.plugins.folder.Folder;
import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.ItemOwnershipAction;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.util.ui.OwnershipLayoutFormatter;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.security.Permission;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Allows managing actions for {@link Folder}s.
 * @author Oleg Nenashev
 * @since 0.9
 */
public class FolderOwnershipAction extends ItemOwnershipAction<AbstractFolder<?>> {

    //TODO: May become a problem once we need to make it flexible (not implemented). Move to helper?
    private static final OwnershipLayoutFormatter<AbstractFolder<?>> DEFAULT_FOLDER_FORMATTER 
            = new OwnershipLayoutFormatter.DefaultJobFormatter<AbstractFolder<?>>();
    
    public FolderOwnershipAction(@Nonnull Folder folder) {
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
        return getDescribedItem().hasPermission(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP);
    }

    @Override
    public IOwnershipHelper<AbstractFolder<?>> helper() {
        return FolderOwnershipHelper.getInstance();
    }
    
    public OwnershipLayoutFormatter<AbstractFolder<?>> getLayoutFormatter() {
        return DEFAULT_FOLDER_FORMATTER;
    }
    
    public HttpResponse doOwnersSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, UnsupportedEncodingException, ServletException, Descriptor.FormException {
        getDescribedItem().checkPermission(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP);
        
        JSONObject jsonOwnership = req.getSubmittedForm().getJSONObject("owners");
        OwnershipDescription descr = OwnershipDescription.parseJSON(jsonOwnership);
        FolderOwnershipHelper.setOwnership(getDescribedItem(), descr);
        
        return HttpResponses.redirectViaContextPath(getDescribedItem().getUrl());
    }
}
