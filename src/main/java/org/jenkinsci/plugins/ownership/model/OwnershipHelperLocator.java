/*
 * The MIT License
 *
 * Copyright (c) 2016 Oleg Nenashev.
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

import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.util.AbstractOwnershipHelper;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;

/**
 * Extension point, which allows to identify {@link IOwnershipHelper} for particular classes.
 * @param <T> Type of the item
 * @author Oleg Nenashev
 * @since 0.9
 */
public abstract class OwnershipHelperLocator <T extends Object> implements ExtensionPoint {
    
    /**
     * Looks up ownership helpers for a class.
     * @param item Item, for which the ownership should be retrieved
     * @return Helper. Returns null if there is no applicable helper provided by this extension.
     */
    @CheckForNull
    public abstract AbstractOwnershipHelper<T> findHelper(Object item);
    
    /**
     * Returns all the registered {@link OwnershipHelperLocator}s.
     * @return All registered {@link OwnershipHelperLocator}s
     */
    @Nonnull
    public static ExtensionList<OwnershipHelperLocator> all() {
        return Jenkins.getActiveInstance().getExtensionList(OwnershipHelperLocator.class);
    }
    
    /**
     * Locates {@link IOwnershipHelper} for the specified item.
     * @param <T> Class of the required helper
     * @param item Item, for which we need a helper
     * @return Located helper. May be null if there is no relevant helper
     */
    @CheckForNull
    @SuppressWarnings("unchecked")
    public static <T> AbstractOwnershipHelper<T> locate(T item) {
        return (AbstractOwnershipHelper<T>)locate(item, item.getClass());
    }
    
    /**
     * Locates {@link IOwnershipHelper} for the specified item.
     * @param <T> Class of the required helper
     * @param item Item, for which we need a helper
     * @param requiredClass Required class
     * @return Located helper. May be null if there is no relevant helper
     */
    @CheckForNull
    @SuppressWarnings("unchecked")
    public static <T> AbstractOwnershipHelper<T> locate(Object item, Class<T> requiredClass) {
        for (OwnershipHelperLocator<?> helper : all()) {
            AbstractOwnershipHelper<?> located = helper.findHelper(item);
            //TODO: Helper verification would be useful
            if (located != null) {
                return (AbstractOwnershipHelper<T>) located;
            }
        }
        return null;
    }
}
