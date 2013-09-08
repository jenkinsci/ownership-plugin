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

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;

/**
 * Provides handlers for ownership description.
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 * @since 0.4
 */
//TODO: refactoring
public class OwnershipDescriptionHelper {
    private OwnershipDescriptionHelper() {}
    
    public static String getOwnerID(OwnershipDescription descr) {
        return descr.getPrimaryOwnerId();
    }
    
    public static String getOwnerEmail(OwnershipDescription descr) {
        String ownerEmail = UserStringFormatter.formatEmail(descr.getPrimaryOwnerId());
        return ownerEmail != null ? ownerEmail : "";
    }
    
    /**
     * Gets comma-separated list of co-owners
     * @param descr
     * @return 
     */
    public static String getCoOwnerIDs(OwnershipDescription descr) {
        String coowners=getOwnerID(descr);
        for (String userId : descr.getCoownersIds()) {
            if (!coowners.isEmpty()) {
                coowners+=",";
            }
            coowners += userId;      
        }
        return coowners;
    }
    
    public static String getCoOwnerEmails(OwnershipDescription descr) {
        String coownerEmails=getOwnerEmail(descr);
        for (String userId : descr.getCoownersIds()) {          
            String coownerEmail = UserStringFormatter.formatEmail(userId);
            if (coownerEmail != null) {
                if (!coownerEmails.isEmpty()) {
                    coownerEmails+=",";
                }
                coownerEmails+=coownerEmail;
            }       
        }
        return coownerEmails;
    }
}
