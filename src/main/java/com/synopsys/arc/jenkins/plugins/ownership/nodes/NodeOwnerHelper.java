/*
 * The MIT License
 *
 * Copyright 2013 Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
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
import com.synopsys.arc.jenkins.plugins.ownership.util.UserCollectionFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.AccessRightsFilter;
import com.synopsys.arc.jenkins.plugins.ownership.util.userFilters.IUserFilter;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.User;
import hudson.slaves.NodeProperty;
import java.util.Collection;

/**
 * Provides helper for Node owner
 * @todo Add Bug reference
 * @deprecated Bug in Jenkins => doesn't work
 * @since 0.0.3
 * @author Oleg Nenashev <nenashev@synopsys.com>
 * @see OwnerNodeProperty
 */
public class NodeOwnerHelper extends AbstractOwnershipHelper<Node> {

    static final NodeOwnerHelper Instance = new NodeOwnerHelper();

    /**
     * Gets OwnerNodeProperty from job if possible
     * @param node Node
     * @return OwnerNodeProperty or null
     */
    public static OwnerNodeProperty getOwnerProperty(Node node)
    {
        NodeProperty prop = node.getNodeProperties().get(OwnerNodeProperty.class);
        return prop != null ? (OwnerNodeProperty)prop : null;
    }
        
    @Override
    public OwnershipDescription getOwnershipDescription(Node item) {
        OwnerNodeProperty prop = getOwnerProperty(item);
        return prop != null ? prop.getOwnership() : OwnershipDescription.DISABLED_DESCR;
    }
    
    @Override
    public Collection<User> getPossibleOwners(Node item) {
        IUserFilter filter = new AccessRightsFilter(item, Computer.CONFIGURE);
        Collection<User> res = UserCollectionFilter.filterUsers(User.getAll(), true, filter);
        return res;
    }   
}
