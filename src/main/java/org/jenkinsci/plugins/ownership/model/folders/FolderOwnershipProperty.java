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

import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipItem;
import com.synopsys.arc.jenkins.plugins.ownership.Messages;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import org.jenkinsci.plugins.ownership.security.folderspecific.FolderSpecificSecurity;
import com.synopsys.arc.jenkins.plugins.ownership.util.ui.OwnershipLayoutFormatter;
import com.synopsys.arc.jenkins.plugins.ownership.util.UserCollectionFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.AccessRightsFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.IUserFilter;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Items;
import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import hudson.model.User;
import hudson.util.XStream2;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.servlet.ServletException;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Ownership property for {@link AbstractFolder}s.
 * @author Oleg Nenashev
 * @since 0.9
 */
public class FolderOwnershipProperty extends AbstractFolderProperty<AbstractFolder<?>>
    implements IOwnershipItem<AbstractFolder<?>>
{
    @CheckForNull
    OwnershipDescription ownership;
    
    /**
     * Additional matrix with project security
     */
    @CheckForNull
    FolderSpecificSecurity itemSpecificSecurity;
    
    @DataBoundConstructor
    public FolderOwnershipProperty(OwnershipDescription ownership, FolderSpecificSecurity security) {
        this.ownership = ownership;
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
    public FolderSpecificSecurity getItemSpecificSecurity() {
        return itemSpecificSecurity != null ? itemSpecificSecurity : OwnershipPlugin.getInstance().getDefaultFoldersSecurity();
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
        return FolderOwnershipHelper.Instance.getDisplayName(usr);
    }
    
    public Collection<User> getUsers()
    {
        //TODO: Sort users
        IUserFilter filter = new AccessRightsFilter(owner, AbstractFolder.CONFIGURE);
        Collection<User> res = UserCollectionFilter.filterUsers(User.getAll(), true, filter);
        return res;
    }
    
    @Override
    public IOwnershipHelper<AbstractFolder<?>> helper() {
        return FolderOwnershipHelper.Instance;
    }

    @Override
    public AbstractFolder<?> getDescribedItem() {
        return owner;
    }

    @Override
    public AbstractFolderProperty<?> reconfigure(StaplerRequest req, JSONObject form) throws Descriptor.FormException {
        return new FolderOwnershipProperty(ownership, itemSpecificSecurity);
    }

    public OwnershipLayoutFormatter<AbstractFolder<?>> getLayoutFormatter() {
        return FolderOwnershipHelper.Instance.getLayoutFormatter();
    }
    
    @Extension(optional = true)
    public static class DescriptorImpl extends AbstractFolderPropertyDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.JobOwnership_Config_SectionTitle();
        }
        
        @Override
        public boolean isApplicable(Class<? extends AbstractFolder> jobType) {
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
        JSONObject jsonOwnership = formData.getJSONObject("owners");
        setOwnershipDescription(OwnershipDescription.parseJSON(jsonOwnership));
    }
    
    public void setOwnershipDescription(@CheckForNull OwnershipDescription descr) throws IOException {
        ownership = descr;
        owner.save();
    }    

    public void setItemSpecificSecurity(@CheckForNull FolderSpecificSecurity security) throws IOException {
        itemSpecificSecurity = security;
        owner.save();
    }
    
    static {
        // TODO: Remove reflection once baseline is updated past 2.85.
        try {
            Method m = XStream2.class.getMethod("addCriticalField", Class.class, String.class);
            m.invoke(Items.XSTREAM2, FolderOwnershipProperty.class, "ownership");
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
