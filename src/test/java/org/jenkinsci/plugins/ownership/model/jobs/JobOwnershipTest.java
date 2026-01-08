package org.jenkinsci.plugins.ownership.model.jobs;

import com.synopsys.arc.jenkins.plugins.ownership.extensions.item_ownership_policy.DropOwnershipPolicy;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerJobProperty;
import hudson.model.Job;
import hudson.model.User;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.ownership.config.PreserveOwnershipPolicy;
import org.jenkinsci.plugins.ownership.test.util.OwnershipPluginConfigurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author cpuydebois
 */
@WithJenkins
class JobOwnershipTest {

    private static final String JOB_CONFIG_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
              <actions/>
              <description/>
              <keepDependencies>false</keepDependencies>
              <properties>
                <com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerJobProperty plugin="ownership@0.9.1">
                  <ownership>
                    <ownershipEnabled>true</ownershipEnabled>
                    <primaryOwnerId>the.owner</primaryOwnerId>
                    <coownersIds class="sorted-set">
                      <string>secondary.owner</string>
                    </coownersIds>
                  </ownership>
                </com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerJobProperty>
              </properties>
              <scm class="hudson.scm.NullSCM"/>
              <canRoam>true</canRoam>
              <disabled>false</disabled>
              <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
              <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
              <triggers/>
              <concurrentBuild>false</concurrentBuild>
              <builders/>
              <publishers/>
              <buildWrappers/>
            </project>""";

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void shouldSupportDropOwnershipPolicy() throws Exception {
        InputStream jobConfigIS = null;
        try {
            // Configure the policy
            OwnershipPluginConfigurer.forJenkinsRule(j)
                    .withItemOwnershipPolicy(new DropOwnershipPolicy())
                    .configure();

            jobConfigIS = new ByteArrayInputStream(JOB_CONFIG_XML.getBytes());
            j.jenkins.createProjectFromXML("job-dsl-generated", jobConfigIS);

            Job job = j.jenkins.getItemByFullName("job-dsl-generated", Job.class);
            assertThat("Cannot locate job 'job-dsl-generated'", job, notNullValue());
            JobOwnerJobProperty ownerProperty = JobOwnerHelper.getOwnerProperty(job);
            assertThat("Property should still be herer",  ownerProperty, notNullValue());
            assertThat("Ownership should be disabled according to DropOwnershipPolicy",
                    ownerProperty.getOwnership().isOwnershipEnabled(), equalTo(false));
            assertThat("There should be no Owner",
                    ownerProperty.getOwnership().getPrimaryOwnerId(), equalTo(User.getUnknown().getId()));
        } finally {
            IOUtils.closeQuietly(jobConfigIS);
        }

    }

    @Test
    void shouldSupportPreserveJobOwnershipPolicy() throws Exception {
        InputStream jobConfigIS = null;
        try {
            // Configure the policy
            OwnershipPluginConfigurer.forJenkinsRule(j)
                    .withItemOwnershipPolicy(new PreserveOwnershipPolicy())
                    .configure();

            jobConfigIS = new ByteArrayInputStream(JOB_CONFIG_XML.getBytes());
            j.jenkins.createProjectFromXML("job-dsl-generated", jobConfigIS);

            Job job = j.jenkins.getItemByFullName("job-dsl-generated", Job.class);
            assertThat("Cannot locate job 'job-dsl-generated'", job, notNullValue());
            JobOwnerJobProperty ownerProperty = JobOwnerHelper.getOwnerProperty(job);
            assertThat("Property should be preserved by PreserveJobOwnershipPolicy",  ownerProperty, notNullValue());
            assertThat("Ownership should still be enabled according to PreserveJobOwnershipPolicy",
                    ownerProperty.getOwnership().isOwnershipEnabled(), equalTo(true));
            assertThat("Owner should still be 'the.owner'",
                    ownerProperty.getOwnership().getPrimaryOwnerId(), equalTo("the.owner"));
        } finally {
            IOUtils.closeQuietly(jobConfigIS);
        }

    }
}
