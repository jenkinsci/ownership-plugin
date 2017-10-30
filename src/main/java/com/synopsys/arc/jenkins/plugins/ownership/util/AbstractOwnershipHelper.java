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

import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import hudson.model.User;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.ownership.model.OwnershipInfo;

/**
 * Provides basic operations for ownership helpers.
 * @param <TObjectType> An item type, which is supported by the class.
 * @since 0.0.4
 * @author Oleg Nenashev
 */
public abstract class AbstractOwnershipHelper<TObjectType>  
    implements IOwnershipHelper<TObjectType> {
    
    @Override
    public final @Nonnull String getDisplayName(@CheckForNull User usr) {
        return UserStringFormatter.format(usr);
    } 
    
    @Override
    public final @CheckForNull String getOwnerEmail(@Nonnull TObjectType item) {
        OwnershipDescription descr = getOwnershipDescription(item);
        return  UserStringFormatter.formatEmail(descr.getPrimaryOwner());      
    }
      
    @Override
    public final @Nonnull String getOwnerLongString(@Nonnull TObjectType item) {
        OwnershipDescription descr = getOwnershipDescription(item);     
        return descr.isOwnershipEnabled() 
                ? UserStringFormatter.format(descr.getPrimaryOwner()) 
                : UserStringFormatter.UNKNOWN_USER_STRING;
    }
       
    @Override
    public final @Nonnull  String getOwner(@Nonnull TObjectType item) {
        OwnershipDescription descr = getOwnershipDescription(item);
        return (descr.isOwnershipEnabled()) 
                ? UserStringFormatter.formatShort(descr.getPrimaryOwnerId())
                : UserStringFormatter.UNKNOWN_USER_STRING;
    }    
    
    @Override
    public final boolean isOwnerExists(@Nonnull TObjectType item) {
        OwnershipDescription descr = getOwnershipDescription(item);
        return descr.isOwnershipEnabled() ? descr.hasPrimaryOwner() : false;
    }

    @Override
    public Collection<User> getPossibleOwners(TObjectType item) {
        return Collections.emptyList();
    }
    
    //TODO: promote to interface
    /**
     * Checks if the summary box should be displayed.
     * @param item Current item
     * @return {@code true} if the summary box should be displayed (even if there is no data); 
     * @since 0.8
     */
    public boolean isDisplayOwnershipSummaryBox(@Nonnull TObjectType item) {
        // If there is no data, check global options
        if (!getOwnershipDescription(item).isOwnershipEnabled()) {
            return !OwnershipPlugin.getInstance().getConfiguration().getDisplayOptions().isHideOwnershipIfNoData();
        }
        
        return true;
    }
 
    /**
     * Gets ownership info of the requested item.
     * @param item Item to be described
     * @return Ownership description. The method returns a 
     * {@link OwnershipDescription#DISABLED_DESCR}
     * @since 0.9
     */
    @Nonnull
    public abstract OwnershipInfo getOwnershipInfo(@Nonnull TObjectType item);
}
