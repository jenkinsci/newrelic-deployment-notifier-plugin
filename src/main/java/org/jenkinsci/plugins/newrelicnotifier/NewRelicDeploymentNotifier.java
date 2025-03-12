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
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.Secret;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.newrelicnotifier.api.NewRelicClient;
import org.jenkinsci.plugins.newrelicnotifier.api.NewRelicClientImpl;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.List;

/**
 * Notifies a New Relic instance about deployment.
 */
public class NewRelicDeploymentNotifier extends Notifier implements SimpleBuildStep {

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
        boolean result = true;

        if (build.getResult() == Result.FAILURE ||
            build.getResult() == Result.ABORTED) {
            listener.error("Build unsuccessful. Skipping New Relic Deployment notification.");
            return false;
        }

        if (getNotifications() == null || getNotifications().isEmpty()) {
            listener.fatalError("Missing notifications!");
            return false;
        }

        EnvVars envVars = build.getEnvironment(listener);
        envVars.overrideAll(build.getBuildVariables());

        NewRelicClient client = getClient();

        for (DeploymentNotificationBean n : getNotifications()) {
            UsernamePasswordCredentials credentials = DeploymentNotificationBean.getCredentials(build.getProject(), n.getApiKey(), client.getApiEndpoint(n.getEuropean()));
            if (credentials == null) {
                listener.error("Invalid credentials for Application ID: %s", n.getApplicationId());
                result = false;
            } else {
                try {
                    if(StringUtils.isEmpty(n.getEntityGuid(envVars))) {
                        client.sendNotification(Secret.toString(credentials.getPassword()),
                                n.getApplicationId(),
                                n.getDescription(envVars),
                                n.getRevision(envVars),
                                n.getChangelog(envVars),
                                n.getUser(envVars),
                                n.getEuropean(envVars));
                        listener.getLogger().println("Notified New Relic. Application ID: " + n.getApplicationId());
                    } else {
                        client.sendNotificationV2(Secret.toString(credentials.getPassword()),
                                n.getChangelog(envVars),
                                n.getCommit(envVars),
                                n.getDeeplink(envVars),
                                n.getDeploymentType(envVars),
                                n.getDescription(envVars),
                                n.getEntityGuid(envVars),
                                n.getGroupId(envVars),
                                n.getTimestamp(envVars),
                                n.getUser(envVars),
                                n.getVersion(envVars),
                                n.getEuropean(envVars),
                                listener);
                    }
                } catch (IOException e) {
                    listener.error("Failed to notify New Relic. Application ID: %s", n.getApplicationId());
                    e.printStackTrace(listener.getLogger());
                    result = false;
                }
            }
        }
        return result;
    }

    // help testing
    public NewRelicClient getClient() {
        return new NewRelicClientImpl();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath filePath, @NonNull Launcher launcher, @NonNull TaskListener taskListener) throws InterruptedException, IOException {
        EnvVars envVars = run.getEnvironment(taskListener);
        NewRelicClient client = getClient();
        for (DeploymentNotificationBean n : getNotifications()) {
            UsernamePasswordCredentials credentials = DeploymentNotificationBean.getCredentials(run.getParent(), n.getApiKey(), client.getApiEndpoint(n.getEuropean(envVars)));
            if (credentials == null) {
                taskListener.error("Invalid credentials for Entity GUID: %s", n.getEntityGuid(envVars));
                continue;
            }
            try {
                client.sendNotificationV2(Secret.toString(credentials.getPassword()),
                        n.getChangelog(envVars),
                        n.getCommit(envVars),
                        n.getDeeplink(envVars),
                        n.getDeploymentType(envVars),
                        n.getDescription(envVars),
                        n.getEntityGuid(envVars),
                        n.getGroupId(envVars),
                        n.getTimestamp(envVars),
                        n.getUser(envVars),
                        n.getVersion(envVars),
                        n.getEuropean(envVars),
                        taskListener);
            } catch (IOException e) {
                taskListener.error("Failed to notify New Relic. Entity GUID: %s", n.getEntityGuid(envVars));
                e.printStackTrace(taskListener.getLogger());
            }
        }
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
