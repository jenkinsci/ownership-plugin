/*
 * The MIT License
 *
 * Copyright (c) 2016 Oleg Nenashev.
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
package org.jenkinsci.plugins.ownership.security.rolestrategy;

import com.michelin.cio.hudson.plugins.rolestrategy.AuthorizationType;
import com.michelin.cio.hudson.plugins.rolestrategy.PermissionEntry;
import com.michelin.cio.hudson.plugins.rolestrategy.Role;
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy;
import com.michelin.cio.hudson.plugins.rolestrategy.RoleMap;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType;
import hudson.model.Computer;
import hudson.model.Item;
import hudson.model.Run;
import hudson.security.Permission;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;

/**
 * Provides functions for Ownership-based security setup.
 * @author Oleg Nenashev
 */
public class OwnershipBasedSecurityTestHelper {
    
    public static void setup(@Nonnull Jenkins jenkins) throws AssertionError, IOException {
        
        Map<String,RoleMap> grantedRoles = new HashMap<String, RoleMap>();
        grantedRoles.put(RoleType.Project.getStringType(), getProjectRoleMap());
        grantedRoles.put(RoleType.Slave.getStringType(), getComputerRoleMap());
        grantedRoles.put(RoleType.Global.getStringType(), getGlobalAdminAndAnonymousRoles());
        
        RoleBasedAuthorizationStrategy strategy = new RoleBasedAuthorizationStrategy(grantedRoles);
        jenkins.setAuthorizationStrategy(strategy);
        jenkins.save();
    }
    
    private static RoleMap getGlobalAdminAndAnonymousRoles() {
        Set<Permission> adminPermissions = new HashSet<Permission>();
        adminPermissions.add(Jenkins.ADMINISTER);
        Role adminRole = createRole("administrator", ".*", adminPermissions);
        
        Set<Permission> anonymousPermissions = new HashSet<Permission>();
        anonymousPermissions.add(Jenkins.READ);
        anonymousPermissions.add(Item.READ);
        anonymousPermissions.add(Item.DISCOVER);
        Role anonymousRole = createRole("anonymous", ".*", anonymousPermissions);
        
        final SortedMap<Role,Set<PermissionEntry>> grantedRoles = new TreeMap<Role, Set<PermissionEntry>>();
        grantedRoles.put(adminRole, singleSid("admin"));
        grantedRoles.put(anonymousRole, singleSid("anonymous"));
        
        return createRoleMap(grantedRoles);
    }
    
    private static RoleMap getProjectRoleMap() {
        Set<Permission> ownerPermissions = new HashSet<Permission>();
        ownerPermissions.add(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP);
        ownerPermissions.addAll(Item.PERMISSIONS.getPermissions());
        ownerPermissions.addAll(Run.PERMISSIONS.getPermissions());
        Role ownerRole = createRole("@OwnerNoSid", ".*", ownerPermissions);
        
        Set<Permission> coownerPermissions = new HashSet<Permission>();
        coownerPermissions.addAll(Item.PERMISSIONS.getPermissions());
        coownerPermissions.addAll(Run.PERMISSIONS.getPermissions());
        coownerPermissions.remove(Item.DELETE);
        coownerPermissions.remove(Run.DELETE);
        Role coOwnerRole = createRole("@CoOwnerNoSid", ".*", coownerPermissions);
        
        return createRoleMapForSid("authenticated", ownerRole, coOwnerRole);
    }
    
    private static RoleMap getComputerRoleMap() {
        Set<Permission> ownerPermissions = new HashSet<Permission>();
        ownerPermissions.add(OwnershipPlugin.MANAGE_SLAVES_OWNERSHIP);
        ownerPermissions.addAll(Computer.PERMISSIONS.getPermissions());
        Role ownerRole = createRole("@OwnerNoSid", ".*", ownerPermissions);
        
        Set<Permission> coownerPermissions = new HashSet<Permission>();
        coownerPermissions.addAll(Computer.PERMISSIONS.getPermissions());
        coownerPermissions.remove(Computer.DELETE);
        coownerPermissions.remove(Computer.CONFIGURE);
        Role coOwnerRole = createRole("@CoOwnerNoSid", ".*", coownerPermissions);
        
        return createRoleMapForSid("authenticated", ownerRole, coOwnerRole);
    }
    
    private static Role createRole(String name, String pattern, Permission ... permissions) {
        Set<Permission> permSet = new HashSet<Permission>();
        for (Permission p : permissions) {
            permSet.add(p);
        }
        return createRole(name, pattern, permSet);
    }
    
    private static RoleMap createRoleMapForSid(String sid, Role ... roles) {
        final SortedMap<Role,Set<PermissionEntry>> grantedRoles = new TreeMap<Role, Set<PermissionEntry>>();
        for (Role role : roles) {
            grantedRoles.put(role, singleSid(sid));
        }
        return createRoleMap(grantedRoles);
    }
    
    private static Set<PermissionEntry> singleSid(String sid) {
        if (sid == null) {
            throw new IllegalArgumentException("SID cannot be null");
        }
        final Set<PermissionEntry> sids = new TreeSet<PermissionEntry>();
        // Special SIDs like "authenticated" and "anonymous" should use EITHER type
        // Regular user/group SIDs use user() or group()
        if ("authenticated".equals(sid) || "anonymous".equals(sid)) {
            sids.add(new PermissionEntry(AuthorizationType.EITHER, sid));
        } else {
            // For regular users, use user() method
            sids.add(PermissionEntry.user(sid));
        }
        return sids;
    }
    
    // TODO: Methods below should be burned with fire when RoleStrategy gets better API
    private static Role createRole(String name, String pattern, Set<Permission> permissions) {
        try {
            Constructor<Role> constructor = locateConstructor(Role.class, String.class, String.class, Set.class);
            try {
                constructor.setAccessible(true);
                return constructor.newInstance(name, pattern, permissions);
            } finally {
                constructor.setAccessible(false);
            }
        } catch (Exception ex) {
            throw new AssertionError("Cannot create role", ex);
        }
    }
    
    @Nonnull
    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> locateConstructor(Class<T> itemClass, Class<?> ... parameters) {
        Constructor<?>[] constructors = itemClass.getDeclaredConstructors();
        for (Constructor<?> c : constructors) {
            Class<?>[] parameterTypes = c.getParameterTypes();
            if (parameterTypes.length != parameters.length) {
                continue;
            }
            
            boolean matches = true;
            for (int i = 0; i < parameters.length; ++i) {
                if (!parameterTypes[i].isAssignableFrom(parameters[i])) {
                    matches = false;
                    break;
                }
            }
            
            if (matches) {
                return (Constructor<T>)c;
            }
        }
        
        throw new IllegalStateException("Cannot locate a constructor for " + itemClass);
    }
    
    private static RoleMap createRoleMap(SortedMap<Role,Set<PermissionEntry>> grantedRoles) {
        try {
            Constructor<RoleMap> constructor = locateConstructor(RoleMap.class, SortedMap.class);
            try {
                constructor.setAccessible(true);
                return constructor.newInstance(grantedRoles);
            } finally {
                constructor.setAccessible(false);
            }
        } catch (Exception ex) {
            throw new AssertionError("Cannot create role map", ex);
        }
    }
    
}
