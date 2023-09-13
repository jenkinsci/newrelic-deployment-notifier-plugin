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

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
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
    private final String commit;
    private final String deeplink;
    private final String user;
    private final String entityGuid;
    private final String deploymentId;
    private final String deploymentType;
    private final String groupId;
    private final String timestamp;
    private final String version;
    private final boolean european;

    @DataBoundConstructor
    public DeploymentNotificationBean(String apiKey, String applicationId, String description, String revision, String changelog, String commit, String deeplink, String user, String entityGuid, String deploymentId, String deploymentType, String groupId, String timestamp, String version, boolean european) {
        super();
        this.apiKey = apiKey;
        this.applicationId = applicationId;
        this.description = description;
        this.revision = revision;
        this.changelog = changelog;
        this.commit = commit;
        this.deploymentId = deploymentId;
        this.deeplink = deeplink;
        this.user = user;
        this.entityGuid = entityGuid;
        this.deploymentType = deploymentType;
        this.groupId = groupId;
        this.timestamp = timestamp;
        this.version = version;
        this.european = european;
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

    public String getCommit() { return commit; }

    public String getDeeplink() {
        return deeplink;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getTimestamp(EnvVars env) {
        return env.expand(getTimestamp());
    }

    public String getVersion() {
        return version;
    }

    public String getVersion(EnvVars env) {
        return env.expand(getVersion());
    }

    public String getGroupId() {
        return groupId;
    }

    public String getGroupId(EnvVars env) {
        return env.expand(getGroupId());
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public String getEntityGuid() {
        return entityGuid;
    }

    public String getDeploymentType() {
        return this.deploymentType;
    }
    public boolean getEuropean() {
        return this.european;
    }

    public String getDeploymentType(EnvVars env) {
        return env.expand(getDeploymentType());
    }

    public boolean getEuropean(EnvVars env) {
        return Boolean.valueOf(env.expand(Boolean.toString(getEuropean())));
    }

    public String getCommit(EnvVars env) { return env.expand(getCommit()); }
    public String getDeploymentId(EnvVars env) { return env.expand(getDeploymentId()); }
    public String getDeeplink(EnvVars env) { return env.expand(getDeeplink()); }

    public String getEntityGuid(EnvVars env) {
        return env.expand(getEntityGuid());
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
    public static StandardUsernamePasswordCredentials getCredentials(Job<?,?> owner, String credentialId, String source) {
        List<StandardUsernamePasswordCredentials> credentials = availableCredentials(owner, source);
        CredentialsMatcher matcher = CredentialsMatchers.withId(credentialId);
        return CredentialsMatchers.firstOrNull(credentials, matcher);
    }

    private static List<StandardUsernamePasswordCredentials> availableCredentials(Job<?,?> owner, String source) {
        return CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
                owner, null, URIRequirementBuilder.fromUri(source).build());
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<DeploymentNotificationBean> {

        public ListBoxModel doFillApiKeyItems(@AncestorInPath Job<?,?> owner) {
            if (owner == null || !owner.hasPermission(Item.CONFIGURE)) {
                return new ListBoxModel();
            }
            return new StandardUsernameListBoxModel().withAll(availableCredentials(owner, getClient().getApiEndpoint()));
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
                NewRelicClient client = getClient();
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

        // help testing
        public NewRelicClient getClient() {
            return new NewRelicClientImpl();
        }
    }
}
