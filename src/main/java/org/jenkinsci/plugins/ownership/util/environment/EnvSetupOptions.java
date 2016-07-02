/*
 * The MIT License
 *
 * Copyright 2015 Oleg Nenashev <o.v.nenashev@gmail.com>.
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

package org.jenkinsci.plugins.ownership.util.environment;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Stores environment setup options for {@link OwnershipPlugin}.
 * @author Oleg Nenashev
 * @since 0.6
 */
public class EnvSetupOptions implements Describable<EnvSetupOptions> {
    
    private final boolean injectNodeOwnership;
    private final boolean injectJobOwnership;

    @DataBoundConstructor
    public EnvSetupOptions(boolean injectJobOwnership, boolean injectNodeOwnership) {
        this.injectNodeOwnership = injectNodeOwnership;
        this.injectJobOwnership = injectJobOwnership;
    }

    public boolean isInjectJobOwnership() {
        return injectJobOwnership;
    }

    public boolean isInjectNodeOwnership() {
        return injectNodeOwnership;
    }
    
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }
  
    public static class DescriptorImpl extends Descriptor<EnvSetupOptions> {
        
        @Override
        public String getDisplayName() {
            return "N/A";
        }
    }
}
