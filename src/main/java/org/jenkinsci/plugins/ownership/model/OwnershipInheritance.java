/*
 * The MIT License
 *
 * Copyright 2026 Ownership Plugin contributors.
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
package org.jenkinsci.plugins.ownership.model;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPluginConfiguration;
import com.synopsys.arc.jenkins.plugins.ownership.util.AbstractOwnershipHelper;
import hudson.model.Item;
import hudson.model.ItemGroup;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Resolves ownership inherited from parent {@link ItemGroup}s.
 */
@Restricted(NoExternalUse.class)
public final class OwnershipInheritance {

    private OwnershipInheritance() {}

    /**
     * Walks parent item groups until an enabled ownership description is found.
     *
     * @param item item whose parents should be inspected
     * @return inherited ownership info, or {@link OwnershipInfo#DISABLED_INFO} if none
     */
    @Nonnull
    public static OwnershipInfo fromParents(@Nonnull Item item) {
        if (OwnershipPluginConfiguration.get().getInheritanceOptions().isBlockInheritanceFromItemGroups()) {
            return OwnershipInfo.DISABLED_INFO;
        }

        ItemGroup parent = item.getParent();
        AbstractOwnershipHelper<ItemGroup> located = OwnershipHelperLocator.locate(parent);
        while (located != null) {
            OwnershipInfo fromParent = located.getOwnershipInfo(parent);
            if (fromParent.getDescription().isOwnershipEnabled()) {
                return fromParent;
            }
            if (parent instanceof Item) {
                Item parentItem = (Item) parent;
                parent = parentItem.getParent();
                located = OwnershipHelperLocator.locate(parent);
            } else {
                located = null;
            }
        }
        return OwnershipInfo.DISABLED_INFO;
    }

    /**
     * Convenience for nullable items (e.g. during renames).
     */
    @Nonnull
    public static OwnershipInfo fromParentsOrDisabled(@CheckForNull Item item) {
        return item == null ? OwnershipInfo.DISABLED_INFO : fromParents(item);
    }
}
