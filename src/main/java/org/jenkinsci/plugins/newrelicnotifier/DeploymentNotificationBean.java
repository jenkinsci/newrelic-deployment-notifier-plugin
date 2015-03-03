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
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Job;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import org.jenkinsci.plugins.newrelicnotifier.api.Application;
import org.jenkinsci.plugins.newrelicnotifier.api.NewRelicClient;
import org.jenkinsci.plugins.newrelicnotifier.api.NewRelicClientImpl;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.List;

/**
 * Bean to hold each deployment notification configuration.
 */
public class DeploymentNotificationBean extends AbstractDescribableImpl<DeploymentNotificationBean> {

    private final String apiKey;
    private final String applicationId;
    private final String description;
    private final String revision;
    private final String changelog;
    private final String user;

    @DataBoundConstructor
    public DeploymentNotificationBean(String apiKey, String applicationId, String description, String revision, String changelog, String user) {
        super();
        this.apiKey = apiKey;
        this.applicationId = applicationId;
        this.description = description;
        this.revision = revision;
        this.changelog = changelog;
        this.user = user;
    }

    public String getApiKey() {
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

    @CheckForNull
    public static StandardUsernamePasswordCredentials getCredentials(Job<?,?> owner, String credentialsId, String source) {
        if (credentialsId != null) {
            for (StandardUsernamePasswordCredentials c : availableCredentials(owner, source)) {
                if (c.getId().equals(credentialsId)) {
                    return c;
                }
            }
        }
        return null;
    }

    private static List<? extends StandardUsernamePasswordCredentials> availableCredentials(Job<?,?> owner, String source) {
        return CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class, owner, null, URIRequirementBuilder.fromUri(source).build());
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<DeploymentNotificationBean> {

        private final NewRelicClient client = new NewRelicClientImpl();

        public ListBoxModel doFillApiKeyItems(@AncestorInPath Job<?,?> owner) {
            if (owner == null || !owner.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }
            return new StandardUsernameListBoxModel().withAll(availableCredentials(owner, client.getApiEndpoint()));
        }

        public FormValidation doCheckApiKey(@QueryParameter("apiKey") String apiKey) {
            if (apiKey == null || apiKey.length() == 0) {
                return FormValidation.error("Missing API Key");
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillApplicationIdItems(@AncestorInPath Job<?,?> owner, @QueryParameter("apiKey") final String apiKey) throws IOException {
            if (owner == null || !owner.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }
            ListBoxModel items = new ListBoxModel();
            if (apiKey != null && apiKey.length() > 0) {
                UsernamePasswordCredentials credentials = getCredentials(owner, apiKey, client.getApiEndpoint());
                if (credentials != null) {
                    for (Application application : client.getApplications(Secret.toString(credentials.getPassword()))) {
                        items.add(application.getName(), application.getId());
                    }
                }
            }
            return items;
        }

        public FormValidation doCheckApplicationId(@QueryParameter("applicationId") String applicationId) {
            if (applicationId == null || applicationId.length() == 0) {
                return FormValidation.error("No applications!");
            }
            return FormValidation.ok();
        }

        @Override
        public String getDisplayName() {
            return "Deployment Notification";
        }
    }
}
