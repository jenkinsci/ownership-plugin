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
package com.synopsys.arc.jenkins.plugins.ownership;

import com.synopsys.arc.jenkins.plugins.ownership.extensions.ItemOwnershipPolicy;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.OwnershipLayoutFormatterProvider;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.item_ownership_policy.AssignCreatorPolicy;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.item_ownership_policy.DropOwnershipPolicy;
import com.synopsys.arc.jenkins.plugins.ownership.security.itemspecific.ItemSpecificSecurity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.ExtensionList;
import hudson.Plugin;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.PermissionScope;
import hudson.tasks.MailAddressResolver;
import hudson.util.FormValidation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Contains global actions and configurations.
 * @since 0.0.1
 * @author Oleg Nenashev
 */
public class OwnershipPlugin extends Plugin {
    
    public static final String LOG_PREFIX="[OwnershipPlugin] - ";
    private static final Logger LOGGER = Logger.getLogger(OwnershipPlugin.class.getName());

    public static final String FAST_RESOLVER_ID="Fast resolver for UI (recommended)";
    
    private static final PermissionGroup PERMISSIONS = new PermissionGroup(OwnershipPlugin.class, Messages._OwnershipPlugin_ManagePermissions_Title());    
    public static final Permission MANAGE_ITEMS_OWNERSHIP = new Permission(PERMISSIONS, "Jobs", Messages._OwnershipPlugin_ManagePermissions_JobDescription(), Permission.CONFIGURE, PermissionScope.ITEM);
    public static final Permission MANAGE_SLAVES_OWNERSHIP = new Permission(PERMISSIONS, "Nodes", Messages._OwnershipPlugin_ManagePermissions_SlaveDescription(), Permission.CONFIGURE, PermissionScope.COMPUTER);
     
    private boolean requiresConfigureRights;
    
    /**
     * @deprecated Replaced by {@link ItemOwnershipPolicy}
     */
    @Deprecated
    private transient boolean assignOnCreate;
    private final List<OwnershipAction> pluginActions = new ArrayList<OwnershipAction>();
    public String mailResolverClassName;
    private ItemSpecificSecurity defaultJobsSecurity;
    private OwnershipPluginConfiguration configuration;
    
    /**
     * @deprecated Use {@link #getInstance()} instead
     */
    @Deprecated
    @SuppressFBWarnings(value = "NM_METHOD_NAMING_CONVENTION", justification = "deprecated")
    public static OwnershipPlugin Instance() {
        return getInstance();
    }
    
    public static OwnershipPlugin getInstance() {
        Jenkins j = Jenkins.getInstance();
        OwnershipPlugin plugin = j != null ? j.getPlugin(OwnershipPlugin.class) : null;
        if (plugin == null) { // Fail horribly
            // TODO: throw a graceful error
            throw new IllegalStateException("Cannot get the plugin's instance. Jenkins or the plugin have not been initialized yet");
        }
        return plugin;
    }
    
    @Override 
    public void start() throws Exception {
	load();
        reinitActionsList();
	Jenkins.getActiveInstance().getActions().addAll(pluginActions);
    }

    @Override
    protected void load() throws IOException {
        super.load();
        
        // Migration to 1.5.0: Check ItemOwnershipPolicy
        if (configuration == null) {
            
            ItemOwnershipPolicy itemOwnershipPolicy = (assignOnCreate) 
                    ? new AssignCreatorPolicy() : new DropOwnershipPolicy();
            configuration = new OwnershipPluginConfiguration(itemOwnershipPolicy);
            
            save();
        }
    }
       
    public boolean isRequiresConfigureRights() {
        return requiresConfigureRights;
    }

    /**
     * @deprecated This method is deprecated since 0.5
     * @return {@code true} if the Item ownership policy is an instance of
     * {@link AssignCreatorPolicy}.
     */
    @Deprecated
    public boolean isAssignOnCreate() {
        return (getConfiguration().getItemOwnershipPolicy() instanceof AssignCreatorPolicy);
    }

    @CheckForNull
    public ItemSpecificSecurity getDefaultJobsSecurity() {
        return defaultJobsSecurity;
    }
    
    public OwnershipPluginConfiguration getConfiguration() {
        return configuration;
    }
     
    /**
     * Gets descriptor of ItemSpecificProperty.
     * Required for jelly.
     * @return Descriptor
     */
    @Nonnull
    public ItemSpecificSecurity.ItemSpecificDescriptor getItemSpecificDescriptor() {
        return ItemSpecificSecurity.DESCRIPTOR;
    }

