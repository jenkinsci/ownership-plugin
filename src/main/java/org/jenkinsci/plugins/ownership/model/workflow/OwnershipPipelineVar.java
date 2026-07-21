/*
 * The MIT License
 *
 * Copyright (c) 2016 CloudBees, Inc.
 * Copyright (c) 2026 Ownership Plugin Contributors.
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
import groovy.lang.GroovyObject;
import java.io.Serializable;
import javax.annotation.CheckForNull;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Pipeline value of the {@code ownership} global variable.
 * Pure Java replacement for the former Groovy {@code Impl} loaded via
 * {@code CleanGroovyClassLoader} (see
 * <a href="https://github.com/jenkinsci/ownership-plugin/issues/84">issue #84</a>).
 */
@Restricted(NoExternalUse.class)
public final class OwnershipPipelineVar implements Serializable {

    private static final long serialVersionUID = 1L;

    private final CpsScript script;

    public OwnershipPipelineVar(CpsScript script) {
        this.script = script;
    }

    @Whitelisted
    public OwnershipDescription getJob() throws Exception {
        return OwnershipGlobalVariable.getJobOwnershipDescription((RunWrapper) script.getProperty("currentBuild"));
    }

    @CheckForNull
    @Whitelisted
    public OwnershipDescription getNode() throws Exception {
        return OwnershipGlobalVariable.getNodeOwnershipDescription(readNodeName());
    }

    @CheckForNull
    private String readNodeName() throws Exception {
        Object env = script.getProperty("env");
        if (env == null) {
            return null;
        }
        Object nodeName;
        if (env instanceof GroovyObject) {
            nodeName = ((GroovyObject) env).getProperty("NODE_NAME");
        } else {
            nodeName = env.getClass().getMethod("get", String.class).invoke(env, "NODE_NAME");
        }
        return nodeName == null ? null : nodeName.toString();
    }
}
