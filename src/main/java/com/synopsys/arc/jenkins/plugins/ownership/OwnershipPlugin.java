/*
 * The MIT License
 *
 * Copyright 2013 Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
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
import com.synopsys.arc.jenkins.plugins.ownership.extensions.item_ownership_policy.AssignCreatorPolicy;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.item_ownership_policy.DropOwnershipPolicy;
import com.synopsys.arc.jenkins.plugins.ownership.security.itemspecific.ItemSpecificSecurity;
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
 * @author Oleg Nenashev <nenashev@synopsys.com>
 */
public class OwnershipPlugin extends Plugin {
    
    public static final String LOG_PREFIX="[OwnershipPlugin] - ";
    public static final String FAST_RESOLVER_ID="Fast resolver for UI (recommended)";
    
    private static final PermissionGroup PERMISSIONS = new PermissionGroup(OwnershipPlugin.class, Messages._OwnershipPlugin_ManagePermissions_Title());    
    public static final Permission MANAGE_ITEMS_OWNERSHIP = new Permission(PERMISSIONS, "Jobs", Messages._OwnershipPlugin_ManagePermissions_JobDescription(), Permission.CONFIGURE, PermissionScope.ITEM);
    public static final Permission MANAGE_SLAVES_OWNERSHIP = new Permission(PERMISSIONS, "Nodes", Messages._OwnershipPlugin_ManagePermissions_SlaveDescription(), Permission.CONFIGURE, PermissionScope.COMPUTER);
     
    private boolean requiresConfigureRights;
    
    /**
     * @deprecated Replaced by {@link ItemOwnershipPolicy}
     */
    private transient boolean assignOnCreate;
    private final List<OwnershipAction> pluginActions = new ArrayList<OwnershipAction>();
    public String mailResolverClassName;
    private ItemSpecificSecurity defaultJobsSecurity;
    private OwnershipPluginConfiguration configuration;
    
    /**
     * @deprecated Use {@link #getInstance()} instead
     */
    public static OwnershipPlugin Instance() {
        return getInstance();
    }
    
    public static OwnershipPlugin getInstance() {
        Plugin plugin = Jenkins.getInstance().getPlugin(OwnershipPlugin.class);
        return plugin != null ? (OwnershipPlugin)plugin : null;
    }
    
    @Override 
    public void start() throws Exception {
	load();
        reinitActionsList();
	Hudson.getInstance().getActions().addAll(pluginActions);
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
     * @return true if the {@link #itemOwnershipPolicy} is an instance of
     * {@link AssignCreatorPolicy}.
     */
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
    
    @Override 
    public void configure(StaplerRequest req, JSONObject formData)
	    throws IOException, ServletException, Descriptor.FormException {
	Hudson.getInstance().getActions().removeAll(pluginActions);
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
        Hudson.getInstance().getActions().addAll(pluginActions);
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
                    return res.findMailAddressFor(user);
                }
            } 
        } catch (ClassNotFoundException ex) {
            // Do nothing - fallback do default handler
        }
        
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
}
