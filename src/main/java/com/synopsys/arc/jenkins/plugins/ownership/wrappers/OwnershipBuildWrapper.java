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
package com.synopsys.arc.jenkins.plugins.ownership.wrappers;

import com.synopsys.arc.jenkins.plugins.ownership.Messages;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerJobProperty;
import com.synopsys.arc.jenkins.plugins.ownership.nodes.OwnerNodeProperty;
import com.synopsys.arc.jenkins.plugins.ownership.util.UserStringFormatter;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Provides wrapper, which injects ownership variables into the build environment;
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 * @since 0.2
 */
public class OwnershipBuildWrapper extends BuildWrapper {
    private boolean injectNodeOwnership;
    private boolean injectJobOwnership;

    @DataBoundConstructor
    public OwnershipBuildWrapper(boolean injectNodeOwnership, boolean injectJobOwnership) {
        this.injectNodeOwnership = injectNodeOwnership;
        this.injectJobOwnership = injectJobOwnership;
    }
    
    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {         
        final Map<String, String> vars = new TreeMap<String, String>();
        if (injectJobOwnership) {
            JobOwnerJobProperty prop = JobOwnerHelper.getOwnerProperty(build.getParent());  
            OwnershipDescription descr = prop != null ? prop.getOwnership() : OwnershipDescription.DISABLED_DESCR;
            getVariables(descr, vars, "JOB");
        }
        
        if (injectNodeOwnership) {
            OwnerNodeProperty prop = build.getBuiltOn().getNodeProperties().get(OwnerNodeProperty.class);
            OwnershipDescription descr = prop!=null ? prop.getOwnership() : OwnershipDescription.DISABLED_DESCR;
            getVariables(descr, vars, "NODE");
        }
        
        // Log items
        for (Entry<String, String> entry : vars.entrySet()) {
            listener.getLogger().println(OwnershipPlugin.LOG_PREFIX+"Setting "+entry.getKey()+"="+entry.getValue());
        }
        
        return new Environment() { 
            @Override
            public void buildEnvVars(Map<String, String> env) {
                env.putAll(vars);
            }
        };
    }
    
    private static void getVariables(OwnershipDescription descr, Map<String, String> target, String prefix) {      
        target.put(prefix+"_OWNER", descr.hasPrimaryOwner() ? descr.getPrimaryOwnerId() : "");
        String ownerEmail = UserStringFormatter.formatEmail(descr.getPrimaryOwnerId());  
        target.put(prefix+"_OWNER_EMAIL", ownerEmail != null ? ownerEmail : "");
        
        String coowners=target.get(prefix+"_OWNER");
        String coownerEmails=target.get(prefix+"_OWNER_EMAIL");
        for (String userId : descr.getCoownersIds()) {
            if (!coowners.isEmpty()) {
                coowners+=",";
            }
            coowners += userId;
            
            String coownerEmail = UserStringFormatter.formatEmail(userId);
            if (coownerEmail != null) {
                if (!coownerEmails.isEmpty()) {
                    coownerEmails+=",";
                }
                coownerEmails+=coownerEmail;
            }       
        }
        target.put(prefix+"_COOWNERS", coowners);
        target.put(prefix+"_COOWNERS_EMAILS", coownerEmails);     
    }
    
    public boolean isInjectJobOwnership() {
        return injectJobOwnership;
    }

    public boolean isInjectNodeOwnership() {
        return injectNodeOwnership;
    }
        
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        
        public DescriptorImpl() {
            super(OwnershipBuildWrapper.class);
        }

        @Override
        public String getDisplayName() {
            return Messages.Wrappers_OwnershipBuildWrapper_DisplayName();
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        } 
    }
}
