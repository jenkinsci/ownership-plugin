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
import hudson.Extension;
import hudson.model.Computer;
import hudson.node_monitors.AbstractNodeMonitorDescriptor;
import hudson.node_monitors.NodeMonitor;
import hudson.slaves.OfflineCause;
import java.io.IOException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Implements monitoring of ownership.
 * @author Oleg Nenashev
 * @since 0.4
 */
public class OwnershipNodeMonitor extends NodeMonitor {

    @Override
    public AbstractNodeMonitorDescriptor<?> getDescriptor() {
        return DESCRIPTOR;
    }
    
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends AbstractNodeMonitorDescriptor<Data> {
        
        @Override
        protected Data monitor(Computer c) throws IOException, InterruptedException {
            OwnershipDescription ownership = ComputerOwnerHelper.getInstance().getOwnershipDescription(c);
            return new Data(ownership);
        }      

        @Override
        public String getDisplayName() {
            return "Ownership";
        }

        @Override
        public NodeMonitor newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new OwnershipNodeMonitor();
        }      
    }
    /**
     * Immutable representation of the monitoring data.
     */
    @ExportedBean  
    public static class Data extends OfflineCause {
        private final OwnershipDescription ownershipDescription;

        public Data(OwnershipDescription ownershipDescription) {
            this.ownershipDescription = ownershipDescription;
        }

        public OwnershipDescription getOwnershipDescription() {
            return ownershipDescription;
        }
                
        @Override
        public String toString() {
            return ownershipDescription.getPrimaryOwnerId();
        }        
    }
}
