/*
 * The MIT License
 *
 * Copyright 2013 Synopsys Inc., Oleg Nenashev
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

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.nodes.NodeOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.util.OwnershipDescriptionHelper;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.model.TaskListener;
import java.io.IOException;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

/**
 * Provides OWNERSHIP token macro.
 * This macro allows to extract information about ownership.
 * An information type can be specified by additional var parameter
 * (see {@link OwnershipFunction} to get a list of supported operations).
 * @author Oleg Nenashev
 * @since 0.4
 */
@Extension(optional = true)
public class OwnershipTokenMacro extends DataBoundTokenMacro {
    public static final String MACRO_NAME="OWNERSHIP";
    
    /**
     * An information type to be retrieved.
     * {@link OwnershipFunction} contains list of supported commands.
     */
    @Parameter
    public String var;

    @Override
    public boolean acceptsMacroName(String string) {
        return string.equals(MACRO_NAME);
    }
    
    @Override
    public String evaluate(AbstractBuild<?, ?> ab, TaskListener tl, String string) throws MacroEvaluationException, IOException, InterruptedException {
        OwnershipFunction func;      
        if (var == null) {
            throw new MacroEvaluationException(MACRO_NAME+" macro requires the 'var' parameter");
        }
        
        try {
            func = OwnershipFunction.valueOf(var);
        } catch (IllegalArgumentException ex) {
            throw new MacroEvaluationException(MACRO_NAME+" macro does not support var="+var);
        }
            
        OwnershipDescription job = JobOwnerHelper.Instance.getOwnershipDescription(ab.getProject());
        
        // Get data for node
        final Node node = ab.getBuiltOn();
        OwnershipDescription nodeDescription = node != null 
                ? NodeOwnerHelper.Instance.getOwnershipDescription(node) 
                : OwnershipDescription.DISABLED_DESCR;
        
        // Evaluate the macro
        return func.evaluate(job, nodeDescription);
    }
           
    private static enum OwnershipFunction {
        JOB_OWNER(true),
        JOB_OWNER_EMAIL(true),
        JOB_COOWNERS(true),
        JOB_COOWNERS_EMAILS(true),
        NODE_OWNER(false),
        NODE_OWNER_EMAIL(false),
        NODE_COOWNERS(false),
        NODE_COOWNERS_EMAILS(false);
        
        private final boolean isJob;
        
        private OwnershipFunction(boolean isJob) {
            this.isJob = isJob;
        }
        
        public boolean isJobFunction() {
            return isJob;
        }
        
        public boolean isNodeFunction() {
            return !isJob;
        }
        
        public String evaluate(OwnershipDescription job, OwnershipDescription node) throws IOException {
            if (isJobFunction() && !job.isOwnershipEnabled()) {
                throw new IOException("Job Ownership is disabled");
            }
            if (isNodeFunction() && !node.isOwnershipEnabled()) {
                throw new IOException("Node Ownership is disabled");
            }
            
            switch(this) {
                case JOB_OWNER:
                    return OwnershipDescriptionHelper.getOwnerID(job);
                case JOB_OWNER_EMAIL:
                    return OwnershipDescriptionHelper.getOwnerEmail(job);
                case JOB_COOWNERS:
                    return OwnershipDescriptionHelper.getCoOwnerIDs(job);
                case JOB_COOWNERS_EMAILS:
                    return OwnershipDescriptionHelper.getCoOwnerEmails(job);   
                case NODE_OWNER:
                    return OwnershipDescriptionHelper.getOwnerID(node);
                case NODE_OWNER_EMAIL:
                    return OwnershipDescriptionHelper.getOwnerEmail(node);
                case NODE_COOWNERS:
                    return OwnershipDescriptionHelper.getCoOwnerIDs(node);
                case NODE_COOWNERS_EMAILS:
                    return OwnershipDescriptionHelper.getCoOwnerEmails(node); 
                default:
                    throw new IOException(this+" ownership function is not implemented");
            }
        }
    }
}
