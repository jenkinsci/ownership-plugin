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

package org.jenkinsci.plugins.ownership.util.mail;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Stores mailing options for {@link OwnershipPlugin}.
 * @author Oleg Nenashev
 * @since 0.6
 */
public class MailOptions implements Describable<MailOptions> {
    
    private final @CheckForNull String contactOwnersSubjectTemplate;
    private final @CheckForNull String contactOwnersBodyTemplate;
    private final boolean contactOwnersLinkDisabled;
    
    private final @CheckForNull String contactAdminsSubjectTemplate;
    private final @CheckForNull String contactAdminsBodyTemplate;
    private final boolean contactAdminsLinkDisabled;   
    
    private final @CheckForNull String emailListSeparator;
    private final @CheckForNull String adminsContactEmail;
    private final boolean hideOwnerAndCoOwnerEmails;

    private static final String DEFAULT_LIST_SEPARATOR = ";";
     
    public static final MailOptions DEFAULT = new MailOptions();

    private MailOptions() {
        this(null,null,null,null,null,DEFAULT_LIST_SEPARATOR);
    }
    
    @DataBoundConstructor
    public MailOptions(
            String contactOwnersSubjectTemplate, String contactOwnersBodyTemplate, 
            boolean contactOwnersLinkDisabled, 
            String contactAdminsSubjectTemplate, String contactAdminsBodyTemplate, 
            boolean contactAdminsLinkDisabled,
            String adminsContactEmail, String emailListSeparator,
            boolean hideOwnerAndCoOwnerEmails) {
        this.contactOwnersSubjectTemplate = contactOwnersSubjectTemplate;
        this.contactOwnersBodyTemplate = contactOwnersBodyTemplate;
        this.contactOwnersLinkDisabled = contactOwnersLinkDisabled;
        
        this.contactAdminsSubjectTemplate = contactAdminsSubjectTemplate;
        this.contactAdminsBodyTemplate = contactAdminsBodyTemplate;
        this.contactAdminsLinkDisabled = contactAdminsLinkDisabled;
        
        this.emailListSeparator = emailListSeparator;
        this.adminsContactEmail = adminsContactEmail;   
        this.hideOwnerAndCoOwnerEmails = hideOwnerAndCoOwnerEmails;
    }
    
    @Deprecated
    public MailOptions(
            String contactOwnersSubjectTemplate, String contactOwnersBodyTemplate, 
            boolean contactOwnersLinkDisabled, 
            String contactAdminsSubjectTemplate, String contactAdminsBodyTemplate, 
            boolean contactAdminsLinkDisabled,
            String adminsContactEmail, String emailListSeparator) {
        this(contactOwnersSubjectTemplate, contactOwnersBodyTemplate, contactOwnersLinkDisabled, 
             contactAdminsSubjectTemplate, contactAdminsBodyTemplate, contactAdminsLinkDisabled, 
             adminsContactEmail, emailListSeparator, false);
    }
  
    @Deprecated
    public MailOptions(
            String contactOwnersSubjectTemplate, String contactOwnersBodyTemplate, 
            String contactAdminsSubjectTemplate, String contactAdminsBodyTemplate, 
             String adminsContactEmail, String emailListSeparator) {
        this(contactOwnersSubjectTemplate, contactOwnersBodyTemplate, false, 
             contactAdminsSubjectTemplate, contactAdminsBodyTemplate, false, 
             adminsContactEmail, emailListSeparator);
    }
    
    public @Nonnull String getContactOwnersSubjectTemplate() {
        return contactOwnersSubjectTemplate != null 
                ? contactOwnersSubjectTemplate 
                : Messages.contactOwnersSubjectTemplate_default();
    }
    
    public @Nonnull String getContactOwnersBodyTemplate() {
        return contactOwnersBodyTemplate != null 
                ? contactOwnersBodyTemplate 
                : Messages.contactOwnersBodyTemplate_default();
    }

    public @Nonnull String getContactAdminsSubjectTemplate() {
        return contactAdminsSubjectTemplate != null 
                ? contactAdminsSubjectTemplate 
                : Messages.contactAdminsSubjectTemplate_default();
    }
    
    public @Nonnull String getContactAdminsBodyTemplate() {
        return contactAdminsBodyTemplate != null 
                ? contactAdminsBodyTemplate 
                : Messages.contactAdminsBodyTemplate_default();
    }
  
    public @Nonnull String getEmailListSeparator() {
        return emailListSeparator != null ? emailListSeparator : DEFAULT_LIST_SEPARATOR;
    }

    public @CheckForNull String getAdminsContactEmail() {
        return adminsContactEmail;
    }

    /**
     * @return Indicates that contact admins link should not be displayed
     * @since 0.7 
     */
    public boolean isContactAdminsLinkDisabled() {
        return contactAdminsLinkDisabled;
    }

    /**
     * @return Indicates that contact owners link should not be displayed
     * @since 0.7 
     */
    public boolean isContactOwnersLinkDisabled() {
        return contactOwnersLinkDisabled;
    }

    /**
     * Check if displaying e-mails of item owners is disabled.
     * @return {@code} true if the links should not be visualized.
     * @since 0.8
     */
    public boolean isHideOwnerAndCoOwnerEmails() {
        return hideOwnerAndCoOwnerEmails;
    }
   
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }
  
    public static class DescriptorImpl extends Descriptor<MailOptions> {
        
        @Override
        public String getDisplayName() {
            return "N/A";
        }
    }
}
