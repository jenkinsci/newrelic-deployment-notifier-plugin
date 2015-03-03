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

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.Secret;
import org.jenkinsci.plugins.newrelicnotifier.api.NewRelicClient;
import org.jenkinsci.plugins.newrelicnotifier.api.NewRelicClientImpl;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.List;

/**
 * Notifies a New Relic instance about deployment.
 */
public class NewRelicDeploymentNotifier extends Notifier {

    private final NewRelicClient client = new NewRelicClientImpl();

    private final List<DeploymentNotificationBean> notifications;

    @DataBoundConstructor
    public NewRelicDeploymentNotifier(List<DeploymentNotificationBean> notifications) {
        super();
        this.notifications = notifications;
    }

    public List<DeploymentNotificationBean> getNotifications() {
        return notifications;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        EnvVars envVars = build.getEnvironment(listener);
        envVars.overrideAll(build.getBuildVariables());

        boolean result = true;
        for (DeploymentNotificationBean n : getNotifications()) {
            UsernamePasswordCredentials credentials = DeploymentNotificationBean.getCredentials(build.getProject(), n.getApiKey(), client.getApiEndpoint());
            if (credentials == null) {
                listener.getLogger().println("Invalid credentials for Application ID: " + n.getApplicationId());
                result = false;
            } else {
                if (client.sendNotification(
                        Secret.toString(credentials.getPassword()),
                        n.getApplicationId(),
                        n.getDescription(envVars),
                        n.getRevision(envVars),
                        n.getChangelog(envVars),
                        n.getUser(envVars))) {
                    listener.getLogger().println("Notified New Relic. Application ID: " + n.getApplicationId());
                } else {
                    listener.getLogger().println("Failed to notify New Relic. Application ID: " + n.getApplicationId());
                    result = false;
                }
            }
        }
        return result;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "New Relic Deployment Notifications";
        }
    }
}
