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
import javax.annotation.Nonnull;

/**
 * Provides a full info about Ownership.
 * {@link OwnershipDescription} is designed to store the persisted data only,
 * this class also provides a runtime content. It should not be serialized anywhere.
 * @author Oleg Nenashev
 * @since 0.9
 */
public class OwnershipInfo {
    
    private final OwnershipDescription description;
    private final OwnershipDescriptionSource<?> source;
    
    public static final OwnershipInfo DISABLED_INFO = 
            new OwnershipInfo(OwnershipDescription.DISABLED_DESCR, new OwnershipDescriptionSource.DisabledSource<Object>());

    public OwnershipInfo(@Nonnull OwnershipDescription description, 
            @Nonnull OwnershipDescriptionSource<?> source) {
        this.description = description;
        this.source = source;
    }

    @Nonnull
    public OwnershipDescription getDescription() {
        return description;
    }

    @Nonnull
    public OwnershipDescriptionSource<?> getSource() {
        return source;
    }
}
