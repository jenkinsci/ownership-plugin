/*
 * The MIT License
 *
 * Copyright 2014 Oleg Nenashev, Synopsys Inc.
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

package com.synopsys.arc.jenkins.plugins.ownership;

import com.synopsys.arc.jenkins.plugins.ownership.extensions.ItemOwnershipPolicy;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.ownership.util.mail.MailOptions;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.ownership.config.DisplayOptions;
import org.jenkinsci.plugins.ownership.config.InheritanceOptions;
import org.jenkinsci.plugins.ownership.model.runs.OwnershipRunListener;
import org.jenkinsci.plugins.ownership.util.environment.EnvSetupOptions;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Configuration of {@link OwnershipPlugin}.
 * @author Oleg Nenashev
 * @since 0.5
 */
public class OwnershipPluginConfiguration 
        implements Describable<OwnershipPluginConfiguration> {

    private final ItemOwnershipPolicy itemOwnershipPolicy;
    private final @CheckForNull MailOptions mailOptions;
    private final @CheckForNull DisplayOptions displayOptions;
    private final @CheckForNull InheritanceOptions inheritanceOptions;
    
    /**
     * Enforces the injection of ownership variables in {@link OwnershipRunListener}.
     * Null means the injection is disabled.
     * @since 0.6
     */
    private final @CheckForNull EnvSetupOptions globalEnvSetupOptions;

    @Deprecated
    public OwnershipPluginConfiguration(@Nonnull ItemOwnershipPolicy itemOwnershipPolicy, 
            @CheckForNull MailOptions mailOptions, EnvSetupOptions globalEnvSetupOptions,
            @Nonnull DisplayOptions displayOptions) {
        this(itemOwnershipPolicy, mailOptions, globalEnvSetupOptions, displayOptions, InheritanceOptions.DEFAULT);
    }
    
    @DataBoundConstructor
    public OwnershipPluginConfiguration(@Nonnull ItemOwnershipPolicy itemOwnershipPolicy, 
            @CheckForNull MailOptions mailOptions, EnvSetupOptions globalEnvSetupOptions,
            @Nonnull DisplayOptions displayOptions, @Nonnull InheritanceOptions inheritanceOptions) {
        this.itemOwnershipPolicy = itemOwnershipPolicy;
        this.mailOptions = mailOptions;
        this.globalEnvSetupOptions = globalEnvSetupOptions;
        this.displayOptions = displayOptions;
        this.inheritanceOptions = inheritanceOptions;
    }
    
    @Deprecated
    public OwnershipPluginConfiguration(@Nonnull ItemOwnershipPolicy itemOwnershipPolicy) {
        this (itemOwnershipPolicy, MailOptions.DEFAULT, null);
    }
    
    @Deprecated
    public OwnershipPluginConfiguration(@Nonnull ItemOwnershipPolicy itemOwnershipPolicy, 
            @Nonnull MailOptions mailOptions, EnvSetupOptions globalEnvSetupOptions) {
        this(itemOwnershipPolicy, mailOptions, globalEnvSetupOptions, DisplayOptions.DEFAULT);
    }

    public @Nonnull ItemOwnershipPolicy getItemOwnershipPolicy() {
        return itemOwnershipPolicy != null ? itemOwnershipPolicy : ItemOwnershipPolicy.getDefault();
    }

    public @Nonnull MailOptions getMailOptions() {
        return mailOptions != null ? mailOptions : MailOptions.DEFAULT;
    }

    /**
     * Gets {@link OwnershipPlugin}'s display options.
     * @return Display options. If the configuration has not been specified after the plugin update,
     *      {@link DisplayOptions#DEFAULT} will be returned.
     * @since 0.8
     */
    public @Nonnull DisplayOptions getDisplayOptions() {
        return displayOptions != null ? displayOptions : DisplayOptions.DEFAULT;
    }

    public InheritanceOptions getInheritanceOptions() {
        return inheritanceOptions != null ? inheritanceOptions : InheritanceOptions.DEFAULT;
    }
    
    /**
     * @return Global environment inject options. Null - global setup is disabled
     * @since 0.6
     */
    public @CheckForNull EnvSetupOptions getGlobalEnvSetupOptions() {
        return globalEnvSetupOptions;
    }

    @Override
    public Descriptor<OwnershipPluginConfiguration> getDescriptor() {
        return DESCRIPTOR;
    }
    
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @Symbol("ownershipConfig")
    public static class DescriptorImpl extends Descriptor<OwnershipPluginConfiguration> {

        @Override
        public String getDisplayName() {
            return "N/A";
        }
    }
    
    
    /**
     * Gets the {@link OwnershipPlugin} configuration.
     * @return The plugin configuration if available.
     * @throws IllegalStateException Jenkins instance is not ready
     * @since 0.8
     */
    public static OwnershipPluginConfiguration get() {
        return OwnershipPlugin.getInstance().getConfiguration();
    }
}
