package org.jenkinsci.plugins.ownership.config;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipAction;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPluginConfiguration;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.ItemOwnershipPolicy;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.OwnershipLayoutFormatterProvider;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.item_ownership_policy.AssignCreatorPolicy;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.item_ownership_policy.DropOwnershipPolicy;
import com.synopsys.arc.jenkins.plugins.ownership.security.itemspecific.ItemSpecificSecurity;
import hudson.Extension;
import hudson.Util;
import hudson.XmlFile;
import hudson.model.User;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Extension
public class OwnershipGlobalConfiguration extends GlobalConfiguration {

    /**
     * @deprecated Replaced by {@link ItemOwnershipPolicy}
     */
    @Deprecated
    private transient boolean assignOnCreate;
    @Deprecated
    private transient List<OwnershipAction> pluginActions = new ArrayList<>();

    private String mailResolverClassName;
    private ItemSpecificSecurity defaultJobsSecurity;
    private OwnershipPluginConfiguration configuration;
    private boolean requiresConfigureRights;

    @Nonnull
    public static OwnershipGlobalConfiguration get() {
        return (OwnershipGlobalConfiguration) Jenkins.get().getDescriptorOrDie(OwnershipGlobalConfiguration.class);
    }

    public OwnershipGlobalConfiguration() {
        load();
    }

    @Override
    protected XmlFile getConfigFile() {
        // Compatibility with the old plugin implementation
        return new XmlFile(new File(Jenkins.get().getRootDir(),"ownership.xml"));
    }

    private void readResolve() {

        // Migration to 1.5.0: Check ItemOwnershipPolicy
        if (configuration == null) {
            ItemOwnershipPolicy itemOwnershipPolicy = (assignOnCreate)
                    ? new AssignCreatorPolicy() : new DropOwnershipPolicy();
            configuration = new OwnershipPluginConfiguration(itemOwnershipPolicy);
        }
    }

    @DataBoundSetter
    public void setConfiguration(OwnershipPluginConfiguration configuration) {
        this.configuration = configuration;
    }

    @DataBoundSetter
    public void setDefaultJobsSecurity(ItemSpecificSecurity defaultJobsSecurity) {
        this.defaultJobsSecurity = defaultJobsSecurity;
    }

    @DataBoundSetter
    public void setRequiresConfigureRights(boolean requiresConfigureRights) {
        this.requiresConfigureRights = requiresConfigureRights;
    }

    @DataBoundSetter
    public void setMailResolverClassName(String mailResolverClassName) {
        this.mailResolverClassName = mailResolverClassName;
    }

    @CheckForNull
    public String getMailResolverClassName() {
        return mailResolverClassName;
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

    public FormValidation doCheckUser(@QueryParameter String userId) {
        userId = Util.fixEmptyAndTrim(userId);
        if (userId == null) {
            return FormValidation.error("Field is empty. Field will be ignored");
        }

        User usr = User.getById(userId, false);
        if (usr == null) {
            return FormValidation.warning("User " + userId + " is not registered in Jenkins");
        }

        return FormValidation.ok();
    }

    /**
     * Gets the configured {@link OwnershipLayoutFormatterProvider}.
     * @return Ownership Layout Formatter to be used
     */
    public @Nonnull OwnershipLayoutFormatterProvider getOwnershipLayoutFormatterProvider() {
        //TODO: replace by the extension point
        return OwnershipLayoutFormatterProvider.DEFAULT_PROVIDER;
    }
}
