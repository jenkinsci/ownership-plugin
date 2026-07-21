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
package com.synopsys.arc.jenkins.plugins.ownership.util.userFilters;

import hudson.model.User;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import javax.annotation.Nonnull;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.userdetails.UsernameNotFoundException;

/**
 * Filters user according to access rights to specified item.
 * @author Oleg Nenashev
 * @since 0.1
 */
public class AccessRightsFilter implements IUserFilter {

    private final AccessControlled item;
    private final Permission permission;

    /**
     * Primary constructor.
     * @param item Controlled item
     * @param permission Permission to be checked
     */
    public AccessRightsFilter(@Nonnull AccessControlled item, @Nonnull Permission permission) {
        this.item = item;
        this.permission = permission;
    }

    @Override
    public boolean filter(@Nonnull User user) {
        boolean permissionCheckResult = false;

        // Impersonate to check the permission
        SecurityContext initialContext = null;
        try {
            Authentication auth = user.impersonate();
            initialContext = hudson.security.ACL.impersonate(auth);
            permissionCheckResult = item.hasPermission(permission);
        } catch (UsernameNotFoundException ex) {
            return false; // Ignore non-existent users like "unknown"
        } finally {
            if (initialContext != null) {
                SecurityContextHolder.setContext(initialContext);
            }
        }

        return permissionCheckResult;
    }
}
