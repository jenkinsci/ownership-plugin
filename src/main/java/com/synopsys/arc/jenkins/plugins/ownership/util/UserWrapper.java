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
import hudson.model.User;

/**
 * Implements wrapper, which allows to implement "non-existent" users.
 * 
 * @author Oleg Nenashev <nenashev@synopsys.com>
 */
public class UserWrapper {
    boolean isUser;
    User user;
    String macro;
    public static final String USER_MACRO_PREFIX = "@";
    
    public UserWrapper(User user)
    {
        this.isUser = true;
        this.user = user;
    }
    
    /**
     * Creates Wrapper from user id or wrapper.
     * @todo Just a hack for "@ME" macro. Need to extend functionality in the future. 
     * @param userMacro 
     */
    public UserWrapper(String userMacro)
    {
        if (userMacro.startsWith(USER_MACRO_PREFIX))
        {
            this.isUser = false;
            this.macro = userMacro;
        }
        else
        {
            this.isUser = true;
            this.user = User.get(userMacro, false, null);
            //   throw new UnsupportedOperationException("User macro must start with prefix '"+USER_MACRO_PREFIX+"'");
        }
        
    }

    public boolean IsUser()
    {
        return isUser;
    }
    
    /**
     * Gets id of the user (calls User.getId() or returns macro).
     * @return 
     */
    public String getId()
    {
        return isUser ? user.getId() : macro;
    }
    
    @Override
    public String toString() {
        return isUser ? UserStringFormatter.format(user) : macro;
    }
    
    public boolean meetsMacro(String userId) {           
        // Handle macroses and get effective user's id
        String comparedId;
        if (isUser)
        {
            if (user == null) return false;
            comparedId = user.getId();
        }
        else
        {
            User current = User.current();
            if (current == null) return false;
            comparedId = current.getId();
        }        

        // Check      
        return comparedId.equals(userId);
    }
    
    
}
