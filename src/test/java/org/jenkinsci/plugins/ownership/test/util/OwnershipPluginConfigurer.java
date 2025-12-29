/*
 * The MIT License
 *
 * Copyright (c) 2015 Oleg Nenashev.
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
package org.jenkinsci.plugins.ownership.test.util;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPluginConfiguration;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.ItemOwnershipPolicy;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.item_ownership_policy.DropOwnershipPolicy;
import com.synopsys.arc.jenkins.plugins.ownership.security.itemspecific.ItemSpecificSecurity;
import org.jenkinsci.plugins.ownership.security.folderspecific.FolderSpecificSecurity;
import java.io.IOException;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.ownership.config.DisplayOptions;
import org.jenkinsci.plugins.ownership.config.InheritanceOptions;
import org.jenkinsci.plugins.ownership.util.environment.EnvSetupOptions;
import org.jenkinsci.plugins.ownership.util.mail.MailOptions;
import org.jvnet.hudson.test.JenkinsRule;
/**
 * Manages configuration of {@link OwnershipPlugin}.
 * @author Oleg Nenashev
 */
public class OwnershipPluginConfigurer {
    
    private final Jenkins jenkins;
    
    // Parameters. All items are nullable
    private boolean requiresConfigurePermissions;
    private String mailResolverClassName;
    private ItemSpecificSecurity defaultJobsSecurity;
    private FolderSpecificSecurity defaultFoldersSecurity;
    private ItemOwnershipPolicy itemOwnershipPolicy;
    private MailOptions mailOptions;
    private DisplayOptions displayOptions;
    private EnvSetupOptions globalEnvSetupOptions;
    private InheritanceOptions inheritanceOptions;
      
    private OwnershipPluginConfigurer(Jenkins jenkins) {
        this.jenkins = jenkins;
        // Default policy in the plugin
        this.itemOwnershipPolicy = new DropOwnershipPolicy();
    }
    
    public static OwnershipPluginConfigurer forJenkinsRule(JenkinsRule rule) {
        return forInstance(rule.jenkins);
    }
    
    public static OwnershipPluginConfigurer forInstance(Jenkins jenkins) {
        return new OwnershipPluginConfigurer(jenkins);
    }
    
    public OwnershipPluginConfigurer requireConfigurePermissions(boolean requiresConfigurePermissions) {
        this.requiresConfigurePermissions = requiresConfigurePermissions;
        return this;
    }
    
    public OwnershipPluginConfigurer withMailResolverClassName(String mailResolverClassName) {
        this.mailResolverClassName = mailResolverClassName;
        return this;
    }
    
    public OwnershipPluginConfigurer withDefaultJobsSecurity(ItemSpecificSecurity defaultJobsSecurity) {
        this.defaultJobsSecurity = defaultJobsSecurity;
        return this;
    }

    public OwnershipPluginConfigurer withDefaultFoldersSecurity(FolderSpecificSecurity defaultFoldersSecurity) {
        this.defaultFoldersSecurity = defaultFoldersSecurity;
        return this;
    }
    
    public OwnershipPluginConfigurer withItemOwnershipPolicy(@Nonnull ItemOwnershipPolicy itemOwnershipPolicy) {
        this.itemOwnershipPolicy = itemOwnershipPolicy;
        return this;
    }
    
    public OwnershipPluginConfigurer withDisplayOptions(DisplayOptions displayOptions) {
        this.displayOptions = displayOptions;
        return this;
    }
    
    public OwnershipPluginConfigurer withMailOptions(MailOptions mailOptions) {
        this.mailOptions = mailOptions;
        return this;
    }
    
    public OwnershipPluginConfigurer withGlobalEnvSetupOptions(EnvSetupOptions globalEnvSetupOptions) {
        this.globalEnvSetupOptions = globalEnvSetupOptions;
        return this;
    }
    
    public OwnershipPluginConfigurer withInheritanceOptions(InheritanceOptions inheritanceOptions) {
        this.inheritanceOptions = inheritanceOptions;
        return this;
    }
    
    public void configure() throws IOException {
        OwnershipPluginConfiguration conf = new OwnershipPluginConfiguration
                (itemOwnershipPolicy, mailOptions, globalEnvSetupOptions, displayOptions, inheritanceOptions);
        jenkins.getPlugin(OwnershipPlugin.class).configure
                (requiresConfigurePermissions, mailResolverClassName, defaultJobsSecurity, defaultFoldersSecurity, conf);
    }
}
