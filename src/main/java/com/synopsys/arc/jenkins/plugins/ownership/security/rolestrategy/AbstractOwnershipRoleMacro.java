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

import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.nodes.NodeOwnerHelper;
import com.michelin.cio.hudson.plugins.rolestrategy.PermissionEntry;
import com.synopsys.arc.jenkins.plugins.rolestrategy.Macro;
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleMacroExtension;
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType;
import static com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType.Project;
import static com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType.Slave;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.Computer;
import hudson.model.Item;
import hudson.model.Node;
import hudson.model.User;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import org.jenkinsci.plugins.ownership.model.OwnershipHelperLocator;

import javax.annotation.CheckForNull;

/**
 * An abstract class for {@link RoleMacroExtension}s provided by the ownership plugin.
 * @author Oleg Nenashev
 * @since 0.1
 */
public abstract class AbstractOwnershipRoleMacro extends RoleMacroExtension {
    
    public static final String NO_SID_SUFFIX="NoSid";
    protected static final String AUTHENTICATED_SID = "authenticated";
    
    @Override
    @SuppressFBWarnings(value = "NM_METHOD_NAMING_CONVENTION", justification = "Part of other plugin API")
    public boolean IsApplicable(RoleType roleType) {
        switch (roleType) {
            case Project:
                return true;
            case Slave:
                return true;
            default:
                return false;
        }
    }
    
    public static OwnershipDescription getOwnership(RoleType type, AccessControlled item) {
        //TODO refactor the code to use OwnershipHelperLocator
        switch(type) {
            case Project:
                if (item instanceof Item) { 
                    IOwnershipHelper<AccessControlled> helper = OwnershipHelperLocator.locate(item);
                    return helper != null ? helper.getOwnershipDescription(item) : OwnershipDescription.DISABLED_DESCR;
                }
                break;
            case Slave:
                if (item instanceof Computer) {
                    Computer comp = (Computer)item;
                    Node node = comp.getNode();
                    if (node != null) {
                        return NodeOwnerHelper.Instance.getOwnershipDescription(node);
                    }
                }
                break;
            default:
                //do nothing => Ownership is disabled
        }
        
        // Fallback to the disabled Ownership description
        return OwnershipDescription.DISABLED_DESCR;
    }
    
    /**
     * Checks if a user has the permission defined for this macro.
     * @param user User
     * @param type Role type
     * @param item Item, for which permissions are being checked
     * @param macro Macro expression
     * @param acceptSecondaryOwners {@code true} if secondary owners should be considered
     * @return {@code true} if the macro provides a permission.
     *         Always {@code false} if the user is {@code null}.
     */
    public static boolean hasPermission(@CheckForNull User user, RoleType type, AccessControlled item,
                                        Macro macro, boolean acceptSecondaryOwners) {
        return user != null && getOwnership(type, item).isOwner(user, acceptSecondaryOwners);
    }
    
    /**
     * New API method with PermissionEntry.
     * Extracts SID from PermissionEntry and delegates to the old method.
     */
    @Override
    public boolean hasPermission(PermissionEntry entry, Permission p, RoleType type, AccessControlled item, Macro macro) {
        // Extract SID from PermissionEntry
        // Check for null entry first
        if (entry == null) {
            return false;
        }
        
        String sid = entry.getSid();
        // Check for null or empty SID - deny access in such cases
        if (sid == null || sid.trim().isEmpty()) {
            return false;
        }
        
        return hasPermission(sid, p, type, item, macro);
    }
    
    /**
     * Legacy method signature - kept for backward compatibility.
     * Subclasses should override this method.
     */
    public boolean hasPermission(String sid, Permission p, RoleType type, AccessControlled item, Macro macro) {
        // Check for null or empty SID - deny access in such cases
        if (sid == null) {
            return false;
        }
        if (sid.trim().isEmpty()) {
            return false;
        }
        
        User user = User.getById(sid, false);
        return hasPermission(user, type, item, macro, false);
    }
}
