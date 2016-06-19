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
package com.synopsys.arc.jenkins.plugins.ownership.wrappers;

import com.synopsys.arc.jenkins.plugins.ownership.Messages;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.ownership.util.environment.EnvSetupOptions;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Provides wrapper, which injects ownership variables into the build environment.
 * The wrapper support both slave and node ownership information.
 * @author Oleg Nenashev
 * @since 0.2
 */
public class OwnershipBuildWrapper extends BuildWrapper {
    
    private @Nonnull EnvSetupOptions envSetupOptions;
    @Deprecated
    private transient final boolean injectNodeOwnership = false;
    @Deprecated
    private transient final boolean injectJobOwnership = false;

    @DataBoundConstructor
    public OwnershipBuildWrapper(@Nonnull EnvSetupOptions envSetupOptions) {
        this.envSetupOptions = envSetupOptions;
    }

    public OwnershipBuildWrapper(boolean injectJobOwnership, boolean injectNodeOwnership) {
        this(new EnvSetupOptions(injectJobOwnership, injectNodeOwnership));
    }
    
    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "Migration of the old data")
    public Object readResolve() {
        if (envSetupOptions == null) {
            envSetupOptions = new EnvSetupOptions(injectJobOwnership, injectNodeOwnership);
        }
        return this;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        return new Environment() {
            // Empty instantination. The entire code has been moved to OwnershipRunListener
        };
    }

    public @Nonnull EnvSetupOptions getEnvSetupOptions() {
        return envSetupOptions;
    }
    
    public boolean isInjectJobOwnership() {
        return envSetupOptions.isInjectJobOwnership();
    }

    public boolean isInjectNodeOwnership() {
        return envSetupOptions.isInjectNodeOwnership();
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