    /**
     * {@link OwnershipPlugin} initialization for test suites.
     * @param requiresConfigureRights
     * @param mailResolverClassName
     * @param defaultJobsSecurity
     * @param configuration
     * @throws IOException 
     */
    public void configure(boolean requiresConfigureRights, String mailResolverClassName, 
            ItemSpecificSecurity defaultJobsSecurity, 
            OwnershipPluginConfiguration configuration) throws IOException {
        this.requiresConfigureRights = requiresConfigureRights;
        this.mailResolverClassName = mailResolverClassName;
        this.defaultJobsSecurity = defaultJobsSecurity;
        this.configuration = configuration;
        
        reinitActionsList();
	save();
        Jenkins.getActiveInstance().getActions().addAll(pluginActions);
    }

    @Override 
    public void configure(StaplerRequest req, JSONObject formData)
	    throws IOException, ServletException, Descriptor.FormException {
	Jenkins.getActiveInstance().getActions().removeAll(pluginActions);
        requiresConfigureRights = formData.getBoolean("requiresConfigureRights");
        
        // Configurations
        configuration = req.bindJSON(OwnershipPluginConfiguration.class, formData);              
              
        if (formData.containsKey("enableResolverRestrictions")) {
            JSONObject mailResolversConf = formData.getJSONObject("enableResolverRestrictions");
            mailResolverClassName = hudson.Util.fixEmptyAndTrim(mailResolversConf.getString("mailResolverClassName"));
        } else {
            mailResolverClassName = null;
        }
        
        if (formData.containsKey("defaultJobsSecurity")) {
            this.defaultJobsSecurity = getItemSpecificDescriptor().newInstance(req, formData.getJSONObject("defaultJobsSecurity"));
        }
        
        reinitActionsList();
	save();
        Jenkins.getActiveInstance().getActions().addAll(pluginActions);
    }
   
    private void reinitActionsList() {
        pluginActions.clear();
    }
    
    //TODO: clarify the default value
    @Nonnull
    public static String getDefaultOwner() {
        User current = User.current();       
        return current != null ? current.getId() : "";
    }
    
    public boolean hasMailResolverRestriction() {
        return mailResolverClassName != null;
    }

    @CheckForNull
    public String getMailResolverClassName() {
        return mailResolverClassName;
    }
    
    /**
     * Gets the configured {@link OwnershipLayoutFormatterProvider}.
     * @since 0.5
     * @return Ownership Layout Formatter to be used
     */
    public @Nonnull OwnershipLayoutFormatterProvider getOwnershipLayoutFormatterProvider() {
        //TODO: replace by the extension point
        return OwnershipLayoutFormatterProvider.DEFAULT_PROVIDER;
    } 
    
    public FormValidation doCheckUser(@QueryParameter String userId) {
        userId = Util.fixEmptyAndTrim(userId);
        if (userId == null) {
            return FormValidation.error("Field is empty. Field will be ignored");
        }
        
        User usr = User.get(userId, false, null);
        if (usr == null) {
            return FormValidation.warning("User " + userId + " is not registered in Jenkins");
        }
       
        return FormValidation.ok();
    }
    
    /**
     * Resolves e-mail using resolvers and global configuration.
     * @param user A user to be used
     * @return A e-mail string or null (if resolution fails)
     */
    @CheckForNull
    public String resolveEmail(User user) {
        try {
            if (hasMailResolverRestriction()) {
                if (mailResolverClassName.equals(FAST_RESOLVER_ID)) {
                    return MailAddressResolver.resolveFast(user);
                } else {
                    Class<MailAddressResolver> resolverClass = (Class<MailAddressResolver>)Class.forName(mailResolverClassName);
                    MailAddressResolver res = MailAddressResolver.all().get(resolverClass);
                    if (res != null) {
                        return res.findMailAddressFor(user);
                    }
                }
            } 
        } catch (ClassNotFoundException ex) {
            // Do nothing - fallback do default handler
        }
        //TODO: ClassCastException (bug)
        //TODO: methods above should log errors
        
        return MailAddressResolver.resolve(user);
    }
    
    @Nonnull
    public Collection<String> getPossibleMailResolvers() {
        ExtensionList<MailAddressResolver> extensions = MailAddressResolver.all();
        List<String> items =new ArrayList<String>(extensions.size());
        items.add(FAST_RESOLVER_ID);
        for (MailAddressResolver resolver : extensions) {
            items.add(resolver.getClass().getCanonicalName());
        }
        return items;
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
