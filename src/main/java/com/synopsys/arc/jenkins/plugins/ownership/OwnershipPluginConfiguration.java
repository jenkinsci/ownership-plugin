/*
 * The MIT License
 *
 * Copyright 2014 Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
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

import com.synopsys.arc.jenkins.plugins.ownership.extensions.ItemOwnershipPolicy;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import javax.annotation.Nonnull;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Oleg Nenashev <nenashev@synopsys.com>
 */
public class OwnershipPluginConfiguration 
        implements Describable<OwnershipPluginConfiguration> {

    private final ItemOwnershipPolicy itemOwnershipPolicy;

    @DataBoundConstructor
    public OwnershipPluginConfiguration(@Nonnull ItemOwnershipPolicy itemOwnershipPolicy) {
        this.itemOwnershipPolicy = itemOwnershipPolicy;
    }

    @Nonnull
    public ItemOwnershipPolicy getItemOwnershipPolicy() {
        return itemOwnershipPolicy;
    }
   
    @Override
    public Descriptor<OwnershipPluginConfiguration> getDescriptor() {
        return DESCRIPTOR;
    }
    
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    
    public static class DescriptorImpl extends Descriptor<OwnershipPluginConfiguration> {

        @Override
        public String getDisplayName() {
            return "N/A";
        }
    }
    
}
