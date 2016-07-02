/*
 * The MIT License
 *
 * Copyright 2015 Oleg Nenashev <o.v.nenashev@gmail.com>.
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

package org.jenkinsci.plugins.ownership.model.runs;

import com.synopsys.arc.jenkins.plugins.ownership.IOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.ItemOwnershipAction;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.util.ui.OwnershipLayoutFormatter;
import com.synopsys.arc.jenkins.plugins.ownership.wrappers.OwnershipBuildWrapper;;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Job;
import hudson.model.Project;
import hudson.model.Run;
import hudson.security.Permission;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.ownership.util.environment.EnvSetupOptions;

/**
 * Displays ownership info for builds. 
 * This implementation is a stub for summaries visualization.
 * Currently, users cannot manage builds ownership.
 * @author Oleg Nenashev
 * @since 0.6
 */
public class RunOwnershipAction extends ItemOwnershipAction<Run> 
         implements EnvironmentContributingAction {

    public RunOwnershipAction(@Nonnull Run describedItem) {
        super(describedItem);
    }

    @Override
    public Permission getOwnerPermission() {
        return OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP;
    }

    @Override
    public Permission getProjectSpecificPermission() {
        return OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP;
    }

    @Override
    public boolean actionIsAvailable() {
        return false; // We don't provide action links now
    }

    @Override
    public IOwnershipHelper<Run> helper() {
        return RunOwnershipHelper.getInstance();
    }
    
    public OwnershipLayoutFormatter<Run> getLayoutFormatter() {
        return OwnershipPlugin.getInstance().getOwnershipLayoutFormatterProvider().getLayoutFormatter(getDescribedItem());
    }   
    
    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        boolean injectNodeOwnership = false;
        boolean injectJobOwnership = false;
        
        // Handle global options
        final EnvSetupOptions globalEnvSetupOptions = OwnershipPlugin.getInstance().
                getConfiguration().getGlobalEnvSetupOptions();
        if (globalEnvSetupOptions != null) {
            injectNodeOwnership |= globalEnvSetupOptions.isInjectNodeOwnership();
            injectJobOwnership |= globalEnvSetupOptions.isInjectJobOwnership();
        }
        
        // Check BuildWrapper options id available
        final Job parent = build.getParent();
        if (parent instanceof Project) { 
            final Project prj = (Project) parent;
            final OwnershipBuildWrapper wrapper = (OwnershipBuildWrapper) 
                    prj.getBuildWrappersList().get(OwnershipBuildWrapper.class);
            if (wrapper != null) {
                injectJobOwnership |= wrapper.isInjectJobOwnership();
                injectNodeOwnership |= wrapper.isInjectNodeOwnership();             
            }
        } // TODO: else do something?
        
        RunOwnershipHelper.setUp(build, env, null, injectJobOwnership, injectNodeOwnership);
    }
}
