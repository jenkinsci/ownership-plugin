/*
 * The MIT License
 *
 * Copyright (c) 2018 Victor Martinez
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
package org.jenkinsci.plugins.ownership.recipients;

import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerJobProperty;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.emailext.EmailRecipientUtils;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import hudson.plugins.emailext.plugins.recipients.RecipientProviderUtilities;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.io.PrintStream;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Victor Martinez
 */
@Extension(optional = true)
public class OwnershipRecipientProvider extends RecipientProvider {

    @DataBoundConstructor
    public OwnershipRecipientProvider() {

    }

    @Override
    public void addRecipients(final ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc) {
        final class Debug implements RecipientProviderUtilities.IDebug {
            private final ExtendedEmailPublisherDescriptor descriptor
                    = Jenkins.getActiveInstance().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

            private final PrintStream logger = context.getListener().getLogger();

            public void send(final String format, final Object... args) {
                descriptor.debug(logger, format, args);
            }
        }
        final Debug debug = new Debug();
        // looking for Upstream build.
        Run<?, ?> cur = context.getRun();
        Cause.UpstreamCause upc = cur.getCause(Cause.UpstreamCause.class);
        Job<?, ?> p = null;
        while (upc != null) {
            // UpstreamCause.getUpStreamProject() returns the full name, so use getItemByFullName
            p = (Job<?, ?>) Jenkins.getActiveInstance().getItemByFullName(upc.getUpstreamProject());
            if (p == null) {
                context.getListener().getLogger().print("There is a break in the project linkage, could not retrieve upstream project information");
                break;
            }
        }
        try {
            addJobOwner(p, to, cc, bcc, env, context, debug);
        } catch (MessagingException e) {
            Logger.getLogger(OwnershipRecipientProvider.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private static void addJobOwner(Job<?,?> job, Set<InternetAddress> to,
                                                  Set<InternetAddress> cc, Set<InternetAddress> bcc, EnvVars env, final ExtendedEmailPublisherContext context, RecipientProviderUtilities.IDebug debug) throws MessagingException {
        final String owner = job.getProperty(JobOwnerJobProperty.class).getOwnership().getPrimaryOwnerEmail();
        if (owner != null) {
            EmailRecipientUtils.addAddressesFromRecipientList(to, cc, bcc, EmailRecipientUtils.getRecipientList(context, owner), env, context.getListener());
        }
    }

    //return User.get(id, false, (Map)null);

    @Extension
    @Symbol("owners")
    public static final class DescriptorImpl extends RecipientProviderDescriptor {

        @Override
        public String getDisplayName() {
            return "Owners";
        }

    }
}
