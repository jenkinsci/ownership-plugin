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
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPluginConfiguration;
import com.synopsys.arc.jenkins.plugins.ownership.security.itemspecific.ItemSpecificSecurity;
import com.synopsys.arc.jenkins.plugins.ownership.util.AbstractOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.util.UserCollectionFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.AccessRightsFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.IUserFilter;
import com.synopsys.arc.jenkins.plugins.ownership.wrappers.OwnershipBuildWrapper;
import hudson.Extension;
import hudson.matrix.MatrixConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.Project;
import hudson.model.User;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.ownership.model.OwnershipHelperLocator;
import org.jenkinsci.plugins.ownership.model.OwnershipInfo;
import org.jenkinsci.plugins.ownership.model.jobs.JobOwnershipDescriptionSource;

/**
 * Helper for Jobs Ownership.
 * @since 0.0.3
 * @author Oleg Nenashev
 */
public class JobOwnerHelper extends AbstractOwnershipHelper<Job<?,?>> {
    
    public final static JobOwnerHelper Instance = new JobOwnerHelper();
      
    /**
     * Gets {@link JobOwnerJobProperty} from the job if possible.
     * The function also handles multi-configuration jobs, so it should be used 
     * wherever it is possible.
     * This method should not be used to retrieve Ownership descriptions, 
     * because it does not take inheritance into account.
     * @param job Job
     * @return JobOwnerJobProperty or {@code null} if it is not configured
     */
    @CheckForNull
    public static JobOwnerJobProperty getOwnerProperty(@Nonnull Job<?,?> job) {
        // Get property from the main job
        JobProperty prop = job.getProperty(JobOwnerJobProperty.class);
        if (prop != null) {
            return (JobOwnerJobProperty)prop;
        }
        
        // Handle matrix prject
        if (job instanceof MatrixConfiguration) {
            return getOwnerProperty(((MatrixConfiguration)job).getParent());
        } 
        return null;
    }
    
    public static boolean isUserExists(@Nonnull User user) {
        assert (user != null);
        return isUserExists(user.getId());
    }
    
    public static boolean isUserExists(@Nonnull String userIdOrFullName) {
        assert (userIdOrFullName != null);
        return User.get(userIdOrFullName, false, null) != null;
    }
     
    @Override
    public @Nonnull OwnershipDescription getOwnershipDescription(@Nonnull Job<?, ?> job) {
        // TODO: Maybe makes sense to unwrap the method to get a better performance (esp. for Security)
        return getOwnershipInfo(job).getDescription();
    }

    @Override
    public OwnershipInfo getOwnershipInfo(Job<?, ?> job) {
        JobOwnerJobProperty prop = getOwnerProperty(job);     
        if (prop != null) {
            OwnershipDescription d = prop.getOwnership();
            if (d.isOwnershipEnabled()) {
                // If Ownership on this level is enabled, we return it
                return new OwnershipInfo(d, new JobOwnershipDescriptionSource(job));
            }
        }
        
        // We go to upper items in order to get the ownership description
        if (!OwnershipPluginConfiguration.get().getInheritanceOptions().isBlockInheritanceFromItemGroups()) {
            ItemGroup parent = job.getParent();
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
        
        // Fallback: we have not found the Ownership using known approaches
        return OwnershipInfo.DISABLED_INFO;
    }

    /**
     * Sets the ownership information.
     * @param job A job to be modified
     * @param descr A description to be set. Use null to drop settings.
     * @throws IOException 
     */
    public static void setOwnership(@Nonnull Job<?, ?> job, 
            @CheckForNull OwnershipDescription descr) throws IOException {
        JobOwnerJobProperty prop = JobOwnerHelper.getOwnerProperty(job);
        if (prop == null) {
            prop = new JobOwnerJobProperty(descr, null);
            job.addProperty(prop);
        } else {
            prop.setOwnershipDescription(descr);
        }
    }
        
    /**
     * Sets the project-specific security.
     * @param job A job to be modified
     * @param security Security settings to be set. Use null to drop settings 
     * @throws IOException 
     */
    public static void setProjectSpecificSecurity(@Nonnull Job<?, ?> job, 
            @CheckForNull ItemSpecificSecurity security) throws IOException {
        JobOwnerJobProperty prop = JobOwnerHelper.getOwnerProperty(job);
        if (prop == null) {
            throw new IOException("Ownership is not configured for "+job);
        } else {
            prop.setItemSpecificSecurity(security);
        }
    }

    @Override
    public @Nonnull Collection<User> getPossibleOwners(@Nonnull Job<?, ?> item) {
        if (OwnershipPlugin.getInstance().isRequiresConfigureRights()) {
            IUserFilter filter = new AccessRightsFilter(item, Job.CONFIGURE);
            return UserCollectionFilter.filterUsers(User.getAll(), true, filter);
        } else {
            return User.getAll();
        }
    }  
   
    @Override
    public String getItemTypeName(Job<?, ?> item) {
        return "job";
    }
    
    @Override
    public String getItemDisplayName(Job<?, ?> item) {
        return item.getFullDisplayName();
    }

    public String getItemURL(Job<?, ?> item) {
        return item.getUrl();
    }
    
    @Extension
    public static class LocatorImpl extends OwnershipHelperLocator<Job<?,?>> {
        
        @Override
        public AbstractOwnershipHelper<Job<?,?>> findHelper(Object item) {
            if (item instanceof Job<?,?>) {
                return Instance;
            }
            return null;
        }      
    }
}
