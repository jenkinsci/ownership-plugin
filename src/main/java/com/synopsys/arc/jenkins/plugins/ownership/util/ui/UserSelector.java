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
package com.synopsys.arc.jenkins.plugins.ownership.util.ui;

import hudson.Extension;
import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.User;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Describable Item, which allows to configure user.
 * Features: validation
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 */
//TODO: Autocompletion
public class UserSelector implements Describable<UserSelector> {
    /**ID of the user*/
    String selectedUserId;

    @DataBoundConstructor
    public UserSelector(String selectedUserId) {
        this.selectedUserId = hudson.Util.fixEmptyAndTrim(selectedUserId);
    }

    public String getSelectedUserId() {
        return selectedUserId;
    }

    @Override
    public Descriptor<UserSelector> getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean equals(Object obj) {
        
        if (obj instanceof UserSelector) {
            UserSelector cmp = (UserSelector)obj;
            return selectedUserId != null ? selectedUserId.equals(cmp.selectedUserId) : cmp.selectedUserId == null;
        }
            
        return false;
    
    }
    
    
        
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    public static class DescriptorImpl extends Descriptor<UserSelector> {
        @Override
        public String getDisplayName() {
            return "Messages";
        }
        
        public FormValidation doCheckSelectedUserId(@QueryParameter String selectedUserId) {
            selectedUserId = Util.fixEmptyAndTrim(selectedUserId);
            if (selectedUserId == null) {
                return FormValidation.error("Field is empty. Field will be ignored");
            }

            User usr = User.get(selectedUserId, false, null);
            if (usr == null) {
                return FormValidation.warning("User " + selectedUserId + " is not registered in Jenkins");
            }

            return FormValidation.ok();
        }
    }
}
