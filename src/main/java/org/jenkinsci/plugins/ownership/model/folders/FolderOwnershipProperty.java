/*
 * The MIT License
 *
 * Copyright (c) 2015-2017 Oleg Nenashev.
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
package org.jenkinsci.plugins.ownership.model.folders;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty;
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor;
import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipItem;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Descriptor;
import java.io.IOException;
import javax.annotation.CheckForNull;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Ownership property for {@link AbstractFolder}s.
 * @author Oleg Nenashev
 * @since 0.9
 */
public class FolderOwnershipProperty 
        extends AbstractFolderProperty<AbstractFolder<?>> 
        implements IOwnershipItem<AbstractFolder<?>>{
    
    @CheckForNull
    OwnershipDescription ownership;

    public FolderOwnershipProperty(@CheckForNull OwnershipDescription ownership) {
        this.ownership = ownership;
    }

    @Override
    public IOwnershipHelper<AbstractFolder<?>> helper() {
        return FolderOwnershipHelper.getInstance();
    }

    @Override
    public AbstractFolder<?> getDescribedItem() {
        return owner;
    }

    @Override
    public OwnershipDescription getOwnership() {
        return ownership != null ? ownership : OwnershipDescription.DISABLED_DESCR;
    }

    /**
     * Sets the new ownership description.
     * @param description Description to be set. Use {@code null} to drop settings.
     * @throws IOException Property cannot be saved.
     */
    public void setOwnershipDescription(@CheckForNull OwnershipDescription description) throws IOException {
        ownership = description;
        owner.save();
    }    

    @Override
    public AbstractFolderProperty<?> reconfigure(StaplerRequest req, JSONObject form) throws Descriptor.FormException {
        // Retain the current configuration in order to prevent changes by form submissions
        return new FolderOwnershipProperty(ownership);
    }
    
    @Extension(optional = true)
    public static class DescriptorImpl extends AbstractFolderPropertyDescriptor {

        @Override
        @SuppressFBWarnings(value = "NP_NONNULL_RETURN_VIOLATION", justification = "TODO: should be fixed, see jenkinsci PR #1880")
        public String getDisplayName() {
            // It prevents the property from displaying
            return null;
        }
        
    }
}
