package com.synopsys.arc.jenkins.plugins.ownership.util;

import hudson.security.SecurityRealm;
import java.util.Comparator;
import jenkins.model.IdStrategy;
import jenkins.model.Jenkins;


public class IdStrategyComparator implements Comparator<String> {

    private final SecurityRealm securityRealm;
    private final IdStrategy groupIdStrategy;
    private final IdStrategy userIdStrategy;

    public IdStrategyComparator() {
        securityRealm = Jenkins.get().getSecurityRealm();
        groupIdStrategy = securityRealm.getGroupIdStrategy();
        userIdStrategy = securityRealm.getUserIdStrategy();
    }

    public int compare(String o1, String o2) {
        int r = userIdStrategy.compare(o1, o2);
        if (r == 0) {
            r = groupIdStrategy.compare(o1, o2);
        }
        return r;
    }
}
