/*
 * The MIT License
 *
 * Copyright (c) 2016-2017 Oleg Nenashev.
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
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.ItemOwnershipPolicy;
import hudson.Extension;
import hudson.Plugin;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

/**
 * Locates changes in {@link AbstractFolder}s and assigns ownership accordingly.
 * @author Oleg Nenashev
 */
@Extension(optional = true)
public class FolderItemListener extends ItemListener {
    
    private static final Logger LOGGER = Logger.getLogger(FolderItemListener.class.getName());
    
    @Override
    public void onCopied(Item src, Item item) {     
        if (!isFoldersPluginEnabled()) {
            return;
        }
        OwnershipDescription d = getPolicy().onCopied(src, item);
        modifyOwnership(item, d);
    }

    @Override
    public void onCreated(Item item) {
        if (!isFoldersPluginEnabled()) {
            return;
        }
        OwnershipDescription d = getPolicy().onCreated(item);
        modifyOwnership(item, d);
    }
    
    private ItemOwnershipPolicy getPolicy() {
        return OwnershipPlugin.getInstance().getConfiguration().getItemOwnershipPolicy();
    }
    
    private boolean isFoldersPluginEnabled() {
        final Plugin plugin = Jenkins.getInstance().getPlugin("cloudbees-folder");
        return plugin != null && plugin.getWrapper().isActive();
    }
    
    private void modifyOwnership(Item item, OwnershipDescription ownership) {
        if (item instanceof AbstractFolder) {
            AbstractFolder<?> folder = (AbstractFolder) item;
            try {
                FolderOwnershipHelper.setOwnership(folder, ownership);
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Cannot change ownership of {0} to [{1}]. {2}",
                        new Object[] {item, ownership, ex});
            }
        }
    }
}
