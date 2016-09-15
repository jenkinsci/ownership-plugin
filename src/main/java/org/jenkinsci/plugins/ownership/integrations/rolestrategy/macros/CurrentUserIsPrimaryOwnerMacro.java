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
package org.jenkinsci.plugins.ownership.integrations.rolestrategy.macros;

import com.synopsys.arc.jenkins.plugins.ownership.security.rolestrategy.AbstractOwnershipRoleMacro;
import static com.synopsys.arc.jenkins.plugins.ownership.security.rolestrategy.AbstractOwnershipRoleMacro.hasPermission;
import com.synopsys.arc.jenkins.plugins.rolestrategy.Macro;
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType;
import hudson.Extension;
import hudson.model.User;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import org.jenkinsci.plugins.ownership.integrations.rolestrategy.Messages;

/**
 * Checks if the current user is a primary owner of the item.
 * @author Oleg Nenashev
 * @since 0.9
 */
@Extension(optional = true)
public class CurrentUserIsPrimaryOwnerMacro extends AbstractOwnershipRoleMacro {
    
    @Override
    public String getName() {
        return "CurrentUserIsPrimaryOwner"; 
    }
    
    @Override
    public String getDescription() {
        return Messages.CurrentUserIsPrimaryOwnerMacro_description();
    }

    @Override
    public boolean hasPermission(String sid, Permission p, RoleType type, AccessControlled item, Macro macro) {    
        User user = User.current();              
        return hasPermission(user, type, item, macro, false);
    }
}
