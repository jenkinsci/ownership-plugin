/*
 * The MIT License
 *
 * Copyright 2013 Oleg Nenashev, Synopsys Inc.
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
package com.synopsys.arc.jenkins.plugins.ownership.jobs;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.ItemOwnershipPolicy;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;
import java.io.IOException;

/**
 * Listener checks for job configuration changes and
 * applies and modifies its ownership info.
 * By default, the plugin drops initial ownership settings.
 * It can also set the current user as a new owner (can be enabled in global configs). 
 * @author Oleg Nenashev
 */
@Extension
public class OwnershipItemListener extends ItemListener {
    
    @Override
    public void onCopied(Item src, Item item) {      
        OwnershipDescription d = getPolicy().onCopied(src, item);
        modifyOwnership(item, d);
    }

    @Override
    public void onCreated(Item item) {
        OwnershipDescription d = getPolicy().onCreated(item);
        modifyOwnership(item, d);
    }
    
    private ItemOwnershipPolicy getPolicy() {
        return OwnershipPlugin.getInstance().getConfiguration().getItemOwnershipPolicy();
    }
    
    private void modifyOwnership(Item item, OwnershipDescription ownership) {
        if (item instanceof Job) {
            Job job = (Job) item;
            try {
                JobOwnerHelper.setOwnership(job, ownership);
            } catch (IOException ex) {
                //TODO: do something
            }
        }
    }
}
