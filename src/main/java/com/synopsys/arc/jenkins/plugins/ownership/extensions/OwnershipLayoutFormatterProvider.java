/*
 * The MIT License
 *
 * Copyright 2014 Oleg Nenashev
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

package com.synopsys.arc.jenkins.plugins.ownership.extensions;

import com.synopsys.arc.jenkins.plugins.ownership.util.ui.OwnershipLayoutFormatter;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.Run;
import javax.annotation.Nonnull;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * The extension creates {@link OwnershipLayoutFormatter}s for various object types.
 * @since 0.5
 * @author Oleg Nenashev
 */
@Restricted(NoExternalUse.class)
public abstract class OwnershipLayoutFormatterProvider {
    
    public static final OwnershipLayoutFormatterProvider DEFAULT_PROVIDER = new DefaultProvider();
    private static final OwnershipLayoutFormatter<Job<?,?>> DEFAULT_JOB_FORMATTER = new OwnershipLayoutFormatter.DefaultJobFormatter<Job<?,?>>();
    private static final OwnershipLayoutFormatter<Node> DEFAULT_NODE_FORMATTER = new OwnershipLayoutFormatter.DefaultJobFormatter<Node>();
    private static final OwnershipLayoutFormatter<Run> DEFAULT_RUN_FORMATTER = new OwnershipLayoutFormatter.DefaultJobFormatter<Run>();
    
    public @Nonnull OwnershipLayoutFormatter<Job<?,?>> getLayoutFormatter(Job<?,?> job) {
        return DEFAULT_JOB_FORMATTER;
    }
    
    public @Nonnull OwnershipLayoutFormatter<Node> getLayoutFormatter(Node node) {
        return DEFAULT_NODE_FORMATTER;
    }
    
    public @Nonnull OwnershipLayoutFormatter<Run> getLayoutFormatter(Run run) {
        return DEFAULT_RUN_FORMATTER;
    }
    
    
    public static final class DefaultProvider extends OwnershipLayoutFormatterProvider {        
    }
}
