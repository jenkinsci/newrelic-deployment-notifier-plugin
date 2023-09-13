/*
 * The MIT License
 *
 * Copyright (c) 2015, Mads Mohr Christensen
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
package org.jenkinsci.plugins.newrelicnotifier;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.apache.commons.lang.RandomStringUtils;
import org.jenkinsci.plugins.newrelicnotifier.api.HttpClientStub;
import org.jenkinsci.plugins.newrelicnotifier.api.NewRelicClientStub;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NewRelicDeploymentNotifierTest {

    protected String username = RandomStringUtils.randomAlphabetic(10);
    protected String password = RandomStringUtils.randomAlphabetic(10);
    protected String credentialsId = RandomStringUtils.randomAlphabetic(4);

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    private NewRelicClientStub client;

    @Before
    public void setup() throws IOException {
        client = spy(new NewRelicClientStub());
        client.setHttpClient(new HttpClientStub());

        UsernamePasswordCredentialsImpl credentials = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, credentialsId, "test", username, password);

        CredentialsProvider.lookupStores(jenkinsRule.getInstance()).iterator().next()
                .addCredentials(Domain.global(), credentials);
    }

    @Test
    public void missingNotifications() throws Exception {
        FreeStyleProject p = jenkinsRule.createFreeStyleProject();
        List<DeploymentNotificationBean> notifications = new ArrayList<>();

        NewRelicDeploymentNotifier notifier = spy(new NewRelicDeploymentNotifier(notifications));
        when(notifier.getClient()).thenReturn(client);

        p.getPublishersList().add(notifier);
        FreeStyleBuild build = p.scheduleBuild2(0).get();

        jenkinsRule.assertBuildStatus(Result.FAILURE, build);
        jenkinsRule.assertLogContains("FATAL: Missing notifications!", build);
    }

    @Test
    public void freestyleProjectNotifier() throws Exception {
        FreeStyleProject p = jenkinsRule.createFreeStyleProject();

        List<DeploymentNotificationBean> notifications = new ArrayList<>();
        DeploymentNotificationBean notificationBean = new DeploymentNotificationBean(
                credentialsId,
                "applicationId",
                "description",
                "revision",
                "changelog",
                "commit",
                "deeplink",
                "user",
                "",
                "deploymentId",
                "deploymentType",
                "groupId",
                "timestamp",
                "version",
                false
        );
        notifications.add(notificationBean);

        NewRelicDeploymentNotifier notifier = spy(new NewRelicDeploymentNotifier(notifications));
        when(notifier.getClient()).thenReturn(client);

        p.getPublishersList().add(notifier);
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        jenkinsRule.assertBuildStatus(Result.SUCCESS, b);
    }

    @Test
    public void freestyleProjectNotifier2() throws Exception {
        FreeStyleProject p = jenkinsRule.createFreeStyleProject();

        List<DeploymentNotificationBean> notifications = new ArrayList<>();
        DeploymentNotificationBean notificationBean = new DeploymentNotificationBean(
                credentialsId,
                "applicationId",
                "description",
                "revision",
                "changelog",
                "commit",
                "deeplink",
                "user",
                "entityGuid",
                "deploymentId",
                "deploymentType",
                "groupId",
                "timestamp",
                "version",
                false
        );
        notifications.add(notificationBean);

        NewRelicDeploymentNotifier notifier = spy(new NewRelicDeploymentNotifier(notifications));
        when(notifier.getClient()).thenReturn(client);

        p.getPublishersList().add(notifier);
        FreeStyleBuild b = p.scheduleBuild2(0).get();
        jenkinsRule.assertBuildStatus(Result.SUCCESS, b);
    }
}
