/*
 * The MIT License
 *
 * Copyright (c) 2017 Jordan Coll
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
package org.jenkinsci.plugins.ownership.model.branches;

import com.synopsys.arc.jenkins.plugins.ownership.Messages;
import hudson.Extension;
import hudson.Util;
import hudson.model.User;
import hudson.util.FormValidation;
import jenkins.branch.Branch;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.RegEx;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A {@link BranchOwnershipStrategy} that determines ownership by matching branch names against a {@link Pattern regular expression}.
 */
public class BranchNameOwnershipStrategy extends BranchOwnershipStrategy {
    
    private Pattern pattern;
    private String ownerExpression;
    
    /**
     * Constructs an instance.
     *
     * @param pattern         A {@link Pattern regular expression} against which branch names should be matched
     * @param ownerExpression A string representing the prospective owner. May contain references to capture groups in the pattern.
     *                        See {@link Matcher#appendReplacement(StringBuffer, String)} for syntax.
     * @throws PatternSyntaxException If the pattern's syntax is invalid
     */
    @DataBoundConstructor
    public BranchNameOwnershipStrategy(@RegEx String pattern, @Nonnull String ownerExpression) throws PatternSyntaxException {
        this.pattern = Pattern.compile(pattern);
        this.ownerExpression = ownerExpression;
    }
    
    public String getPattern() {
        return pattern.pattern();
    }
    
    public String getOwnerExpression() {
        return ownerExpression;
    }
    
    @Override
    @Nullable
    public String determineOwner(Branch branch) {
        Matcher matcher = pattern.matcher(branch.getName());
        
        if (matcher.matches()) {
            String prospectiveOwner = matcher.replaceAll(ownerExpression);
            
            if (User.get(prospectiveOwner, false, Collections.emptyMap()) != null) {
                return prospectiveOwner;
            }
        }
        
        return null;
    }
    
    @Extension
    public static class DescriptorImpl extends BranchOwnershipStrategy.BranchOwnershipStrategyDescriptor {
        
        @Override
        @Nonnull
        public String getDisplayName() {
            return Messages.BranchOwnership_Strategy_BranchNameOwnershipStrategy_DisplayName();
        }
        
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckPattern(@QueryParameter String value) {
            try {
                Pattern.compile(value);
            } catch (PatternSyntaxException ex) {
                return FormValidation.error(Messages.BranchOwnership_Strategy_BranchNameOwnershipStrategy_InvalidRegex(ex.getMessage()));
            }
            
            return FormValidation.ok();
        }
        
        @Restricted(NoExternalUse.class)
        public FormValidation doCheckOwnerExpression(@QueryParameter String value) {
            return Util.fixEmptyAndTrim(value) != null ? FormValidation.ok() : FormValidation.warning("Ownership will be disabled");
        }
    }
}
