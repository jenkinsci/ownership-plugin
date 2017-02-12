/*
 * The MIT License
 *
 * Copyright 2017 Ksenia Nenasheva <ks.nenasheva@gmail.com>.
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
package org.jenkinsci.plugins.ownership.integrations.securityinspector;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.util.AbstractOwnershipHelper;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.ownership.model.OwnershipHelperLocator;
import org.jenkinsci.plugins.securityinspector.util.JenkinsHelper;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.StaplerRequest;

public class OwnerFilter {
    
    /**
     * Include regex string.
     */
    @CheckForNull
    private final String includeRegex;

    /**
     * Compiled include pattern from the includeRegex string.
     */
    @CheckForNull
    private transient Pattern includePattern;
    
    /**
     * Constructs empty filter.
     */
    public OwnerFilter() {
        this.includeRegex = null;
        this.includePattern = null;
    }
    
    @Restricted(NoExternalUse.class)
    public OwnerFilter(@Nonnull StaplerRequest req)
            throws Descriptor.FormException, ServletException {
        if (req.getParameter("useincluderegex") != null) {
            includeRegex = Util.nullify(req.getParameter("_.includeRegex"));
            if (includeRegex == null) {
                includePattern = null;
            } else {
                includePattern = Pattern.compile(includeRegex);
            }
        } else {
            includeRegex = null;
            includePattern = null;
        }
    }
    
    @Nonnull
    @Restricted(NoExternalUse.class)
    public List<TopLevelItem> doFilter(User owner) {
        
        final SortedSet<String> names = new TreeSet<String>();
        
        final Jenkins jenkins = JenkinsHelper.getInstanceOrFail();
        final List<Item> allItems = jenkins.getAllItems(Item.class);
        String itemName;
        
        for (Item item : allItems) {
            AbstractOwnershipHelper<Item> located = OwnershipHelperLocator.locate(item);
            if (located == null) {
                continue;
            }
            
            OwnershipDescription ownershipDescription = located.getOwnershipDescription(item);
            if (ownershipDescription.isOwner(owner, true)) {
                itemName = item.getFullName();
                names.add(itemName);
            }
        }
        
        if (includePattern != null) {
            for (Item item : allItems) {
                AbstractOwnershipHelper<Item> located = OwnershipHelperLocator.locate(item);
                if (located == null) {
                    continue;
                }
            
                itemName = item.getFullName();
                OwnershipDescription ownershipDescription = located.getOwnershipDescription(item);
                if (ownershipDescription.isOwner(owner, true) 
                        & includePattern.matcher(itemName).matches()) {
                    names.add(itemName);
                }
            }
        } else {
            for (Item item : allItems) {
                AbstractOwnershipHelper<Item> located = OwnershipHelperLocator.locate(item);
                if (located == null) {
                    continue;
                }
            
                OwnershipDescription ownershipDescription = located.getOwnershipDescription(item);
                if (ownershipDescription.isOwner(owner, true)) {
                    itemName = item.getFullName();
                    names.add(itemName);
                }
            }
        }
        
        List<TopLevelItem> items = new ArrayList<TopLevelItem>();
        for (String n : names) {
            TopLevelItem item = jenkins.getItemByFullName(n, TopLevelItem.class);
            if (item != null) {
                items.add(item);
            }
        }
        
        return items;
    }
    
    @CheckForNull
    public Pattern getIncludePattern() {
        return includePattern;
    }

    @CheckForNull
    public String getIncludeRegex() {
        return includeRegex;
    } 
}
