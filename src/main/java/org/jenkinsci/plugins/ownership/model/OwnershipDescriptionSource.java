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

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import hudson.model.Job;
import hudson.model.Node;
import javax.annotation.CheckForNull;

/**
 * References the source of {@link OwnershipDescription}.
 * This describable class is being used in UI (e.g. {@code manage-owners} page).
 * The summary info can be retrieved using {@code summary.jelly}.
 * @param <TSourceType> Type of the source items.
 * @author Oleg Nenashev
 * @since 0.9
 */
public abstract class OwnershipDescriptionSource<TSourceType extends Object> {
    
    @CheckForNull
    private final TSourceType item;

    public OwnershipDescriptionSource(@CheckForNull TSourceType item) {
        this.item = item;
    }
       
    /**
     * Provides a reference to the item, which acts as a source.
     * It may be {@link Job}, {@link Node} or whatever other class.
     * @return Instance, which is referenced as an Item source.
     *  {@code null} means that there is no item, which could describe the {@link OwnershipDescription}.
     */
    @CheckForNull
    public TSourceType getItem() {
        return item;
    }
    
    public static class DisabledSource<TSourceType extends Object> extends OwnershipDescriptionSource<TSourceType>{
        public DisabledSource() {
            super(null);
        }
    }
}
