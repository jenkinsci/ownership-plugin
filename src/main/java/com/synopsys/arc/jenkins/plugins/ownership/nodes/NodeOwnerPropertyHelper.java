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
import hudson.model.Node;
import hudson.model.User;
import hudson.slaves.NodeProperty;
import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.ownership.model.OwnershipInfo;
import org.jenkinsci.plugins.ownership.model.nodes.NodeOwnershipDescriptionSource;

/**
 * Provides helper for Node owner
 * @since 0.0.3
 * @author Oleg Nenashev
 * @see OwnerNodeProperty
 * @see NodeOwnerHelper
 */
public class NodeOwnerPropertyHelper extends AbstractOwnershipHelper<NodeProperty> {

    static final NodeOwnerPropertyHelper Instance = new NodeOwnerPropertyHelper();

    /**
     * Gets OwnerNodeProperty from job if possible
     * @param node Node property
     * @return OwnerNodeProperty or null
     */
    @CheckForNull
    private static OwnerNodeProperty getOwnerProperty(@CheckForNull NodeProperty node)  {
        return (OwnerNodeProperty)node;
    }
      
    @Nonnull
    @Override
    public OwnershipDescription getOwnershipDescription(@CheckForNull NodeProperty item) {
        // TODO: This method impl is a performance hack. May be replaced by getOwnershipInfo() in 1.0
        OwnerNodeProperty prop = getOwnerProperty(item);
        OwnershipDescription descr = (prop != null) ? prop.getOwnership() : null;
        return descr != null ? descr : OwnershipDescription.DISABLED_DESCR;
    }

    @Override
    public OwnershipInfo getOwnershipInfo(NodeProperty item) {
        OwnerNodeProperty prop = getOwnerProperty(item);
        OwnershipDescription descr = (prop != null) ? prop.getOwnership() : null;
        return descr != null 
                ? new OwnershipInfo(descr, new NodeOwnershipDescriptionSource(getNode(item)))
                : OwnershipInfo.DISABLED_INFO;
    }
    
    @Nonnull
    @Override
    public Collection<User> getPossibleOwners(NodeProperty item) {
        if (OwnershipPlugin.getInstance().isRequiresConfigureRights()) {
            //FIXME: Fix after fix of bug at Jenkins
            return UserCollectionFilter.filterUsers(User.getAll(), true);
        } else {
            return User.getAll();
        }
    }   

    private @CheckForNull Node getNode(@Nonnull NodeProperty item){
        if (item instanceof OwnerNodeProperty) {
            OwnerNodeProperty prop = (OwnerNodeProperty) item;
            return prop.getNode();
        }
        return null;
    }

    @Override
    public String getItemTypeName(NodeProperty item) {
        return NodeOwnerHelper.ITEM_TYPE_NAME;
    }
    
    @Override
    public String getItemDisplayName(NodeProperty item) { 
        Node node = getNode(item);
        return node != null ? NodeOwnerHelper.Instance.getItemDisplayName(node) : "unknown node";
    }

    @Override
    public String getItemURL(NodeProperty item) {
        Node node = getNode(item);
        return node != null ? NodeOwnerHelper.Instance.getItemURL(node) : null;
    }     
}
