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
package org.jenkinsci.plugins.newrelicnotifier.api;

import hudson.model.TaskListener;

import java.io.IOException;
import java.util.List;

/**
 * REST client interface for the New Relic API.
 */
public interface NewRelicClient {

    /**
     * Get the list of applications available to record deployment notifications for.
     *
     * @param apiKey New Relic API key
     * @return A list of applications available for supplied API key.
     * @throws IOException when HttpClient is not able to be closed
     * @see <a href="https://docs.newrelic.com/docs/apm/apis/requirements/api-key">https://docs.newrelic.com/docs/apm/apis/requirements/api-key</a>
     */
    List<Application> getApplications(String apiKey) throws IOException;

    /**
     * Submit deployment notification
     *
     * @param apiKey New Relic API key
     * @param applicationId Application to register deployment for
     * @param description Text annotation for the deployment
     * @param revision The revision number from your source control system
     * @param changelog A list of changes for this deployment
     * @param user The name of the user/process that triggered this deployment
     * @throws IOException when HttpClient is not able to be closed or unexpected status code received
     * @see <a href="https://docs.newrelic.com/docs/apm/apis/requirements/api-key">https://docs.newrelic.com/docs/apm/apis/requirements/api-key</a>
     */
    void sendNotification(
            String apiKey,
            String applicationId,
            String description,
            String revision,
            String changelog,
            String user,
            boolean european
    ) throws IOException;

    /**
     * Get API Endpoint URL for looking up credentials.
     *
     * @return The endpoint URL for the New Relic API
     */
    String getApiEndpoint();

    String getApiEndpoint(boolean european);

    /**
     * Submit deployment notification
     *
     * @param apiKey New Relic API key
     * @param changelog A list of changes for this deployment
     * @param commit A commit hash for the deployment
     * @param deepLink A deep link for the deployment
     * @param deploymentType A deployment type for the deployment
     * @param description Text annotation for the deployment
     * @param entityGuid An entity GUID to identify the application for Nerdgraph deployment markers
     * @param groupId A group id for the deployment
     * @param timestamp A timestamp for the deployment
     * @param user The name of the user/process that triggered this deployment
     * @param version A version for the deployment
     * @throws IOException when HttpClient is not able to be closed or unexpected status code received
     * @see <a href="https://docs.newrelic.com/docs/apm/apis/requirements/api-key">https://docs.newrelic.com/docs/apm/apis/requirements/api-key</a>
     */
    void sendNotificationV2(
            String apiKey,
            String changelog,
            String commit,
            String deepLink,
            String deploymentType,
            String description,
            String entityGuid,
            String groupId,
            String timestamp,
            String user,
            String version,
            boolean european,
            TaskListener listener
    ) throws IOException;
}
