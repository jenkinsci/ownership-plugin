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
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.Folder;
import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPluginConfiguration;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerJobProperty;
import com.synopsys.arc.jenkins.plugins.ownership.util.AbstractOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.util.UserCollectionFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.AccessRightsFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.IUserFilter;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.User;
import java.io.IOException;
import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.ownership.model.OwnershipHelperLocator;
import org.jenkinsci.plugins.ownership.model.OwnershipInfo;

/**
 * Integration with Folders plugin.
 * @author Oleg Nenashev
 * @since 0.9
 */
public class FolderOwnershipHelper extends AbstractOwnershipHelper<AbstractFolder<?>> {
    
    static final FolderOwnershipHelper INSTANCE = new FolderOwnershipHelper();

    @Nonnull
    public static FolderOwnershipHelper getInstance() {
        return INSTANCE;
    }
    
    /**
     * Gets OwnerNodeProperty from job if possible.
     * @param folder Folder
     * @return OwnerNodeProperty or null
     */
    @CheckForNull
    public static FolderOwnershipProperty getOwnerProperty(@Nonnull AbstractFolder<?> folder) {
        FolderOwnershipProperty prop = folder.getProperties().get(FolderOwnershipProperty.class);
        return prop != null ? prop : null;
    }
    
    @Override
    public String getItemTypeName(AbstractFolder<?> item) {
        return "folder";
    }

    @Override
    public String getItemDisplayName(AbstractFolder<?> item) {
        return item.getDisplayName();
    }

    @Override
    public String getItemURL(AbstractFolder<?> item) {
        return item.getUrl();
    }
    
    @Override
    public OwnershipDescription getOwnershipDescription(AbstractFolder<?> item) {
        // TODO: Maybe makes sense to unwrap the method to get a better performance (esp. for Security)
        return getOwnershipInfo(item).getDescription();
    }

    @Override
    public OwnershipInfo getOwnershipInfo(AbstractFolder<?> item) {
        if (item == null) { // Handle renames, etc.
            return OwnershipInfo.DISABLED_INFO;
        }
        
        // Retrieve Ownership from the Folder property
        FolderOwnershipProperty prop = getOwnerProperty(item);
        if (prop != null) {
            OwnershipDescription d = prop.getOwnership();
            if (d.isOwnershipEnabled()) {
                return new OwnershipInfo(prop.getOwnership(), new FolderOwnershipDescriptionSource(item));
            }
        }
        
        // We go to upper items in order to get the ownership description
        if (!OwnershipPluginConfiguration.get().getInheritanceOptions().isBlockInheritanceFromItemGroups()) {
            ItemGroup parent = item.getParent();
            AbstractOwnershipHelper<ItemGroup> located = OwnershipHelperLocator.locate(parent);
            while (located != null) {
                OwnershipInfo fromParent = located.getOwnershipInfo(parent);
                if (fromParent.getDescription().isOwnershipEnabled()) {
                    return fromParent;
                }
                if (parent instanceof Item) {
                    Item parentItem = (Item)parent;
                    parent = parentItem.getParent();
                    located = OwnershipHelperLocator.locate(parent);
                } else {
                    located = null;
                }
            }
        }
        
        return OwnershipInfo.DISABLED_INFO;
    }
    
    @Override
    public Collection<User> getPossibleOwners(AbstractFolder<?> item) {
        if (OwnershipPlugin.getInstance().isRequiresConfigureRights()) {
            IUserFilter filter = new AccessRightsFilter(item, Folder.CONFIGURE);
            return UserCollectionFilter.filterUsers(User.getAll(), true, filter);
        } else {
            return User.getAll();
        }
    }
    
    /**
     * Sets the ownership information.
     * @param folder Folder to be modified
     * @param descr A description to be set. Use null to drop settings.
     * @throws IOException 
     */
    public static void setOwnership(@Nonnull AbstractFolder<?> folder, 
            @CheckForNull OwnershipDescription descr) throws IOException {
        FolderOwnershipProperty prop = getOwnerProperty(folder);
        if (prop == null) {
            prop = new FolderOwnershipProperty(descr);
            folder.addProperty(prop);
        } else {
            prop.setOwnershipDescription(descr);
        }
    }
    
    @Extension(optional = true)
    public static class LocatorImpl extends OwnershipHelperLocator<AbstractFolder<?>> {
        
        @Override
        public AbstractOwnershipHelper<AbstractFolder<?>> findHelper(Object item) {
            if (item instanceof AbstractFolder<?>) {
                return INSTANCE;
            }
            return null;
        }      
    }
}
