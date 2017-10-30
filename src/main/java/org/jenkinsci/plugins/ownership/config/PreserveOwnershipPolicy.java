package org.jenkinsci.plugins.ownership.config;

import com.synopsys.arc.jenkins.plugins.ownership.Messages;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.ItemOwnershipPolicy;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.ItemOwnershipPolicyDescriptor;
import com.synopsys.arc.jenkins.plugins.ownership.util.AbstractOwnershipHelper;
import hudson.Extension;
import hudson.model.Item;
import org.jenkinsci.plugins.ownership.model.OwnershipHelperLocator;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

/**
 * A policy, which keeps the previous job's ownership.
 * @author cpuydebois
 * @since TODO
 */
public class PreserveOwnershipPolicy extends ItemOwnershipPolicy {

    @DataBoundConstructor
    public PreserveOwnershipPolicy() {
    }

    @Override
    public OwnershipDescription onCreated(@Nonnull Item item) {
        AbstractOwnershipHelper<Item> helper = OwnershipHelperLocator.locate(item);
        if (helper != null) {
            return helper.getOwnershipDescription(item);
        }
        return null;
    }

    @Extension
    public static class DescriptorImpl extends ItemOwnershipPolicyDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.ItemOwnershipPolicy_PreserveOwnershipPolicy_displayName();
        }
    }
}
