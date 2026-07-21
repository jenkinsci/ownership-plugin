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
package org.jenkinsci.plugins.ownership.security.folderspecific;

import com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty;
import hudson.model.Item;
import hudson.security.Permission;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-style coverage for {@link FolderSpecificSecurity}.
 */
@WithJenkins
class FolderSpecificSecurityTest {

    private JenkinsRule r;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        r = rule;
    }

    @Test
    void nullMatrixConstructor_createsEmptyProperty() {
        FolderSpecificSecurity security = new FolderSpecificSecurity(null);
        assertThat(security.getPermissionsMatrix(), notNullValue());
        assertThat(security.getPermissionsMatrix().getGrantedPermissionEntries().entrySet(), empty());
    }

    @Test
    void getDescriptor_and_displayName() {
        FolderSpecificSecurity security = new FolderSpecificSecurity(null);
        assertThat(security.getDescriptor(), sameInstance(FolderSpecificSecurity.DESCRIPTOR));
        assertThat(FolderSpecificSecurity.DESCRIPTOR.getDisplayName(), is("Folder-specific security"));
    }

    @Test
    void clone_copiesGrantedSidsIndependently() {
        FolderSpecificSecurity original = matrixWith(Item.READ, "alice");
        FolderSpecificSecurity copy = original.clone();

        assertThat(copy, notNullValue());
        assertThat(copy, not(sameInstance(original)));
        assertThat(copy.getPermissionsMatrix(), not(sameInstance(original.getPermissionsMatrix())));

        assertTrue(copy.getPermissionsMatrix().getGrantedPermissionEntries().get(Item.READ)
                .stream().anyMatch(e -> "alice".equals(e.getSid())));
    }

    @Test
    void newInstance_withoutPermissionsMatrix_yieldsEmptySecurity() throws Exception {
        FolderSpecificSecurity security = FolderSpecificSecurity.DESCRIPTOR.newInstance(
                (org.kohsuke.stapler.StaplerRequest2) null, new JSONObject());
        assertThat(security, notNullValue());
        assertThat(security.getPermissionsMatrix().getGrantedPermissionEntries().entrySet(), empty());
    }

    static FolderSpecificSecurity matrixWith(Permission permission, String sid) {
        Map<Permission, Set<String>> permissions = new HashMap<>();
        permissions.put(permission, Collections.singleton(sid));
        return new FolderSpecificSecurity(new AuthorizationMatrixProperty(permissions));
    }
}
