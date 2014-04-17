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

import javax.annotation.Nonnull;

/**
 * Class provides basic methods for ownership handling.
 * @param <TObjectType> Type of the described object
 * @author Oleg Nenashev <nenashev@synopsys.com>
 * @since 0.0.3
 */
public interface IOwnershipItem<TObjectType> {
    /**
     * Gets a helper for typical operations.
     * @return Helper
     */
    @Nonnull
    public abstract IOwnershipHelper<TObjectType> helper();
    
    /**
     * Gets item, which is being described by action.
     * @return Described item
     */
    @Nonnull
    public TObjectType getDescribedItem();
    
    /**
     * Gets ownership description.
     * By default, returns {@link #OwnershipDescription.DISABLED_DESCR}
     * @return Ownership Description (not null)
     */
    @Nonnull
    public OwnershipDescription getOwnership();
}
