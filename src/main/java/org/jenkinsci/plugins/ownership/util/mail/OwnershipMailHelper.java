/*
 * The MIT License
 *
 * Copyright 2015 Oleg Nenashev <o.v.nenashev@gmail.com>.
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

package org.jenkinsci.plugins.ownership.util.mail;

import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.util.UserStringFormatter;
import hudson.Util;
import hudson.model.User;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;

/**
 *
 * @author Oleg Nenashev
 */
public class OwnershipMailHelper {
   
    private static enum Mode { ContactOwners, ContactAdmins };

    /**
     * Generate a mailto URL, which allows to contact owners.
     * This URL should define default subject and body.
     * @param <TObjectType> Item type
     * @param item Item with ownership description's
     * @param helper Variables resolution helper
     * @return MailTo URL or null if it is not available
     * @since 0.6
     */
    public static <TObjectType> String getContactOwnersMailToURL (TObjectType item, 
            IOwnershipHelper<TObjectType> helper) {
        return getMailToURL(item, helper, Mode.ContactOwners);
    }
    
    public static <TObjectType> String getContactAdminsMailToURL (TObjectType item, 
            IOwnershipHelper<TObjectType> helper) {
        return getMailToURL(item, helper, Mode.ContactAdmins);
    }
    
    private static <TObjectType> String getMailToURL (TObjectType item, 
            IOwnershipHelper<TObjectType> helper, Mode mode) {
    
        final Jenkins instance = Jenkins.getInstance();
        if (instance == null) {
            // Cannot construct e-mail if Jenkins has not been initialized
            return null;
        }
        final OwnershipPlugin plugin = instance.getPlugin(OwnershipPlugin.class);
        if (plugin == null) {
            // Plugin is not initialized
            assert false : "Ownership plugin has not been loaded yet";
            return null;
        }
        final MailOptions mailOptions = plugin.getConfiguration().getMailOptions();
        final OwnershipDescription ownershipDescription = helper.getOwnershipDescription(item);
        
        // Handle enablers
        if (!ownershipDescription.isOwnershipEnabled()) {
            return null;
        }
        switch (mode) {
            case ContactOwners:
                if (mailOptions.isContactOwnersLinkDisabled()) {
                    return null;
                }
                break;
            case ContactAdmins:
                if (mailOptions.isContactAdminsLinkDisabled()) {
                    return null;
                }
                break;
            default:
                // Do nothing
        }

        // Prepare the data        
        final List<String> to = new LinkedList<String>();
        final List<String> cc = new LinkedList<String>();
        final Map<String, String> envVars = getSubstitutionVars(instance,item,helper);
        
        // to - job owner
        if (ownershipDescription.hasPrimaryOwner()) {
            String email = UserStringFormatter.formatEmail(ownershipDescription.getPrimaryOwnerId());
            if (email != null) {
                switch (mode) {
                   case ContactAdmins:
                       cc.add(email);
                       break;
                   default:
                       to.add(email);
                }
            }
        }

        // cc - job co-owners
        final Set<String> coOwners = ownershipDescription.getCoownersIds();
        if (!coOwners.isEmpty()) {
            for (String coOwnerId : coOwners) {
                String email = UserStringFormatter.formatEmail(coOwnerId);
                if (email != null) {
                    cc.add(email);
                }
            }
        }

        // Prepare subject and body using formatters
        final String body, subject;       
        switch (mode) {
            case ContactOwners:
                subject = resolveVariablesFor(mailOptions.getContactOwnersSubjectTemplate(), envVars); 
                body = resolveVariablesFor(mailOptions.getContactOwnersBodyTemplate(), envVars);
                break;
            case ContactAdmins:
                final String adminEmail = mailOptions.getAdminsContactEmail();
                to.add(adminEmail);
                subject = resolveVariablesFor(mailOptions.getContactAdminsSubjectTemplate(), envVars); 
                body = resolveVariablesFor(mailOptions.getContactAdminsBodyTemplate(), envVars);
                break;
            default:
                assert false : "Unsupported mode " + mode;
                throw new IllegalStateException("Mode "+mode+" is unsupported");
        }
        
        MailFormatter formatter = new MailFormatter(mailOptions.getEmailListSeparator());
        try {         
            final String formattedURL = formatter.createMailToString(
                to, cc, null, subject, body);
            return formattedURL;
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("Unsupported encoding: "+formatter.getEncoding(), ex);
        }    
    }
    
    private static @Nonnull <TObjectType> Map<String,String> getSubstitutionVars(
            @Nonnull Jenkins jenkins,
            @Nonnull TObjectType item,
            @Nonnull IOwnershipHelper<TObjectType> helper) {
        Map<String,String> res = new TreeMap<String, String>();
        
        // User info
        final User user = User.current();
        if (user != null && user != User.getUnknown()) {
            res.put("USER_ID", user.getId());
            res.put("USER_FULL_NAME", user.getFullName());
        }
        
        // Item info
        res.put("ITEM_TYPE_NAME", helper.getItemTypeName(item));
        res.put("ITEM_DISPLAY_NAME", helper.getItemDisplayName(item));
        res.put("ITEM_URL", jenkins.getRootUrl()+helper.getItemURL(item));
                
        return res;
    }
    
    private static @Nonnull String resolveVariablesFor
        (@Nonnull String inputString, @Nonnull Map<String,String> vars) {
        return Util.replaceMacro(inputString, vars);
    }
}
