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
package com.synopsys.arc.jenkins.plugins.ownership;

import com.synopsys.arc.jenkins.plugins.ownership.util.OwnershipDescriptionHelper;
import com.synopsys.arc.jenkins.plugins.ownership.nodes.OwnerNodeProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.User;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

/**
 * Contains description of item's ownership. 
 * This class is a main information entry for all ownership features.
 * @author Oleg Nenashev
 * @since 0.0.3
 */
public class OwnershipDescription implements Serializable {
    /**
     * Disabled description, which means that ownership is disabled
     */
    public static final OwnershipDescription DISABLED_DESCR = new OwnershipDescription(false, "");
    
    /**
     * Indicates if ownership is enabled
     */
    @Whitelisted
    boolean ownershipEnabled;
    
    /**
     * UserId of primary user
     */
    String primaryOwnerId;
    
    /**
     * Sids of the secondary owners (fka co-owners).
     * Sids can include users and groups.  
     */
    @Whitelisted
    Set<String> coownersIds;

    /**
     * Constructor.
     * @param ownershipEnabled indicates that the ownership is enabled
     * @param primaryOwnerId userId of primary owner
     * @deprecated Use constructor with secondary owners specification
     */
    public OwnershipDescription(boolean ownershipEnabled, @Nonnull String primaryOwnerId) {
        this(ownershipEnabled, primaryOwnerId, null);
    }

