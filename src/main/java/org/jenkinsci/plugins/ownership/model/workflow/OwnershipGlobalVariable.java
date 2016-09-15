/*
 * The MIT License
 *
 * Copyright (c) 2016 CloudBees, Inc.
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
package org.jenkinsci.plugins.ownership.model.workflow;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.nodes.NodeOwnerHelper;
import groovy.lang.Binding;
import hudson.Extension;
import hudson.model.Node;
import hudson.model.Run;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.ProxyWhitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.StaticWhitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Provides {@link OwnershipDescription}s for different item types.
 * @author Oleg Nenashev
 * @since 0.9
 */
@Extension
public class OwnershipGlobalVariable extends GlobalVariable {

    @Override 
    public final Object getValue(CpsScript script) throws Exception {   
        final Binding binding = script.getBinding();
        final Object loadedObject;
        if (binding.hasVariable(getName())) {
            loadedObject = binding.getVariable(getName());
        } else {
            // Note that if this were a method rather than a constructor, we would need to mark it @NonCPS lest it throw CpsCallableInvocation.
            loadedObject = script.getClass().getClassLoader()
                    .loadClass(getClassName())
                    .getConstructor(CpsScript.class).newInstance(script);
            binding.setVariable(getName(), loadedObject);
        }
        return loadedObject;
    }
    
    /**
     * Canonical name of the class to be loaded.
     * @return Canonical class name
     */
    @Nonnull
    private String getClassName() {
        return getClass().getName() + ".Impl";
    }
    
    @Override 
    public String getName() {
        return "ownership";
    }
    
    /**
     * Loads a snippet from the resource file.
     * @param name Name of the snippet (e.g. &quot;loadMultipleFiles&quot;)
     * @return Workflow script text
     * @throws IOException Loading error
     */
    @Restricted(NoExternalUse.class)
    public static String getSampleSnippet(String name) throws IOException {
        final String resourceName = "OwnershipGlobalVariable/sample_" + name + ".groovy";
        final InputStream scriptStream = OwnershipGlobalVariable.class.getResourceAsStream(resourceName);
        if (scriptStream == null) {
            throw new IOException("Cannot find sample script in " + resourceName);
        }
        return IOUtils.toString(scriptStream, "UTF-8");
    }
    
    @Restricted(NoExternalUse.class)
    @Whitelisted
    public static OwnershipDescription getJobOwnershipDescription(RunWrapper currentRun) {
        Run<?, ?> rawBuild = currentRun.getRawBuild();
        if (rawBuild == null) {
            throw new IllegalStateException("Cannot retrieve build from Pipeline Run Wrapper");
        }
        return JobOwnerHelper.Instance.getOwnershipDescription(rawBuild.getParent());
    }
    
    @CheckForNull
    @Whitelisted
    @Restricted(NoExternalUse.class)
    public static OwnershipDescription getNodeOwnershipDescription(@CheckForNull String nodeName) {
        if (nodeName == null) {
            throw new IllegalStateException("Cannot get Ownership info outside the node() block");
        }
        final Node node = "master".equals(nodeName) 
                ? Jenkins.getActiveInstance()
                : Jenkins.getActiveInstance().getNode(nodeName);
        if (node == null) {
            throw new IllegalStateException("Cannot retrieve node by the name specified in env.NODE_NAME");
        }     
        return NodeOwnerHelper.Instance.getOwnershipDescription(node);
    }
    
    @Extension(optional = true)
    public static class MiscWhitelist extends ProxyWhitelist {

        public MiscWhitelist() throws IOException {
            super(new StaticWhitelist(
                    "new java.util.TreeMap",
                    "method groovy.lang.Closure call java.lang.Object",
                    "method java.lang.Object toString"
            ));
        }
    }
}
