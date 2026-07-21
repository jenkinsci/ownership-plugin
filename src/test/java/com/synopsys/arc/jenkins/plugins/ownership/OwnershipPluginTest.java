/*
 * The MIT License
 *
 * Copyright 2026
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synopsys.arc.jenkins.plugins.ownership.extensions.OwnershipLayoutFormatterProvider;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.item_ownership_policy.AssignCreatorPolicy;
import com.synopsys.arc.jenkins.plugins.ownership.extensions.item_ownership_policy.DropOwnershipPolicy;
import com.synopsys.arc.jenkins.plugins.ownership.security.itemspecific.ItemSpecificSecurity;
import hudson.model.User;
import hudson.security.ACL;
import hudson.tasks.MailAddressResolver;
import hudson.util.FormValidation;
import java.lang.reflect.Proxy;
import java.util.Collection;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.ownership.security.folderspecific.FolderSpecificSecurity;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Coverage for OwnershipPlugin APIs outside the defaultJobsSecurity binding path.
 */
@WithJenkins
class OwnershipPluginTest {

    private JenkinsRule r;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        r = rule;
        OwnershipPluginConfigurer.forJenkinsRule(r).configure();
    }

    @Test
    void doCheckUser_empty_unknown_and_known() throws Exception {
        OwnershipPlugin plugin = OwnershipPlugin.getInstance();
        User.getById("alice", true);

        FormValidation empty = plugin.doCheckUser("  ");
        assertThat(empty.kind, is(FormValidation.Kind.ERROR));

        FormValidation unknown = plugin.doCheckUser("nosuchuser");
        assertThat(unknown.kind, is(FormValidation.Kind.WARNING));
        assertThat(unknown.getMessage(), containsString("nosuchuser"));

        FormValidation known = plugin.doCheckUser("alice");
        assertThat(known.kind, is(FormValidation.Kind.OK));
    }

    @Test
    void resolveEmail_withoutRestriction_usesDefaultResolver() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withMailResolverClassName(null)
                .configure();
        User alice = User.getById("alice", true);
        alice.addProperty(new hudson.tasks.Mailer.UserProperty("alice@example.com"));

        String email = OwnershipPlugin.getInstance().resolveEmail(alice);
        assertThat(email, is("alice@example.com"));
        assertFalse(OwnershipPlugin.getInstance().hasMailResolverRestriction());
        assertThat(OwnershipPlugin.getInstance().getMailResolverClassName(), nullValue());
    }

    @Test
    void resolveEmail_withFastResolverRestriction() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withMailResolverClassName(OwnershipPlugin.FAST_RESOLVER_ID)
                .configure();
        User alice = User.getById("alice", true);
        alice.addProperty(new hudson.tasks.Mailer.UserProperty("alice-fast@example.com"));

        assertTrue(OwnershipPlugin.getInstance().hasMailResolverRestriction());
        assertThat(OwnershipPlugin.getInstance().getMailResolverClassName(), is(OwnershipPlugin.FAST_RESOLVER_ID));
        assertThat(OwnershipPlugin.getInstance().resolveEmail(alice), is("alice-fast@example.com"));
    }

    @Test
    void resolveEmail_withUnknownClass_fallsBackToDefault() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withMailResolverClassName("com.example.DoesNotExistMailResolver")
                .configure();
        User alice = User.getById("alice", true);
        alice.addProperty(new hudson.tasks.Mailer.UserProperty("alice-fallback@example.com"));

        assertThat(OwnershipPlugin.getInstance().resolveEmail(alice), is("alice-fallback@example.com"));
    }

    @Test
    void resolveEmail_withCustomResolverExtension() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withMailResolverClassName(TestMailResolver.class.getCanonicalName())
                .configure();
        User alice = User.getById("alice", true);

        assertThat(OwnershipPlugin.getInstance().resolveEmail(alice), is("from-test-resolver@example.com"));
    }

    @Test
    void getPossibleMailResolvers_includesFastIdAndExtensions() {
        Collection<String> resolvers = OwnershipPlugin.getInstance().getPossibleMailResolvers();
        assertThat(resolvers, hasItem(OwnershipPlugin.FAST_RESOLVER_ID));
        assertThat(resolvers, hasItem(TestMailResolver.class.getCanonicalName()));
    }

    @Test
    void getDefaultOwner_currentUserOrEmpty() throws Exception {
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());

        try (hudson.security.ACLContext ignored = ACL.as2(Jenkins.ANONYMOUS2)) {
            assertThat(OwnershipPlugin.getDefaultOwner(), is(""));
        }

        User alice = User.getById("alice", true);
        try (hudson.security.ACLContext ignored = ACL.as(alice)) {
            assertThat(OwnershipPlugin.getDefaultOwner(), is("alice"));
        }
    }

    @Test
    void programmaticConfigure_setsFlagsAndDescriptorAccessors() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .requireConfigurePermissions(true)
                .withItemOwnershipPolicy(new AssignCreatorPolicy())
                .configure();

        OwnershipPlugin plugin = OwnershipPlugin.getInstance();
        assertTrue(plugin.isRequiresConfigureRights());
        assertTrue(plugin.isAssignOnCreate());
        assertThat(plugin.getItemSpecificDescriptor(), sameInstance(ItemSpecificSecurity.DESCRIPTOR));
        assertThat(
                plugin.getOwnershipLayoutFormatterProvider(),
                sameInstance(OwnershipLayoutFormatterProvider.DEFAULT_PROVIDER));
        assertThat(OwnershipPlugin.getLogger(), notNullValue());
        assertThat(plugin.getConfiguration().getItemOwnershipPolicy(), instanceOf(AssignCreatorPolicy.class));

        OwnershipPluginConfigurer.forJenkinsRule(r)
                .requireConfigurePermissions(false)
                .withItemOwnershipPolicy(new DropOwnershipPolicy())
                .configure();
        assertFalse(OwnershipPlugin.getInstance().isRequiresConfigureRights());
        assertFalse(OwnershipPlugin.getInstance().isAssignOnCreate());
    }

    @Test
    void configure_setsRequiresConfigureRightsAndMailResolver() throws Exception {
        OwnershipPlugin plugin = OwnershipPlugin.getInstance();
        JSONObject form = new JSONObject();
        form.put("requiresConfigureRights", true);
        JSONObject restriction = new JSONObject();
        restriction.put("mailResolverClassName", OwnershipPlugin.FAST_RESOLVER_ID);
        form.put("enableResolverRestrictions", restriction);

        plugin.configure(stubRequestKeepingConfiguration(), form);

        assertTrue(plugin.isRequiresConfigureRights());
        assertTrue(plugin.hasMailResolverRestriction());
        assertThat(plugin.getMailResolverClassName(), is(OwnershipPlugin.FAST_RESOLVER_ID));
    }

    @Test
    void configure_withoutMailRestriction_clearsResolver() throws Exception {
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withMailResolverClassName(OwnershipPlugin.FAST_RESOLVER_ID)
                .configure();
        assertTrue(OwnershipPlugin.getInstance().hasMailResolverRestriction());

        JSONObject form = new JSONObject();
        form.put("requiresConfigureRights", false);
        OwnershipPlugin.getInstance().configure(stubRequestKeepingConfiguration(), form);

        assertFalse(OwnershipPlugin.getInstance().hasMailResolverRestriction());
        assertThat(OwnershipPlugin.getInstance().getMailResolverClassName(), nullValue());
    }

    @Test
    void configure_withoutDefaultJobsSecurity_keepsExistingDefaults() throws Exception {
        ItemSpecificSecurity defaults = new ItemSpecificSecurity(null);
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withDefaultJobsSecurity(defaults)
                .configure();
        assertThat(OwnershipPlugin.getInstance().getDefaultJobsSecurity(), sameInstance(defaults));

        JSONObject form = new JSONObject();
        form.put("requiresConfigureRights", false);
        // no defaultJobsSecurity key
        OwnershipPlugin.getInstance().configure(stubRequestKeepingConfiguration(), form);

        assertThat(OwnershipPlugin.getInstance().getDefaultJobsSecurity(), sameInstance(defaults));
    }

    @Test
    void getFolderSpecificDescriptor() {
        assertThat(
                OwnershipPlugin.getInstance().getFolderSpecificDescriptor(),
                sameInstance(FolderSpecificSecurity.DESCRIPTOR));
    }

    @Test
    void configure_withoutDefaultFoldersSecurity_keepsExistingDefaults() throws Exception {
        FolderSpecificSecurity defaults = new FolderSpecificSecurity(null);
        OwnershipPluginConfigurer.forJenkinsRule(r)
                .withDefaultFoldersSecurity(defaults)
                .configure();
        assertThat(OwnershipPlugin.getInstance().getDefaultFoldersSecurity(), sameInstance(defaults));

        JSONObject form = new JSONObject();
        form.put("requiresConfigureRights", false);
        OwnershipPlugin.getInstance().configure(stubRequestKeepingConfiguration(), form);

        assertThat(OwnershipPlugin.getInstance().getDefaultFoldersSecurity(), sameInstance(defaults));
    }

    @Test
    void deprecatedInstance_matchesGetInstance() {
        assertThat(OwnershipPlugin.Instance(), sameInstance(OwnershipPlugin.getInstance()));
    }

    private static StaplerRequest2 stubRequestKeepingConfiguration() {
        OwnershipPluginConfiguration conf = OwnershipPlugin.getInstance().getConfiguration();
        return (StaplerRequest2) Proxy.newProxyInstance(
                StaplerRequest2.class.getClassLoader(),
                new Class<?>[] {StaplerRequest2.class},
                (proxy, method, args) -> {
                    if ("bindJSON".equals(method.getName())
                            && args != null
                            && args.length == 2
                            && args[0] == OwnershipPluginConfiguration.class) {
                        return conf;
                    }
                    if ("toString".equals(method.getName())) {
                        return "stubRequestKeepingConfiguration";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    @TestExtension
    public static class TestMailResolver extends MailAddressResolver {
        @Override
        public String findMailAddressFor(User u) {
            return "from-test-resolver@example.com";
        }
    }
}
