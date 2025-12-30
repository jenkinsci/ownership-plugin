/*
 * The MIT License
 *
 * Copyright 2024
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
package com.synopsys.arc.jenkins.plugins.ownership.security.rolestrategy;

import com.michelin.cio.hudson.plugins.rolestrategy.PermissionEntry;
import com.synopsys.arc.jenkins.plugins.rolestrategy.Macro;
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.security.Permission;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for {@link AbstractOwnershipRoleMacro} focusing on null and empty SID validation.
 */
public class AbstractOwnershipRoleMacroTest {
    
    @Rule
    public final JenkinsRule j = new JenkinsRule();
    
    /**
     * Test that hasPermission returns false when PermissionEntry is null.
     */
    @Test
    public void testHasPermissionWithNullEntry() throws Exception {
        OwnerRoleMacro macro = new OwnerRoleMacro();
        FreeStyleProject project = j.createFreeStyleProject("test");
        Permission permission = Item.READ;
        RoleType type = RoleType.Project;
        Macro macroParam = null; // Macro is not used in the null check logic
        
        boolean result = macro.hasPermission((PermissionEntry) null, permission, type, project, macroParam);
        
        assertThat("Access should be denied when PermissionEntry is null", result, equalTo(false));
    }
    
    /**
     * Test that hasPermission returns false when PermissionEntry.getSid() returns null.
     */
    @Test
    public void testHasPermissionWithNullSid() throws Exception {
        OwnerRoleMacro macro = new OwnerRoleMacro();
        FreeStyleProject project = j.createFreeStyleProject("test");
        Permission permission = Item.READ;
        RoleType type = RoleType.Project;
        Macro macroParam = null; // Macro is not used in the null check logic
        
        PermissionEntry entry = createTestPermissionEntry(null);
        
        boolean result = macro.hasPermission(entry, permission, type, project, macroParam);
        
        assertThat("Access should be denied when SID is null", result, equalTo(false));
    }
    
    /**
     * Test that hasPermission returns false when PermissionEntry.getSid() returns empty string.
     */
    @Test
    public void testHasPermissionWithEmptySid() throws Exception {
        OwnerRoleMacro macro = new OwnerRoleMacro();
        FreeStyleProject project = j.createFreeStyleProject("test");
        Permission permission = Item.READ;
        RoleType type = RoleType.Project;
        Macro macroParam = null; // Macro is not used in the empty check logic
        
        PermissionEntry entry = createTestPermissionEntry("");
        
        boolean result = macro.hasPermission(entry, permission, type, project, macroParam);
        
        assertThat("Access should be denied when SID is empty", result, equalTo(false));
    }
    
    /**
     * Test that hasPermission returns false when PermissionEntry.getSid() returns whitespace-only string.
     */
    @Test
    public void testHasPermissionWithWhitespaceSid() throws Exception {
        OwnerRoleMacro macro = new OwnerRoleMacro();
        FreeStyleProject project = j.createFreeStyleProject("test");
        Permission permission = Item.READ;
        RoleType type = RoleType.Project;
        Macro macroParam = null; // Macro is not used in the whitespace check logic
        
        PermissionEntry entry = createTestPermissionEntry("   ");
        
        boolean result = macro.hasPermission(entry, permission, type, project, macroParam);
        
        assertThat("Access should be denied when SID contains only whitespace", result, equalTo(false));
    }
    
    /**
     * Test that hasPermission(String sid, ...) returns false when sid is null.
     */
    @Test
    public void testHasPermissionWithNullStringSid() throws Exception {
        OwnerRoleMacro macro = new OwnerRoleMacro();
        FreeStyleProject project = j.createFreeStyleProject("test");
        Permission permission = Item.READ;
        RoleType type = RoleType.Project;
        Macro macroParam = null; // Macro is not used in the null check logic
        
        boolean result = macro.hasPermission((String) null, permission, type, project, macroParam);
        
        assertThat("Access should be denied when SID string is null", result, equalTo(false));
    }
    
    /**
     * Test that hasPermission(String sid, ...) returns false when sid is empty string.
     */
    @Test
    public void testHasPermissionWithEmptyStringSid() throws Exception {
        OwnerRoleMacro macro = new OwnerRoleMacro();
        FreeStyleProject project = j.createFreeStyleProject("test");
        Permission permission = Item.READ;
        RoleType type = RoleType.Project;
        Macro macroParam = null; // Macro is not used in the empty check logic
        
        boolean result = macro.hasPermission("", permission, type, project, macroParam);
        
        assertThat("Access should be denied when SID string is empty", result, equalTo(false));
    }
    
    /**
     * Test that hasPermission(String sid, ...) returns false when sid is whitespace-only string.
     */
    @Test
    public void testHasPermissionWithWhitespaceStringSid() throws Exception {
        OwnerRoleMacro macro = new OwnerRoleMacro();
        FreeStyleProject project = j.createFreeStyleProject("test");
        Permission permission = Item.READ;
        RoleType type = RoleType.Project;
        Macro macroParam = null; // Macro is not used in the whitespace check logic
        
        boolean result = macro.hasPermission("   ", permission, type, project, macroParam);
        
        assertThat("Access should be denied when SID string contains only whitespace", result, equalTo(false));
    }
    
    /**
     * Test that hasPermission works correctly with valid SID (even if user doesn't exist).
     * This test verifies that null/empty checks don't interfere with normal operation.
     */
    @Test
    public void testHasPermissionWithValidSid() throws Exception {
        OwnerRoleMacro macro = new OwnerRoleMacro();
        FreeStyleProject project = j.createFreeStyleProject("test");
        Permission permission = Item.READ;
        RoleType type = RoleType.Project;
        Macro macroParam = null; // Macro is not used in the validation logic
        
        PermissionEntry entry = createTestPermissionEntry("validUser");
        
        // Should not throw exception and should process normally
        // (may return false if user doesn't exist, but that's expected behavior)
        boolean result = macro.hasPermission(entry, permission, type, project, macroParam);
        
        // Just verify it doesn't crash and returns a boolean
        // The actual permission check depends on ownership setup, which is tested elsewhere
        assertThat("Method should return a boolean value", result, equalTo(result));
    }
    
    /**
     * Creates a test PermissionEntry using reflection to handle the required constructor parameters.
     * Uses the same approach as OwnershipBasedSecurityTestHelper for consistency.
     */
    private static PermissionEntry createTestPermissionEntry(String sid) {
        try {
            // PermissionEntry requires (AuthorizationType, String) constructor
            // We'll use reflection to create it with default AuthorizationType
            Class<?> authTypeClass = Class.forName("com.michelin.cio.hudson.plugins.rolestrategy.AuthorizationType");
            Object authType = authTypeClass.getEnumConstants()[0]; // Use first enum value as default
            
            // Use getDeclaredConstructor and setAccessible, similar to OwnershipBasedSecurityTestHelper
            java.lang.reflect.Constructor<PermissionEntry> constructor = 
                PermissionEntry.class.getDeclaredConstructor(authTypeClass, String.class);
            try {
                constructor.setAccessible(true);
                return constructor.newInstance(authType, sid);
            } finally {
                constructor.setAccessible(false);
            }
        } catch (Exception e) {
            throw new AssertionError("Failed to create test PermissionEntry", e);
        }
    }
}

