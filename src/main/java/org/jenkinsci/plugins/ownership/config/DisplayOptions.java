/*
 * The MIT License
 *
 * Copyright 2015 Oleg Nenashev
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

package org.jenkinsci.plugins.ownership.config;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPluginConfiguration;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.ownership.model.runs.RunOwnershipAction;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Stores display options for {@link OwnershipPlugin}.
 * This section is attached as an advanced section to {@link OwnershipPluginConfiguration}.
 * @author Oleg Nenashev
 * @since 0.8
 */
public class DisplayOptions implements Describable<DisplayOptions> {
       
    public static final DisplayOptions DEFAULT = new DisplayOptions(false, false);
    
    private final boolean hideRunOwnership;
    private final boolean hideOwnershipIfNoData;

    @DataBoundConstructor
    public DisplayOptions(boolean hideRunOwnership, boolean hideOwnershipIfNoData) {
        this.hideRunOwnership = hideRunOwnership;
        this.hideOwnershipIfNoData = hideOwnershipIfNoData;
    }

    /**
     * Disables Run summary boxes in {@link RunOwnershipAction}.
     * @return {@code true} if {@link RunOwnershipAction}'s summary box should not be displayed.
     */
    public boolean isHideRunOwnership() {
        return hideRunOwnership;
    }

    /**
     * Does not display Ownership summary boxes if Ownership is not configured.
     * @return {@code true} to hide empty Ownership summary boxes.
     */
    public boolean isHideOwnershipIfNoData() {
        return hideOwnershipIfNoData;
    }
    
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    @Symbol("ownershipVisualization")
    public static class DescriptorImpl extends Descriptor<DisplayOptions> {
        
        @Override
        public String getDisplayName() {
            return "N/A";
        }
    }
}
