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
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.util.AbstractOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.util.UserCollectionFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.AccessRightsFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.IUserFilter;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.User;
import hudson.slaves.NodeProperty;
import java.io.IOException;
import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.ownership.model.OwnershipDescriptionSource;
import org.jenkinsci.plugins.ownership.model.OwnershipInfo;
import org.jenkinsci.plugins.ownership.model.nodes.NodeOwnershipDescriptionSource;

/**
 * Provides helper for Node owner.
 * TODO: Add Bug reference
 * @since 0.0.3
 * @author Oleg Nenashev
 * @see OwnerNodeProperty
 */
public class NodeOwnerHelper extends AbstractOwnershipHelper<Node> {

    public static final NodeOwnerHelper Instance = new NodeOwnerHelper();
    /**package*/ static final String ITEM_TYPE_NAME = "node";

    /**
     * Gets OwnerNodeProperty from job if possible.
     * @param node Node
     * @return OwnerNodeProperty or null
     */
    @CheckForNull
    public static OwnerNodeProperty getOwnerProperty(@Nonnull Node node) {
        NodeProperty prop = node.getNodeProperties().get(OwnerNodeProperty.class);
        return prop != null ? (OwnerNodeProperty)prop : null;
    }
        
    @Override
    public OwnershipDescription getOwnershipDescription(@CheckForNull Node item) {
        // TODO: This method impl is a performance hack. May be replaced by getOwnershipInfo() in 1.0
        if (item == null) { // Handle renames, etc.
            return OwnershipDescription.DISABLED_DESCR;
        }
        
        OwnerNodeProperty prop = getOwnerProperty(item);
        return prop != null ? prop.getOwnership() : OwnershipDescription.DISABLED_DESCR;
    }

    @Override
    public OwnershipInfo getOwnershipInfo(Node item) {
        if (item == null) { // Handle renames, etc.
            return OwnershipInfo.DISABLED_INFO;
        }
        
        OwnerNodeProperty prop = getOwnerProperty(item);
        return prop != null ? new OwnershipInfo(OwnershipDescription.DISABLED_DESCR, 
                new NodeOwnershipDescriptionSource(item)) : OwnershipInfo.DISABLED_INFO;
    }
    
    @Override
    public Collection<User> getPossibleOwners(Node item) {
        if (OwnershipPlugin.getInstance().isRequiresConfigureRights()) {
            IUserFilter filter = new AccessRightsFilter(item, Computer.CONFIGURE);
            return UserCollectionFilter.filterUsers(User.getAll(), true, filter);
        } else {
            return User.getAll();
        }
    }  
    
    /**
     * Sets ownership of the node.
     * @param node A target node
     * @param descr An ownership description to be assigned
     * @throws IOException A property modification error
     */
    public static void setOwnership(@Nonnull Node node, 
            @CheckForNull OwnershipDescription descr) throws IOException {
        OwnerNodeProperty prop = NodeOwnerHelper.getOwnerProperty(node);
        if (prop == null) {
            prop = new OwnerNodeProperty(node, descr);
            node.getNodeProperties().add(prop);
        } else {
            prop.setOwnershipDescription(descr);
        }
    }

    @Override
    public String getItemTypeName(Node item) {
        return ITEM_TYPE_NAME;
    }
    
    @Override
    public String getItemDisplayName(Node item) {
        return item.getDisplayName();
    }

    @Override
    public String getItemURL(Node item) {
        Computer c = item.toComputer();
        return c != null ? ComputerOwnerHelper.Instance.getItemURL(c) : null;
    }
}