    /**
     * Constructor.
     * Class is being used as DataBound in {@link OwnerNodeProperty}.
     * @param ownershipEnabled Indicates that the ownership is enabled.
     * @param primaryOwnerId userId of primary owner. Use null if there is no owner 
     * @param secondaryOwnerIds userIds of secondary owners. Use {@code null} if there is no secondary owners.
     */
    public OwnershipDescription(boolean ownershipEnabled, @Nullable String primaryOwnerId, @Nullable Collection<String> secondaryOwnerIds) {
        this.ownershipEnabled = ownershipEnabled;
        this.primaryOwnerId = primaryOwnerId;
        this.coownersIds =  secondaryOwnerIds != null ? new TreeSet<String>(secondaryOwnerIds) : new TreeSet<String>();
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
        builder.append("primary owner=");
        builder.append(primaryOwnerId);
        if (!coownersIds.isEmpty()) {
            builder.append(" secondary owners:[");
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
    @Whitelisted
    public boolean isOwnershipEnabled() {
        return ownershipEnabled;
    }
   
    /**
     * Gets id of primary owner.
     * @return userId of the primary owner. The result will be "unknown" if the
     * user is not specified.
     */
    @Whitelisted
    public @Nonnull String getPrimaryOwnerId() {        
        return ownershipEnabled ? primaryOwnerId :  User.getUnknown().getId();
    }
    
    /**
     * Get primary owner.
     * @return Primary owner's user. The result may be null if someone deletes
     * a user.
     */
    @CheckForNull
    public User getPrimaryOwner() {
        return ownershipEnabled ? User.get(primaryOwnerId, false, null) : null;
    }

    /**
     * Gets list of secondary owners (fka co-owners).
     * @return Collection of secondary owners
     * @deprecated use {@link #getSecondaryOwnerIds()}
     */
    @Nonnull
    @Deprecated
    public Set<String> getCoownersIds() {
        return getSecondaryOwnerIds();
    }
    
    /**
     * Gets list of secondary owners.
     * @return Collection of secondary owners
     * @since 0.9
     */
    @Nonnull
    @Whitelisted
    public Set<String> getSecondaryOwnerIds() {
        return coownersIds;
    }
    
    /**
     * @deprecated Use {@link #parseJSON(net.sf.json.JSONObject)} instead.
     */
    @Nonnull
    @Deprecated
    @SuppressFBWarnings(value = "NM_METHOD_NAMING_CONVENTION", justification = "deprecated")
    public static OwnershipDescription Parse(JSONObject formData)
            throws Descriptor.FormException {
        return parseJSON(formData);
    }
    
    /**
     * Parse a JSON input to construct the ownership description.
     * @param formData Object with a data
     * @return Ownership Description 
     * @throws hudson.model.Descriptor.FormException Parsing error
     */
    @Nonnull
    public static OwnershipDescription parseJSON(JSONObject formData)
            throws Descriptor.FormException
    {
        // Read primary owner
        String primaryOwnerId = formData.getString( "primaryOwner" );

        // Read coowners
        Set<String> secondaryOwnerIds = new TreeSet<String>();
        if (formData.has("coOwners")) {
            JSONObject coOwners = formData.optJSONObject("coOwners");
            if (coOwners == null) {         
                for (Object obj : formData.getJSONArray("coOwners")) {
                   addUser(secondaryOwnerIds, (JSONObject)obj);
                }
            } else {
                addUser(secondaryOwnerIds, coOwners);
            }
        }   
        return new OwnershipDescription(true, primaryOwnerId, secondaryOwnerIds);
    }
    
    private static void addUser(Set<String> target, JSONObject userObj) throws Descriptor.FormException {
        String userId = Util.fixEmptyAndTrim(userObj.getString("coOwner"));
        
        //TODO: validate user string
        if (userId != null) {
            target.add(userId);
        }
    }
              
    /**
     * Check if User is an owner.
     * @param user User to be checked
     * @param includeSecondaryOwners Check if user belongs to secondary owners
     * @return {@code true} if the user belongs to primary owners (and/or secondary owners)
     */
    public boolean isOwner(User user, boolean includeSecondaryOwners) {
        if (user == null) {
            return false;
        }
        if (isPrimaryOwner(user)) {
            return true;
        }
        return includeSecondaryOwners ? coownersIds.contains(user.getId()) : false;
    }
    
    @Whitelisted
    public boolean hasPrimaryOwner() {
        return ownershipEnabled && getPrimaryOwner() != null;
    }
    /**
     * Checks if the specified user is a primary owner.
     * @param user User to be checked
     * @return {@code true} if the user is a primary owner
     */
    public boolean isPrimaryOwner(@CheckForNull User user) {
        return user != null && user == getPrimaryOwner();
    }
    
    /**
     * Gets ID of the primary owner.
     * @return userId of the primary owner. The result will be "unknown" if the
     * user is not specified.
     * @since 0.9
     * @deprecated Use {@link #getPrimaryOwnerId()}
     */
    @Deprecated
    public @Nonnull String getOwnerId() {
        return getPrimaryOwnerId();
    }
    
    /**
     * Gets primary owner's e-mail.
     * This method utilizes {@link OwnershipPlugin} global configuration to resolve emails.
     * @return Primary owner's e-mail or empty string if it is not available
     * @since 0.9
     * @deprecated Use {@link #getPrimaryOwnerEmail()} 
     */
    @Nonnull
    @Deprecated
    public String getOwnerEmail() {
        return getPrimaryOwnerEmail();
    }
    
    /**
     * Gets primary owner's e-mail.
     * This method utilizes {@link OwnershipPlugin} global configuration to resolve emails.
     * @return Primary owner's e-mail or empty string if it is not available
     * @since 0.9
     */
    @Nonnull
    @Whitelisted
    public String getPrimaryOwnerEmail() {
        return OwnershipDescriptionHelper.getOwnerEmail(this);
    }
    
    /**
     * Gets a comma-separated list of co-owners.
     * @return List of co-owner user IDs
     * @deprecated use {@link #getSecondaryOwnerIds()}
     */
    @Deprecated
    public @Nonnull Set<String> getCoOwnerIds() {
        return coownersIds;
    }
    
    /**
     * Gets e-mails of secondary owners.
     * This method utilizes {@link OwnershipPlugin} global configuration to resolve emails.
     * @return List of secondary owner e-mails (may be empty)
     * @deprecated use {@link #getSecondaryOwnerEmails()}
     */
    @Deprecated
    public @Nonnull Set<String> getCoOwnerEmails() {
        return OwnershipDescriptionHelper.getSecondaryOwnerEmails(this, false);
    }
    
    /**
     * Gets e-mails of secondary owners.
     * This method utilizes {@link OwnershipPlugin} global configuration to resolve emails.
     * @return List of secondary owner e-mails (may be empty)
     * @since 0.9
     */
    @Whitelisted
    public @Nonnull Set<String> getSecondaryOwnerEmails() {
        return OwnershipDescriptionHelper.getSecondaryOwnerEmails(this, false);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + (this.ownershipEnabled ? 1 : 0);
        hash = 41 * hash + (this.primaryOwnerId != null ? this.primaryOwnerId.hashCode() : 0);
        hash = 41 * hash + (this.coownersIds != null ? this.coownersIds.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OwnershipDescription other = (OwnershipDescription) obj;
        if (this.ownershipEnabled != other.ownershipEnabled) {
            return false;
        } else if (!this.ownershipEnabled) {
            // Treat disabled ownership configurations as equal ones
            return true;
        }
        
        if ((this.primaryOwnerId == null) ? (other.primaryOwnerId != null) : !this.primaryOwnerId.equals(other.primaryOwnerId)) {
            return false;
        }
        if (this.coownersIds != other.coownersIds && (this.coownersIds == null || !this.coownersIds.equals(other.coownersIds))) {
            return false;
        }
        return true;
    }
    
    /**
     * Check if ownership is enabled.
     * @param descr Ownership description (can be {@code null})
     * @return True if the ownership is enabled
     * @since 0.1
     */
    public static boolean isEnabled(OwnershipDescription descr) {
        return descr != null && descr.ownershipEnabled;
    }   
}
