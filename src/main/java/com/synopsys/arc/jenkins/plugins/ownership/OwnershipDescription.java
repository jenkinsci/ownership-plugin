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
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Contains description of item's ownership. 
 * @author Oleg Nenashev <nenashev@synopsys.com>
 * @since 0.0.3
 */
public class OwnershipDescription {
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
     * @param primaryOwnerId userId of primary owner
     * @deprecated Use constructor with co-owners specification
     */
    public OwnershipDescription(boolean ownershipEnabled, String primaryOwnerId) {
        this.ownershipEnabled = ownershipEnabled;
        this.primaryOwnerId = primaryOwnerId;
        this.coownersIds = new TreeSet<String>();
    }

    /**
     * Constructor.
     * Class is being used as DataBound in "OwnerNodeProperty".
     * @param primaryOwnerId userId of primary owner 
     * @param coownersIds userIds of secondary owners
     */
    public OwnershipDescription(boolean ownershipEnabled, String primaryOwnerId, Collection<String> coownersIds) {
        this.ownershipEnabled = ownershipEnabled;
        this.primaryOwnerId = primaryOwnerId;
        this.coownersIds = new TreeSet<String>(coownersIds);
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
     * @return userId of primary owner 
     */
    public String getPrimaryOwnerId() {
        return ownershipEnabled ? primaryOwnerId :  User.getUnknown().getId();
    }
    
    /**
     * Get primary owner.
     * @return Primary owner's user.
     */
    public User getPrimaryOwner() {
        return User.get(primaryOwnerId, false, null);
    }

    /**
     * Gets list of coowners.
     * @return Collection of co-owners
     */
    public Set<String> getCoownersIds() {
        return coownersIds;
    }
    
    public static OwnershipDescription Parse(JSONObject formData, String blockName)
            throws Descriptor.FormException
    {
        Object blockObject = formData.get( blockName );
        boolean ownershipIsEnabled = false;
        String primaryOwner = null;
        Set<String> coOwnersSet = null;
        
        if( blockObject != null ) {
            ownershipIsEnabled = true;          
            JSONObject debugJSON = (JSONObject)blockObject;

            // Read primary owner
            primaryOwner = debugJSON.getString( "primaryOwner" );
            
            // Read coowners
            coOwnersSet = new TreeSet<String>();
            if (debugJSON.has("coOwners")) {
                JSONObject coOwners = debugJSON.optJSONObject("coOwners");
                if (coOwners == null) {         
                    for (Object obj : debugJSON.getJSONArray("coOwners")) {
                       addUser(coOwnersSet, (JSONObject)obj);
                    }
                } else {
                    addUser(coOwnersSet, coOwners);
                }
            }
        }
        
        return new OwnershipDescription(ownershipIsEnabled, primaryOwner, coOwnersSet);
    }
    
    private static void addUser(Set<String> target, JSONObject userObj) throws Descriptor.FormException {
        String userId = Util.fixEmptyAndTrim(((JSONObject)userObj).getString("coOwner"));
        
        //TODO: validate user string
        
        if (userId != null) {
            target.add(userId);
        }
    }
            
    
    /**
     * Check if User is owner.
     * @param user User to be checked
     * @param acceptCoowners Check if user belongs to coowners
     * @return true if User belongs to primary owners (and/or coowners)
     */
    public boolean isOwner(User user, boolean acceptCoowners)
    {
        if (isPrimaryOwner(user)) {
            return true;
        }
        
        return acceptCoowners ? coownersIds.contains(user.getId()) : false;
    }
    
    public boolean hasPrimaryOwner()
    {
        return ownershipEnabled && getPrimaryOwner() != null;
    }
    
    public boolean isPrimaryOwner(User user)
    {
        return user != null && user == getPrimaryOwner();
    }
    
    /**
     * Check if ownership is enabled.
     * @param descr Ownership description (can be null)
     * @since 0.1
     */
    public static boolean isEnabled(OwnershipDescription descr) {
        return descr != null && descr.ownershipEnabled;
    }
    
    /**
     * 
     */
    static void check() {
        
    }
}
