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
package com.synopsys.arc.jenkins.plugins.ownership.security.itemspecific;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.security.AuthorizationMatrixProperty;
import hudson.security.Permission;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Implements item-specific property map.
 * This class relies on {@link AuthorizationMatrixProperty} from Jenkins core.
 * @author Oleg Nenashev
 * @since 0.3
 */
public class ItemSpecificSecurity implements Describable<ItemSpecificSecurity>, Cloneable {
    
    private @Nonnull AuthorizationMatrixProperty permissionsMatrix;
    
    @DataBoundConstructor
    public ItemSpecificSecurity(@CheckForNull AuthorizationMatrixProperty permissionsMatrix) {
        this.permissionsMatrix = permissionsMatrix != null 
                ? permissionsMatrix
                : new AuthorizationMatrixProperty(new TreeMap<Permission, Set<String>>());
    }

    @Override
    public ItemSpecificDescriptor getDescriptor() {
        return DESCRIPTOR; 
    }

    @Nonnull
    public AuthorizationMatrixProperty getPermissionsMatrix() {
         return permissionsMatrix;
    }

    @Override
    public ItemSpecificSecurity clone() {
        ItemSpecificSecurity newItem;
        try {
            newItem = (ItemSpecificSecurity)super.clone();
            newItem.permissionsMatrix = new AuthorizationMatrixProperty(this.permissionsMatrix.getGrantedPermissions());
            return newItem;
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(ItemSpecificSecurity.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }     
    }
        
    public static final ItemSpecificDescriptor DESCRIPTOR = new ItemSpecificDescriptor();
    
    @Extension
    public static class ItemSpecificDescriptor extends Descriptor<ItemSpecificSecurity> {
        @Override
        public String getDisplayName() {
            return "Item-specific security";
        }

        @Override
        public ItemSpecificSecurity newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            AuthorizationMatrixProperty prop = null;
            if (formData.containsKey("permissionsMatrix")) {
                Descriptor<?> d= Jenkins.getActiveInstance().getDescriptor(AuthorizationMatrixProperty.class);
                prop = (AuthorizationMatrixProperty)d.newInstance(req, formData.getJSONObject("permissionsMatrix"));
            }
            return new ItemSpecificSecurity(prop);
        }
    }
}
