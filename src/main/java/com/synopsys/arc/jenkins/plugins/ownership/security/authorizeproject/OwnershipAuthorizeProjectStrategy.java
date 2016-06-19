/*
 * The MIT License
 *
 * Copyright 2014 Oleg Nenashev, Synopsys Inc.
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

package com.synopsys.arc.jenkins.plugins.ownership.security.authorizeproject;

import com.synopsys.arc.jenkins.plugins.ownership.Messages;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Queue;
import hudson.model.User;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.jenkinsci.plugins.authorizeproject.AuthorizeProjectStrategy;
import org.jenkinsci.plugins.authorizeproject.AuthorizeProjectStrategyDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Provides support of the ownership {@link AuthorizeProjectStrategy}.
 * This strategy authenticates as a job's owner if it is specified.
 * Otherwise, the anonymous user will be used.
 * @since 0.5
 * @author Oleg Nenashev
 */
public class OwnershipAuthorizeProjectStrategy extends AuthorizeProjectStrategy {

    @DataBoundConstructor
    public OwnershipAuthorizeProjectStrategy() {
    }

    @Override
    public Authentication authenticate(AbstractProject<?, ?> ap, Queue.Item item) {    
        OwnershipDescription d = JobOwnerHelper.Instance.getOwnershipDescription(ap);
        if (!d.hasPrimaryOwner()) { // fallback to anonymous
            return Jenkins.ANONYMOUS;
        }    
        User owner = User.get(d.getPrimaryOwnerId(), false, Collections.emptyMap());
        if (owner == null) { // fallback to anonymous
            return Jenkins.ANONYMOUS;
        }
        try {
            return owner.impersonate();
        } catch (UsernameNotFoundException ex) { // fallback to anonymous
            return Jenkins.ANONYMOUS;
        }
    }
      
    @Extension(optional = true)
    public static class DescriptorImpl extends AuthorizeProjectStrategyDescriptor {
        
        @Override
        public String getDisplayName() {
            return Messages.Security_AuthorizeProject_OwnershipAuthorizeProjectStrategy_DisplayName();
        }
    }
}
