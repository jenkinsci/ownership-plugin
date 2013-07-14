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
package com.synopsys.arc.jenkins.plugins.ownership.util;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import hudson.model.User;

/**
 * Helper for User string formatting.
 * @author Oleg Nenashev <nenashev@synopsys.com>
 */
public class UserStringFormatter {
    
    public static final String UNKNOWN_USER_STRING="N/A";
    
    /**
     * Formats string to be displayed.
     * @param user User
     * @return Formatted string
     */
    public static String format(User user)
    {
        return user !=null ? user.getFullName()+" ("+user.getId()+")" : UNKNOWN_USER_STRING;
    }
    
    public static String format(String userId) {
        return format(User.get(userId, false, null));
    }
    
    public static String formatShort(String userId) {
        return (userId != null && !userId.isEmpty()) ? userId : UNKNOWN_USER_STRING;
    } 
        
    /**
     * Formats e-mail from a user Id.
     * @param userId
     * @return e-mail
     * @since 0.2
     */
    public static String formatEmail(String userId) {
        return formatEmail(User.get(userId, false, null));
    }
    
    public static String formatEmail(User user) {
        OwnershipPlugin plugin = OwnershipPlugin.Instance();
        if (user == null || plugin == null)
        {
            return null;
        }          
        //FIXME: replace by E-mail resolver (https://issues.jenkins-ci.org/browse/JENKINS-18745)
        return user.getId() + plugin.getEmailSuffix();
    }
}
