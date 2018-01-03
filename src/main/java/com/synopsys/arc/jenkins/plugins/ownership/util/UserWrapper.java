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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.User;
import javax.annotation.Nonnull;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Implements a wrapper, which allows to implement the "non-existent" user macro.
 * @author Oleg Nenashev
 * @since 0.1
 */
@Restricted(NoExternalUse.class)
public class UserWrapper {

    boolean isUser;
    User user;
    String macro;
    public static final String USER_MACRO_PREFIX = "@";

    public UserWrapper(@Nonnull User user) {
        this.isUser = true;
        this.user = user;
    }

    /**
     * Creates Wrapper from user id or wrapper.
     *
     * TODO: Just a hack for "@ME" macro. Need to extend functionality in the
     * future.
     * @param userMacro
     */
    public UserWrapper(@Nonnull String userMacro) {
        if (userMacro.startsWith(USER_MACRO_PREFIX)) {
            this.isUser = false;
            this.macro = userMacro;
        } else {
            this.isUser = true;
            this.user = User.getById(userMacro, false);
            //   throw new UnsupportedOperationException("User macro must start with prefix '"+USER_MACRO_PREFIX+"'");
        }

    }

    /**
     * @deprecated Use {@link #isUser() }
     */
    @Deprecated
    @SuppressFBWarnings(value = "NM_METHOD_NAMING_CONVENTION")
    public boolean IsUser() {
        return isUser;
    }
    
    public boolean isUser() {
        return isUser;
    }

    /**
     * Gets id of the user (calls User.getId() or returns macro).
     *
     * @return ID or macro
     */
    public String getId() {
        return isUser ? user.getId() : macro;
    }

    @Override
    public String toString() {
        return isUser ? UserStringFormatter.format(user) : macro;
    }

    public boolean meetsMacro(String userId) {
        // Handle macros and get effective user's id
        String comparedId;
        if (isUser) {
            if (user == null) {
                return false;
            }
            comparedId = user.getId();
        } else {
            User current = User.current();
            if (current == null) {
                return false;
            }
            comparedId = current.getId();
        }

        // Check      
        return User.idStrategy().equals(comparedId, userId);
    }

}
