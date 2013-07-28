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
package com.synopsys.arc.jenkins.plugins.ownership;

import hudson.model.User;
import java.util.Collection;

/**
 * Adapter for typical ownership operations with different item types.
 * Every IOwnership item should provide ownership helper.
 * @param <TObjectType> Type of object, for which ownership should be resolved
 * @author Oleg Nenashev <nenashev@synopsys.com>
 * @since 0.0.2
 * @see IOwnershipItem
 */
public interface IOwnershipHelper<TObjectType>  {
    /**
     * Get ownerId of the item
     * @param item Item to be describes
     * @return userId
     */
    public String getOwner(TObjectType item);
    
    /**
     * Get e-mail of the owner
     * @param item Item to be describes
     * @return Owner's e-mail
     * @since 0.0.4
     */
    public String getOwnerEmail(TObjectType item);
    
    /**
     * Check if item has owners
     * @param item Item to be checked
     * @return true if item has owners
     */
    public boolean isOwnerExists(TObjectType item);
    
    /**
     * Get Display string of the owner (and coowners)
     * @param item Item to be described
     * @return User description string
     */
    public String getOwnerLongString(TObjectType item);
    
    
    
    /**
     * Gets ownership description of the requested item
     * @param item Item to be described
     * @return Ownership description
     * @since 0.0.3
     */
    public OwnershipDescription getOwnershipDescription(TObjectType item);
    
    
    /**
     * Get list of users, who can be item's owner
     * @param item Item to be described
     * @return List of users, who can be item's owner
     * @since 0.0.3
     */
    public Collection<User> getPossibleOwners(TObjectType item);
    
    /**
     * Gets User's name for visualization
     * @param usr User
     * @return String for display
     */
    public String getDisplayName(User usr);
}
