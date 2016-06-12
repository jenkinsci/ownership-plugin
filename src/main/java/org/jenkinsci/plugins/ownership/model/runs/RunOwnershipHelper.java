/*
 * The MIT License
 *
 * Copyright 2015 Oleg Nenashev <o.v.nenashev@gmail.com>.
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

package org.jenkinsci.plugins.ownership.model.runs;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPluginConfiguration;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerJobProperty;
import com.synopsys.arc.jenkins.plugins.ownership.nodes.OwnerNodeProperty;
import com.synopsys.arc.jenkins.plugins.ownership.util.AbstractOwnershipHelper;
import com.synopsys.arc.jenkins.plugins.ownership.util.UserStringFormatter;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.model.Run;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.ownership.model.OwnershipInfo;

/**
 * Helper for {@link Run} ownership management.
 * @author Oleg Nenashev
 * @since 0.6
 */
public class RunOwnershipHelper extends AbstractOwnershipHelper<Run> {

    private static final RunOwnershipHelper instance = new RunOwnershipHelper();

    public static RunOwnershipHelper getInstance() {
        return instance;
    }

    @Override
    public String getItemTypeName(Run item) {
        return "run";
    }
    
    @Override
    public String getItemDisplayName(Run item) {
        return item.getFullDisplayName();
    }

    @Override
    public String getItemURL(Run item) {
        return item.getUrl();
    }

    @Override
    public OwnershipDescription getOwnershipDescription(Run item) {
        return JobOwnerHelper.Instance.getOwnershipDescription(item.getParent());
    }    

    @Override
    public OwnershipInfo getOwnershipInfo(Run item) {
        return JobOwnerHelper.Instance.getOwnershipInfo(item.getParent());
    }
    
    /**
     * Environment setup according to wrapper configurations.
     * @param build Input build
     * @param target Target destination (output)
     * @param listener Build listener
     * @param injectJobOwnership Injects Job ownership info
     * @param injectNodeOwnership Injects Node ownership info
     * @since 0.6 Method is public
     */
    public static void setUp (@Nonnull AbstractBuild build, @Nonnull Map<String, String> target, 
            @CheckForNull BuildListener listener, 
            boolean injectJobOwnership, boolean injectNodeOwnership) {
        if (injectJobOwnership) { 
            OwnershipDescription descr = JobOwnerHelper.Instance.getOwnershipDescription(build.getParent());
            getVariables(descr, target, "JOB");
        }
             
        if (injectNodeOwnership) {
            Node node = build.getBuiltOn();
            if (node == null) {
                assert false : "Cannot retrieve node of the build. Probably, it has been deleted";
                if (listener != null) {
                    listener.error("Cannot retrieve node of the build. "
                            + "Probably, it has been deleted. Variables will be ignored.");
                }
                return; // Ignore the error
            }
            
            OwnerNodeProperty prop = node.getNodeProperties().get(OwnerNodeProperty.class);
            OwnershipDescription descr = prop!=null ? prop.getOwnership() : OwnershipDescription.DISABLED_DESCR;
            getVariables(descr, target, "NODE");
        }
    }
    
    //TODO: Replace by OwnershipDescriptionHelper
    private static void getVariables(OwnershipDescription descr, Map<String, String> target, String prefix) {      
        target.put(prefix+"_OWNER", descr.hasPrimaryOwner() ? descr.getPrimaryOwnerId() : "");
        String ownerEmail = UserStringFormatter.formatEmail(descr.getPrimaryOwnerId());  
        target.put(prefix+"_OWNER_EMAIL", ownerEmail != null ? ownerEmail : "");
        
        StringBuilder coowners=new StringBuilder(target.get(prefix+"_OWNER"));   
        StringBuilder coownerEmails= new StringBuilder(target.get(prefix+"_OWNER_EMAIL"));
        for (String userId : descr.getCoownersIds()) {
            if (coowners.length() != 0) {
                coowners.append(",");
            }
            coowners.append(userId);
            
            String coownerEmail = UserStringFormatter.formatEmail(userId);
            if (coownerEmail != null) {
                //TODO: may corrupt logic on empty owner
                if (coownerEmails.length() != 0) {
                    coownerEmails.append(",");
                }
                coownerEmails.append(coownerEmail);
            }       
        }
        target.put(prefix+"_COOWNERS", coowners.toString());
        target.put(prefix+"_COOWNERS_EMAILS", coownerEmails.toString());     
    }

    @Override
    public boolean isDisplayOwnershipSummaryBox(Run item) {
        return super.isDisplayOwnershipSummaryBox(item) &&
               !OwnershipPlugin.getInstance().getConfiguration().getDisplayOptions().isHideRunOwnership();
    }
}
