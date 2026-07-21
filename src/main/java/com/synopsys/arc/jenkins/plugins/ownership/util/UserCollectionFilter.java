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

import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.IUserFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.UserComparator;
import hudson.model.User;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import javax.annotation.Nonnull;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Filters {@link Collection}s of {@link User}s.
 * @author Oleg Nenashev
 * @since 0.1
 */
@Restricted(NoExternalUse.class)
public class UserCollectionFilter {

    @Nonnull
    public static Collection<User> filterUsers(
            @Nonnull Collection<User> input, boolean enableSort, @Nonnull IUserFilter... filters) {
        // Sort users
        UserComparator comparator = new UserComparator();
        LinkedList<User> userList = new LinkedList<>(User.getAll());
        if (enableSort) {
            Collections.sort(userList, comparator);
        }

        // Prepare new list
        Collection<User> res = new ArrayList<>();
        for (User user : userList) {
            if (user == null) {
                continue;
            }

            boolean meets = true;
            for (int i = 0; meets && i < filters.length; i++) {
                meets = filters[i].filter(user);
            }

            if (meets) {
                res.add(user);
            }
        }
        return res;
    }
}
