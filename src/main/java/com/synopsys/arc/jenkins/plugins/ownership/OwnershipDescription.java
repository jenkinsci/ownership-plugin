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

import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.User;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.json.JSONObject;

/**
 * Contains description of item's ownership. 
 * This class is a main information entry for all ownership features.
 * @author Oleg Nenashev <nenashev@synopsys.com>
 * @since 0.0.3
 */
public class OwnershipDescription implements Serializable {
    /**
     * Disabled description, which means that ownership is disabled
     */
    public static final OwnershipDescription DISABLED_DESCR = new OwnershipDescription(false, null);
    
    /**
     * Indicates if ownership is enabled
     */
    boolean ownershipEnabled;
    
    /**
     * UserId of primary user
     */
    String primaryOwnerId;
    
    /**
     * Sids of the co-Owners.
     * Sids can include users and groups.  
     */
    Set<String> coownersIds;

    /**
     * Constructor.
     * @param ownershipEnabled indicates that the ownership is enabled
     * @param primaryOwnerId userId of primary owner
     * @deprecated Use constructor with co-owners specification
     */
    public OwnershipDescription(boolean ownershipEnabled, @Nonnull String primaryOwnerId) {
        this(ownershipEnabled, primaryOwnerId, null);
    }

    /**
     * Constructor.
     * Class is being used as DataBound in {@link OwnerNodeProperty}.
     * @param ownershipEnabled Indicates that the ownership is enabled.
     * @param primaryOwnerId userId of primary owner 
     * @param coownersIds userIds of secondary owners. Use null if there is no owners.
     */
    public OwnershipDescription(boolean ownershipEnabled, @Nonnull String primaryOwnerId, @Nullable Collection<String> coownersIds) {
        this.ownershipEnabled = ownershipEnabled;
        this.primaryOwnerId = primaryOwnerId;
        this.coownersIds =  coownersIds != null ? new TreeSet<String>(coownersIds) : new TreeSet<String>();
    }
    
    public void assign(@Nonnull OwnershipDescription descr) {
        this.ownershipEnabled = descr.ownershipEnabled;
        this.primaryOwnerId = descr.primaryOwnerId;
        this.coownersIds = descr.coownersIds;
    }

    @Override
    public String toString() {
        if (!ownershipEnabled) {
            return "ownership:disabled";
        }
           
        StringBuilder builder = new StringBuilder();
        builder.append("owner=");
        builder.append(primaryOwnerId);
        if (!coownersIds.isEmpty()) {
            builder.append(" co-owners:[");
            for (String coownerId : coownersIds) {
                builder.append(coownerId);
                builder.append(' ');
            }
            builder.append(']');
        }
        return builder.toString();
    }
    
    /**
     * Check if ownership is enabled.
     * @return true if ownership is enabled
     */
    public boolean isOwnershipEnabled() {
        return ownershipEnabled;
    }
   
    /**
     * Gets id of primary owner.
     * @return userId of the primary owner. The result will be "unknown" if the
     * user is not specified.
     */
    @Nonnull
    public String getPrimaryOwnerId() {        
        return ownershipEnabled ? primaryOwnerId :  User.getUnknown().getId();
    }
    
    /**
     * Get primary owner.
     * @return Primary owner's user. The result may be null if someone deletes
     * a user.
     */
    @CheckForNull
    public User getPrimaryOwner() {
        return User.get(primaryOwnerId, false, null);
    }

    /**
     * Gets list of co-owners.
     * @return Collection of co-owners
     */
    @Nonnull
    public Set<String> getCoownersIds() {
        return coownersIds;
    }
    
    /**
     * @deprecated Use {@link #parseJSON(net.sf.json.JSONObject)} instead.
     */
    @Nonnull
    public static OwnershipDescription Parse(JSONObject formData)
            throws Descriptor.FormException
    {
        return parseJSON(formData);
    }
    
    /**
     * Parse a JSON input to construct {@link OwershipDescription}.
     * @param formData Object with a data
     * @return OwnershipDescription 
     * @throws hudson.model.Descriptor.FormException Parsing error
     */
    @Nonnull
    public static OwnershipDescription parseJSON(JSONObject formData)
            throws Descriptor.FormException
    {
        // Read primary owner
        String primaryOwner = formData.getString( "primaryOwner" );

        // Read coowners
        Set<String> coOwnersSet = new TreeSet<String>();
        if (formData.has("coOwners")) {
            JSONObject coOwners = formData.optJSONObject("coOwners");
            if (coOwners == null) {         
                for (Object obj : formData.getJSONArray("coOwners")) {
                   addUser(coOwnersSet, (JSONObject)obj);
                }
            } else {
                addUser(coOwnersSet, coOwners);
            }
        }   
        return new OwnershipDescription(true, primaryOwner, coOwnersSet);
    }
    
    private static void addUser(Set<String> target, JSONObject userObj) throws Descriptor.FormException {
        String userId = Util.fixEmptyAndTrim(((JSONObject)userObj).getString("coOwner"));
        
        //TODO: validate user string
        if (userId != null) {
            target.add(userId);
        }
    }
              
    /**
     * Check if User is an owner.
     * @param user User to be checked
     * @param acceptCoowners Check if user belongs to co-owners
     * @return true if User belongs to primary owners (and/or co-owners)
     */
    public boolean isOwner(User user, boolean acceptCoowners) {
        if (user == null) {
            return false;
        }
        if (isPrimaryOwner(user)) {
            return true;
        }
        return acceptCoowners ? coownersIds.contains(user.getId()) : false;
    }
    
    public boolean hasPrimaryOwner() {
        return ownershipEnabled && getPrimaryOwner() != null;
    }
    
    public boolean isPrimaryOwner(User user) {
        return user != null && user == getPrimaryOwner();
    }
    
    /**
     * Check if ownership is enabled.
     * @param descr Ownership description (can be null)
     * @return True if the ownership is enabled
     * @since 0.1
     */
    public static boolean isEnabled(OwnershipDescription descr) {
        return descr != null && descr.ownershipEnabled;
    }   
}
