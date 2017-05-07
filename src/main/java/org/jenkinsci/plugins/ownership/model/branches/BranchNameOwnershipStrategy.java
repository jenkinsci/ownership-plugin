package org.jenkinsci.plugins.ownership.model.branches;

import hudson.Extension;
import hudson.Util;
import hudson.model.User;
import hudson.util.FormValidation;
import jenkins.branch.Branch;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.RegEx;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class BranchNameOwnershipStrategy extends BranchOwnershipStrategy {
    
    private Pattern pattern;
    private String ownerExpression;
    
    @DataBoundConstructor
    public BranchNameOwnershipStrategy(@RegEx String pattern, @Nonnull String ownerExpression) {
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
            return "By branch name";
        }
        
        public FormValidation doCheckPattern(@QueryParameter String value) {
            try {
                Pattern.compile(value);
            } catch (PatternSyntaxException ex) {
                return FormValidation.error("Invalid regex: %s", ex.getMessage());
            }
            
            return FormValidation.ok();
        }
        
        public FormValidation doCheckOwnerExpression(@QueryParameter String value) {
            return Util.fixEmptyAndTrim(value) != null ? FormValidation.ok() : FormValidation.warning("Ownership will be disabled");
        }
    }
}
