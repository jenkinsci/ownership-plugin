/*
 * The MIT License
 *
 * Copyright 2016 Oleg Nenashev
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
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;
import org.jenkinsci.plugins.ownership.model.folders.FolderOwnershipHelper;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Stores inheritance options for {@link OwnershipPlugin}.
 * This section is attached as an advanced section to {@link OwnershipPluginConfiguration}.
 * These options has been created 
 * @author Oleg Nenashev
 * @since 0.9
 */
public class InheritanceOptions implements Describable<InheritanceOptions> {
       
    public static final InheritanceOptions DEFAULT = new InheritanceOptions(false);
    
    private final boolean blockInheritanceFromItemGroups;

    @DataBoundConstructor
    public InheritanceOptions(boolean blockInheritanceFromItemGroups) {
        this.blockInheritanceFromItemGroups = blockInheritanceFromItemGroups;
    }

    /**
     * Blocks ownership inheritance from {@link ItemGroup}s.
     * This inheritance is used in {@link JobOwnerHelper} and {@link FolderOwnershipHelper}
     * in order to retrieve the info from parent folders.
     * Such inheritance may impact the performance of Jenkins instance, hence it is possible to disable it.
     * @return {@code true} if ownership inheritance should be blocked.
     */
    public boolean isBlockInheritanceFromItemGroups() {
        return blockInheritanceFromItemGroups;
    }
    
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }
  
    public static class DescriptorImpl extends Descriptor<InheritanceOptions> {
        
        @Override
        public String getDisplayName() {
            return "N/A";
        }
    }
}
