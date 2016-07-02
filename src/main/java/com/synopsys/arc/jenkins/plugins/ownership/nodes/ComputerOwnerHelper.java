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
package com.synopsys.arc.jenkins.plugins.ownership.nodes;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.util.AbstractOwnershipHelper;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.User;
import java.io.IOException;
import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.collections.ListUtils;
import org.jenkinsci.plugins.ownership.model.OwnershipInfo;

/**
 * Provides ownership helper for {@link Computer}.
 * The class implements a wrapper of {@link NodeOwnerHelper}.
 * @author Oleg Nenashev
 */
public class ComputerOwnerHelper extends AbstractOwnershipHelper<Computer> {

    static final ComputerOwnerHelper Instance = new ComputerOwnerHelper();

    public static ComputerOwnerHelper getInstance() {
        return Instance;
    }
        
    @Override
    public OwnershipDescription getOwnershipDescription(@Nonnull Computer item) {
        // TODO: This method impl is a performance hack. May be replaced by getOwnershipInfo() in 1.0
        Node node = item.getNode();      
        return node != null 
                ? NodeOwnerHelper.Instance.getOwnershipDescription(node)
                : OwnershipDescription.DISABLED_DESCR; // No node - no ownership
    }

    @Override
    public OwnershipInfo getOwnershipInfo(Computer item) {
        Node node = item.getNode();      
        return node != null 
                ? NodeOwnerHelper.Instance.getOwnershipInfo(node)
                : OwnershipInfo.DISABLED_INFO;
    }

    @Override
    public Collection<User> getPossibleOwners(@Nonnull Computer computer) {
        Node node = computer.getNode();
        return node != null 
                ? NodeOwnerHelper.Instance.getPossibleOwners(node)
                : ListUtils.EMPTY_LIST;
    }  
    
    public static void setOwnership(@Nonnull Computer computer, 
            @CheckForNull OwnershipDescription descr) throws IOException {
        Node node = computer.getNode();
        if (node == null) {
            throw new IOException("Cannot set ownership. Probably, the node has been renamed or deleted.");
        }
        
        NodeOwnerHelper.setOwnership(node, descr);
    }

    @Override
    public String getItemTypeName(Computer item) {
        return "computer";
    }

    public String getItemDisplayName(Computer item) {
        return item.getDisplayName();
    }

    public String getItemURL(Computer item) {
        //TODO: Absolute URL
        return item.getUrl();
    }
}
