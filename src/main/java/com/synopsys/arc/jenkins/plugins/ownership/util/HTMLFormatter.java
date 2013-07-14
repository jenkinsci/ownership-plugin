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

import hudson.model.User;
import jenkins.model.Jenkins;

/**
 * Provides additional formatters for jelly layout.
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 * @since 0.2
 */
public class HTMLFormatter {
    /**
     * Formats user's e-mail link.
     * @param userId id of the user
     * @return 
     */
    public static String formatEmailURI(String userId) {
        String email = UserStringFormatter.formatEmail(userId);
        if (userId != null && email != null) {
            return "<a href=\"mailto://"+email+"\">"+email+"</a>";
        } else {
            return null;
        }          
    }
    
    public static String formatShortUserURI(String userId) {
        return formatUserURI(userId, false);
    }
    
    public static String formatUserURI(String userId) {
        return formatUserURI(userId, true);
    }
    
    public static String formatUserURI(String userId, boolean useLongFormat) {
        User usr = User.get(userId, false, null);
        if (usr != null) {
            String userStr = useLongFormat 
                ? UserStringFormatter.format(usr) 
                : UserStringFormatter.formatShort(usr.getId());
            return "<a href=\""+Jenkins.getInstance().getRootUrl()+"user/"+userId+"\">"+userStr+"</a>";
        } else { // just return name w/o hyperlink
            return userId;
        }
    }
}
