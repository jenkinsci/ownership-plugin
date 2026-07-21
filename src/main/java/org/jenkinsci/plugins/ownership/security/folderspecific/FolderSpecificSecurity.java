/*
 * The MIT License
 *
 * Copyright 2026 Rustam Atai (atai)
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
package org.jenkinsci.plugins.ownership.security.folderspecific;

import com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.security.Permission;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Implements folder-specific property map.
 * Uses {@link AuthorizationMatrixProperty} from matrix-auth for folders
 * (distinct from the job {@link hudson.security.AuthorizationMatrixProperty}).
 * Inspired by work in ownership-plugin PR #70 by Dmitry Gruzd (@SofteqDG);
 * patterned after {@code ItemSpecificSecurity}.
 *
 * @author Rustam Atai
 */
public class FolderSpecificSecurity implements Describable<FolderSpecificSecurity>, Cloneable {

    private @Nonnull AuthorizationMatrixProperty permissionsMatrix;

    @DataBoundConstructor
    public FolderSpecificSecurity(@CheckForNull AuthorizationMatrixProperty permissionsMatrix) {
        this.permissionsMatrix = permissionsMatrix != null
                ? permissionsMatrix
                : new AuthorizationMatrixProperty(new HashMap<Permission, Set<String>>());
    }

    @Override
    public FolderSpecificDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Nonnull
    public AuthorizationMatrixProperty getPermissionsMatrix() {
        return permissionsMatrix;
    }

    /**
     * Checks whether the stored matrix grants {@code sid} the given permission.
     * Uses matrix-auth 3.x typed entries (USER/GROUP/EITHER), not the deprecated
     * two-argument {@code hasPermission(String, Permission)}.
     */
    public boolean grants(@Nonnull String sid, @Nonnull Permission p) {
        return permissionsMatrix.hasPermission(sid, p, true) || permissionsMatrix.hasPermission(sid, p, false);
    }

    @Override
    public FolderSpecificSecurity clone() {
        FolderSpecificSecurity newItem;
        try {
            newItem = (FolderSpecificSecurity) super.clone();
            Map<Permission, Set<PermissionEntry>> permissionEntries =
                    this.permissionsMatrix.getGrantedPermissionEntries();
            Map<Permission, Set<String>> permissions = new HashMap<>();
            for (Map.Entry<Permission, Set<PermissionEntry>> entry : permissionEntries.entrySet()) {
                Set<String> sids = new HashSet<>();
                for (PermissionEntry permEntry : entry.getValue()) {
                    sids.add(permEntry.getSid());
                }
                permissions.put(entry.getKey(), sids);
            }
            newItem.permissionsMatrix = new AuthorizationMatrixProperty(permissions);
            return newItem;
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(FolderSpecificSecurity.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static final FolderSpecificDescriptor DESCRIPTOR = new FolderSpecificDescriptor();

    @Extension(optional = true)
    public static class FolderSpecificDescriptor extends Descriptor<FolderSpecificSecurity> {
        @Override
        public String getDisplayName() {
            return "Folder-specific security";
        }

        @Override
        public FolderSpecificSecurity newInstance(StaplerRequest2 req, JSONObject formData) throws FormException {
            AuthorizationMatrixProperty prop = null;
            if (formData.containsKey("permissionsMatrix")) {
                Descriptor<?> d = Jenkins.get().getDescriptor(AuthorizationMatrixProperty.class);
                if (d != null) {
                    prop = (AuthorizationMatrixProperty)
                            d.newInstance(req, formData.getJSONObject("permissionsMatrix"));
                }
            }
            return new FolderSpecificSecurity(prop);
        }
    }
}
