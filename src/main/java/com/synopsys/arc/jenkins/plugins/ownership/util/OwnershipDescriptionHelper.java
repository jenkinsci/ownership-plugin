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
package com.synopsys.arc.jenkins.plugins.ownership.util;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

//TODO: Do we really need this helper class now?
//TODO co-owners have not been renamed to secondary owners, but it's an internal-only class
/**
 * Provides handlers for ownership description.
 * @author Oleg Nenashev
 * @since 0.4 - Public API class
 * @since TODO - Class API is restricted, use OwnershipDescription instead
 * @see OwnershipDescription
 */
@Restricted(NoExternalUse.class)
public class OwnershipDescriptionHelper {
    
    private OwnershipDescriptionHelper() {}
    
    /**
     * Gets ID of the primary owner.
     * @param descr Ownership description 
     * @return userId of the primary owner. The result will be "unknown" if the
     * user is not specified.
     * @deprecated Use {@link #getPrimaryOwnerId(com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription)}
     */
    @Nonnull 
    @Deprecated
    public static String getOwnerID(@Nonnull OwnershipDescription descr) {
        return getPrimaryOwnerId(descr);
    }
    
    /**
     * Gets id of the primary owner.
     * @param descr Ownership description 
     * @return userId of the primary owner. The result will be "unknown" if the
     * user is not specified.
     */
    @Nonnull
    public static String getPrimaryOwnerId(@Nonnull OwnershipDescription descr) {
        return descr.getPrimaryOwnerId();
    }
    
    /**
     * Gets owner's e-mail.
     * @param descr Ownership description
     * @return Owner's e-mail or empty string if it is not available
     * @deprecated Use {@link #getPrimaryOwnerEmail(com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription)}
     */
    @Nonnull
    @Deprecated
    public static String getOwnerEmail(@Nonnull OwnershipDescription descr) {
        return getPrimaryOwnerEmail(descr);
    }
    
    /**
     * Gets e-mail of the primary owner.
     * @param descr Ownership description
     * @return Owner's e-mail or empty string if it is not available
     * @since 0.9
     */
    @Nonnull
    public static String getPrimaryOwnerEmail(@Nonnull OwnershipDescription descr) {
        String ownerEmail = UserStringFormatter.formatEmail(descr.getPrimaryOwnerId());
        return ownerEmail != null ? ownerEmail : "";
    }
    
    /**
     * Gets a comma-separated list of co-owners.
     * @param descr Ownership description
     * @param includeOwner Include owner to the list
     * @return Set of co-owner user IDs
     * @since 0.9
     */
    @Nonnull
    public static Set<String> getSecondaryOwnerIds(@Nonnull OwnershipDescription descr, boolean includeOwner) {
        Set<String> res = new TreeSet<String>();
        if (includeOwner) {
            res.add(getPrimaryOwnerId(descr));
        }
        for (String userId : descr.getSecondaryOwnerIds()) {
            res.add(userId);      
        }
        return res;
    }
    
    /**
     * Gets a comma-separated list of co-owners.
     * Owner will be also considered as co-owner.
     * @param descr Ownership description
     * @return List of co-owner user IDs
     * @deprecated Use {@link #getAllOwnerIdsString(com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription)}
     */
    @Deprecated
    @Nonnull
    public static String getCoOwnerIDs(@Nonnull OwnershipDescription descr) {
        return getAllOwnerEmailsString(descr);
    }
    
    /**
     * Gets a comma-separated list of primary and secondary owners.
     * @param descr Ownership description
     * @return String containing comma-separated list of owner user IDs
     * @since 0.9
     */
    @Nonnull 
    public static String getAllOwnerIdsString(@Nonnull OwnershipDescription descr) {
        StringBuilder coowners= new StringBuilder();
        for (String userId : getSecondaryOwnerIds(descr, true)) {
            if (coowners.length() != 0) {
                coowners.append(",");
            }
            coowners.append(userId);      
        }
        return coowners.toString();
    }
    
    /**
     * Gets e-mails of secondary owners (including owner if required).
     * @param descr Ownership description
     * @param includeOwner Include owner to the list
     * @return Set of secondary owner emails
     * @since 0.9
     */
    public static Set<String> getSecondaryOwnerEmails(@Nonnull OwnershipDescription descr, boolean includeOwner) {
        Set<String> res = new TreeSet<String>();
        
        if (includeOwner) {
            res.add(getOwnerEmail(descr));
        }
        for (String userId : descr.getSecondaryOwnerIds()) {          
            String coownerEmail = UserStringFormatter.formatEmail(userId);
            if (coownerEmail != null) {
                res.add(coownerEmail);
            }       
        }
        return res;
    }
    
    /**
     * Gets e-mails of secondary owners (including primary owner).
     * @param descr Ownership description
     * @return Comma-separated list of secondary owner e-mails (may be empty)
     * @since 0.9
     */
    public static String getAllOwnerEmailsString(@Nonnull OwnershipDescription descr) {
        StringBuilder coownerEmails=new StringBuilder();
        for (String coownerEmail : getSecondaryOwnerEmails(descr, true)) {          
            if (coownerEmails.length() != 0) {
                coownerEmails.append(",");
            }
            coownerEmails.append(coownerEmail);    
        }
        return coownerEmails.toString();
    }
}
