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
package com.synopsys.arc.jenkins.plugins.ownership.jobs;

import hudson.Extension;
import hudson.matrix.MatrixConfiguration;
import hudson.model.Action;
import hudson.model.Job;
import java.util.Collection;
import static java.util.Collections.singleton;
import java.util.LinkedList;
import jenkins.model.TransientActionFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Creates a "Manage Ownership" action for jobs.
 * Action will be available for all top-level job items. 
 * Matrix configurations will be ignored.
 * @author Oleg Nenashev
 */
@Extension
@Restricted(NoExternalUse.class)
public class OwnershipActionFactory extends TransientActionFactory<Job> {
    /**Empty actions collection for invalid project type*/
    private static final Collection<? extends Action> EMPTY_ACTIONS 
            = new LinkedList<Action>();
    
    @Override
    public Collection<? extends Action> createFor(Job target) {
        return (target instanceof MatrixConfiguration) 
                ? EMPTY_ACTIONS : singleton(new JobOwnerJobAction(target));
    }    

    @Override
    public Class<Job> type() {
        return Job.class;
    }
}
