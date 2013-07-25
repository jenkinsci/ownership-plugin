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
package com.synopsys.arc.jenkins.plugins.ownership.security.rolestrategy;

import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipItem;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerJobProperty;
import com.synopsys.arc.jenkins.plugins.ownership.nodes.OwnerNodeProperty;
import com.synopsys.arc.jenkins.plugins.rolestrategy.Macro;
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleMacroExtension;
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType;
import static com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType.Project;
import static com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType.Slave;
import hudson.model.Computer;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.Node;
import hudson.model.User;
import hudson.security.AccessControlled;

/**
 *
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 */
abstract class AbstractOwnershipRoleMacro extends RoleMacroExtension {
    public static final String NO_SID_SUFFIX="NoSid";
    
    @Override
    public boolean IsApplicable(RoleType roleType) {
        switch (roleType) {
            case Project:
                return true;
            case Slave:
                return true;
            default:
                return false;
        }
    }
    
    public static OwnershipDescription getOwnership(RoleType type, AccessControlled item) {
        IOwnershipItem ownership = null;
        switch(type) {
            case Project:
                if (Job.class.isAssignableFrom(item.getClass())) { 
                    Job prj = (Job)item;
                    JobProperty prop = prj.getProperty(JobOwnerJobProperty.class);
                    if (prop != null) {
                        ownership = ((JobOwnerJobProperty)prop);
                    }                 
                }
                break;
            case Slave:
                if (Computer.class.isAssignableFrom(item.getClass())) {
                    Computer comp = (Computer)item;
                    Node node = comp.getNode();
                    if (node != null) {
                        ownership = node.getNodeProperties().get(OwnerNodeProperty.class);
                    }
                }
                break;
            default:
                //do nothing
        }
        return ownership!=null ? ownership.getOwnership() : OwnershipDescription.DISABLED_DESCR;
    }
    
    public static boolean hasPermission(User user, RoleType type, AccessControlled item, Macro macro, boolean acceptCoowners) {
        //TODO: implement
        if (user == null) {
            return false;
        }       
        return getOwnership(type, item).isOwner(user, acceptCoowners);
    }
}
