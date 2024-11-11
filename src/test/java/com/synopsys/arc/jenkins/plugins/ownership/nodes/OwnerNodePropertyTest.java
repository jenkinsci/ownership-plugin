/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
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
package com.synopsys.arc.jenkins.plugins.ownership.nodes;

import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import hudson.cli.CLICommandInvoker;
import hudson.cli.UpdateNodeCommand;
import hudson.model.Computer;
import hudson.model.Node;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

import static hudson.cli.CLICommandInvoker.Matcher.failedWith;
import static hudson.cli.CLICommandInvoker.Matcher.succeededSilently;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class OwnerNodePropertyTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Before
    public void setupSecurity() {
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        MockAuthorizationStrategy mas = new MockAuthorizationStrategy();
        mas.grant(Jenkins.ADMINISTER) // Implies MANAGE_SLAVES_OWNERSHIP.
                .everywhere()
                .to("admin");
        mas.grant(Computer.CONFIGURE, Jenkins.READ)
                .everywhere()
                .to("non-admin");
        r.jenkins.setAuthorizationStrategy(mas);
    }

    @Test
    @Issue("SECURITY-498")
    public void changeOwnerViaPost() throws Exception {
        String nodeName; // Computer#updateByXml replaces the existing node with a new instance, so we always need to look up the current instance.
        String nodeUrl;
        {
            Node n = r.createSlave();
            n.getNodeProperties().add(new OwnerNodeProperty(n, new OwnershipDescription(true, "admin", null)));
            nodeName = n.getNodeName();
            nodeUrl = n.toComputer().getUrl();
        }

        JenkinsRule.WebClient wc = r.createWebClient();
        wc.login("non-admin", "non-admin");
        WebRequest req = new WebRequest(wc.createCrumbedUrl(String.format("%sconfig.xml", nodeUrl)), HttpMethod.POST);
        req.setAdditionalHeader("Content-Type", "application/xml");
        req.setRequestBody(getNodeXml(nodeName, "admin"));
        wc.getPage(req);
        assertThat("Users should be able to configure jobs when ownership is unchanged",
                getPrimaryOwner(nodeName), is(equalTo("admin")));

        try {
            wc.login("non-admin", "non-admin");
            req.setRequestBody(getNodeXml(nodeName, "non-admin"));
            wc.getPage(req);
            fail("Users should not be able to configure job ownership without Manger Ownership/Jobs permissions");
        } catch (FailingHttpStatusCodeException e) {
            assertThat(getPrimaryOwner(nodeName), is(equalTo("admin")));
        }

        wc.login("admin", "admin");
        req.setRequestBody(getNodeXml(nodeName, "non-admin"));
        wc.getPage(req);
        assertThat("Users with Manage Ownership/Jobs permissions should be able to change ownership",
                getPrimaryOwner(nodeName), is(equalTo("non-admin")));
    }

    @Test
    @Issue("SECURITY-498")
    public void changeOwnerViaCLI() throws Exception {
        String nodeName;
        {
            Node n = r.createSlave();
            n.getNodeProperties().add(new OwnerNodeProperty(n, new OwnershipDescription(true, "admin", null)));
            nodeName = n.getNodeName();
        }

        CLICommandInvoker command = new CLICommandInvoker(r, new UpdateNodeCommand())
                .asUser("non-admin")
                .withArgs(nodeName)
                .withStdin(getNodeXmlAsStream(nodeName, "admin"));
        assertThat("Users without Overall/Administer permissions should not be able to configure nodes via CLI",
                command.invoke(), failedWith(1));
        assertThat(getPrimaryOwner(nodeName), is(equalTo("admin")));

        command.asUser("admin")
                .withArgs(nodeName)
                .withStdin(getNodeXmlAsStream(nodeName, "non-admin"));
        assertThat("Users with Overall/Administer permissions should be able to configure jobs via CLI", 
                command.invoke(), succeededSilently());
        assertThat(getPrimaryOwner(nodeName), is(equalTo("non-admin")));
    }

    private String getPrimaryOwner(String nodeName) {
        return r.jenkins.getNode(nodeName).getNodeProperties().get(OwnerNodeProperty.class).getOwnership().getPrimaryOwnerId();
    }

    private String getNodeXml(String nodeName, String ownerSid) {
        return String.format(NODE_XML_TEMPLATE, nodeName, ownerSid);
    }

    private InputStream getNodeXmlAsStream(String nodeName, String ownerSid) {
        return new ByteArrayInputStream(getNodeXml(nodeName, ownerSid).getBytes(StandardCharsets.UTF_8));
    }

    private static final String NODE_XML_TEMPLATE =
            "<?xml version='1.0' encoding='UTF-8'?>" +
            "<slave>" +
            "    <name>%s</name>" +
            "    <description/>" +
            "    <remoteFS>/tmp/dumbnode</remoteFS>" +
            "    <numExecutors>1</numExecutors>" +
            "    <mode>NORMAL</mode>" +
            "    <retentionStrategy class=\"hudson.slaves.RetentionStrategy$Always\"/>" +
            "    <launcher/>" +
            "    <label/>" +
            "    <nodeProperties>" +
            "        <com.synopsys.arc.jenkins.plugins.ownership.nodes.OwnerNodeProperty plugin=\"ownership@0.10.1-SNAPSHOT\">" +
            "            <ownership>" +
            "                <ownershipEnabled>true</ownershipEnabled>" +
            "                <primaryOwnerId>%s</primaryOwnerId>" +
            "                <coownersIds class=\"sorted-set\"/>" +
            "            </ownership>" +
            "            <nodeName>DumbNode</nodeName>" +
            "        </com.synopsys.arc.jenkins.plugins.ownership.nodes.OwnerNodeProperty>" +
            "    </nodeProperties>" +
            "</slave>";

}
