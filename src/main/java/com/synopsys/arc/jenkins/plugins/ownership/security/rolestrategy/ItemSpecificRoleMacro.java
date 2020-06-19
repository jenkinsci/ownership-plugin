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
package com.synopsys.arc.jenkins.plugins.ownership.security.rolestrategy;

import org.jenkinsci.plugins.ownership.model.OwnershipHelperLocator;
import com.synopsys.arc.jenkins.plugins.ownership.util.AbstractOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.Messages;
import com.synopsys.arc.jenkins.plugins.rolestrategy.Macro;
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.security.AccessControlled;
import hudson.security.Permission;

/**
 * Macro invokes evaluation of item-specific access rights.
 * @author Oleg Nenashev
 * @since 0.4
 */
@Extension(optional = true, ordinal = -5)
public class ItemSpecificRoleMacro extends AbstractOwnershipRoleMacro {
    
    @Override
    public String getName() {
        return Messages.Security_RoleStrategy_ItemSpecificMacro_Name();
    }

    @Override
    public String getDescription() {
        return Messages.Security_RoleStrategy_ItemSpecificMacro_Description();
    }

    @Override
    @SuppressFBWarnings(value = "NM_METHOD_NAMING_CONVENTION", justification = "Part of other plugin API")
    public boolean IsApplicable(RoleType roleType) {
        return roleType == RoleType.Project;
    }
   
    @Override
    public boolean hasPermission(String sid, Permission p, RoleType type, AccessControlled item, Macro macro) {
        if (type == RoleType.Project) {
            AbstractOwnershipHelper helper = OwnershipHelperLocator.locate(item);
            if (helper != null) {
                return helper.hasItemSpecificPermission(item, sid, p);
            }
        }
        return false;
    }
}
