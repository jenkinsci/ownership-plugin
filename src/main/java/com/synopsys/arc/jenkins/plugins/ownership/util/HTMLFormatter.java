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

import hudson.Functions;
import hudson.model.User;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import jenkins.model.Jenkins;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Provides additional formatters for jelly layout.
 * @author Oleg Nenashev
 * @since 0.2
 */
@Restricted(NoExternalUse.class)
public class HTMLFormatter {
    
    /**
     * Formats user's e-mail link.
     * @param userId id of the user
     * @return User e-mail in the following format: &lt;user@doma.in&gt;
     */
    public static @CheckForNull String formatEmailURI(@Nonnull String userId) {
        String email = Functions.escape(UserStringFormatter.formatEmail(userId));
        if (email != null) {
            return "<a href=\"mailto:"+email+"\">&lt;"+email+"&gt;</a>";
        } else {
            return null;
        }          
    }
    
    public static @Nonnull String formatShortUserURI(@Nonnull String userId) {
        return formatUserURI(userId, false);
    }
    
    public static @Nonnull String formatUserURI(@Nonnull String userId) {
        return formatUserURI(userId, true);
    }
    
    public static @Nonnull String formatUserURI(@Nonnull String userId, boolean useLongFormat) {
        User usr = User.get(userId, false, null);
        if (usr != null) {
            String userStr = useLongFormat 
                ? usr.getDisplayName()
                : UserStringFormatter.formatShort(usr.getId());
            return "<a href=\""+Jenkins.getActiveInstance().getRootUrl()+"user/"+Functions.escape(userId)+"\">"+Functions.escape(userStr)+"</a>";
        } else { // just return name w/o hyperlink
            return userId + " (unregistered)";
        }
    }
}
