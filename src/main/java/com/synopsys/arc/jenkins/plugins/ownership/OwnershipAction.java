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
package com.synopsys.arc.jenkins.plugins.ownership;

import hudson.model.Action;

/**
 * Provides Floating box with ownership description.
 * @author Oleg Nenashev <nenashev@synopsys.com>
 */
public abstract class OwnershipAction implements Action {
    public static final String ICON_FILENAME = "user.gif";
    public static final String URL_NAME = "ownership";
    
    @Override
    public String getIconFileName() {
         return actionIsAvailable() ? ICON_FILENAME : null; 
    }
     
    @Override
    public String getUrlName() {
        return actionIsAvailable() ? URL_NAME : null;
    }

    @Override
    public String getDisplayName() {
        return actionIsAvailable() ? Messages.OwnershipAction_ManageOwnership_DisplayName() : null;
    }
       
    public String getManageOwnershipTitle() {
        return Messages.OwnershipAction_ManageOwners_DisplayName();
    }
    
    public String getConfigureSpecificAccessTitle() {
        return Messages.OwnershipAction_ConfigureSpecificAccess_DisplayName();
    }
   
    public abstract boolean actionIsAvailable();
}
