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

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.util.AbstractOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.util.UserCollectionFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.AccessRightsFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.IUserFilter;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.User;
import java.io.IOException;
import java.util.Collection;

/**
 * Helper for Jobs Ownership.
 * @since 0.0.3
 * @author Oleg Nenashev <nenashev@synopsys.com>
 */
public class JobOwnerHelper extends AbstractOwnershipHelper<Job<?,?>>{
    final static JobOwnerHelper Instance = new JobOwnerHelper();
    
    /**
     * Gets JobOwnerProperty from job if possible
     * @param job Job
     * @return JobOwnerJobProperty or null
     */
    public static JobOwnerJobProperty getOwnerProperty(Job<?,?> job) {
        AbstractProject project = (AbstractProject) job;
        JobProperty prop = project.getProperty(JobOwnerJobProperty.class);
        return prop != null ? (JobOwnerJobProperty)prop : null;
    }
       
    public static boolean isUserExists(User user) {
        assert (user != null);
        return isUserExists(user.getId());
    }
    
    public static boolean isUserExists(String userIdOrFullName) {
        assert (userIdOrFullName != null);
        return User.get(userIdOrFullName, false, null) != null;
    }
        
    @Override
    public OwnershipDescription getOwnershipDescription(Job<?, ?> job) {
        JobOwnerJobProperty prop = getOwnerProperty(job);     
        return (prop != null) ? prop.getOwnership() : OwnershipDescription.DISABLED_DESCR;
    }
    
    public static void setOwnership(Job<?, ?> job, OwnershipDescription descr) throws IOException {
        JobOwnerJobProperty prop = JobOwnerHelper.getOwnerProperty(job);
        if (prop == null) {
            prop = new JobOwnerJobProperty(descr);
            job.addProperty(prop);
        } else {
            prop.setOwnershipDescription(descr);
        }
    }

    @Override
    public Collection<User> getPossibleOwners(Job<?, ?> item) {
        if (OwnershipPlugin.Instance().isRequiresConfigureRights()) {
            IUserFilter filter = new AccessRightsFilter(item, Job.CONFIGURE);
            return UserCollectionFilter.filterUsers(User.getAll(), true, filter);
        } else {
            return User.getAll();
        }
    }  
}
