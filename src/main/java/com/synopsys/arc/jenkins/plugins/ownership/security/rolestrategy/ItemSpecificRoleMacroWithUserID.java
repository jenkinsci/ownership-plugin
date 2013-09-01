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
package com.synopsys.arc.jenkins.plugins.ownership.security.rolestrategy;

import com.synopsys.arc.jenkins.plugins.ownership.Messages;
import com.synopsys.arc.jenkins.plugins.rolestrategy.Macro;
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType;
import hudson.Extension;
import hudson.model.User;
import hudson.security.AccessControlled;
import hudson.security.Permission;

/**
 * Macro invokes evaluation of item-specific access rights for the current user.
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 */
@Extension
public class ItemSpecificRoleMacroWithUserID extends ItemSpecificRoleMacro {

    @Override
    public String getName() {
        return super.getName() + "WithUserID";
    }
    
    @Override
    public String getDescription() {
        return super.getDescription()+Messages.Security_RoleStrategy_WithUserDescriptionSuffix();
    }

    @Override
    public boolean hasPermission(String sid, Permission p, RoleType type, AccessControlled item, Macro macro) {
        // check sid
        boolean res = super.hasPermission(sid, p, type, item, macro);
        if (res) return true;
        
        if (sid.equals(AUTHENTICATED_SID)) {
            User usr = User.current();
            return usr != null ? super.hasPermission(usr.getId(), p, type, item, macro) : false; 
        }
        return false;
    }    
}
