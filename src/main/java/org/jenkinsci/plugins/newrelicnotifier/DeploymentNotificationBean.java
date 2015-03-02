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

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import org.jenkinsci.plugins.newrelicnotifier.api.Application;
import org.jenkinsci.plugins.newrelicnotifier.api.NewRelicClient;
import org.jenkinsci.plugins.newrelicnotifier.api.NewRelicClientImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

/**
 * Bean to hold each deployment notification configuration.
 */
public class DeploymentNotificationBean extends AbstractDescribableImpl<DeploymentNotificationBean> {

    private final Secret apiKey;
    private final String applicationId;
    private final String description;
    private final String revision;
    private final String changelog;
    private final String user;

    @DataBoundConstructor
    public DeploymentNotificationBean(Secret apiKey, String applicationId, String description, String revision, String changelog, String user) {
        super();
        this.apiKey = apiKey;
        this.applicationId = applicationId;
        this.description = description;
        this.revision = revision;
        this.changelog = changelog;
        this.user = user;
    }

    public Secret getApiKey() {
        return apiKey;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getDescription() {
        return description;
    }

    public String getRevision() {
        return revision;
    }

    public String getChangelog() {
        return changelog;
    }

    public String getUser() {
        return user;
    }

    public String getDescription(EnvVars env) {
        return env.expand(getDescription());
    }

    public String getRevision(EnvVars env) {
        return env.expand(getRevision());
    }

    public String getChangelog(EnvVars env) {
        return env.expand(getChangelog());
    }

    public String getUser(EnvVars env) {
        return env.expand(getUser());
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<DeploymentNotificationBean> {

        private final NewRelicClient client = new NewRelicClientImpl();

        public FormValidation doCheckApiKey(@QueryParameter("apiKey") Secret apiKey) {
            if (apiKey == null || 0 == Secret.toString(apiKey).length()) {
                return FormValidation.error("Missing API Key");
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillApplicationIdItems(@QueryParameter("apiKey") final Secret apiKey) throws IOException {
            ListBoxModel items = new ListBoxModel();
            if (apiKey != null && Secret.toString(apiKey).length() > 0) {
                for (Application application : client.getApplications(Secret.toString(apiKey))) {
                    items.add(application.getName(), application.getId());
                }
            }
            return items;
        }

        @Override
        public String getDisplayName() {
            return "Deployment Notification";
        }
    }
}
