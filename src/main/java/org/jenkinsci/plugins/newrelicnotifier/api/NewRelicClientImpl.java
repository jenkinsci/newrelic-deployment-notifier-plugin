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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import hudson.model.TaskListener;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

/**
 * REST client implementation for the New Relic API.
 */
public class NewRelicClientImpl implements NewRelicClient {

    public static final String API_SCHEME = "https";

    public static final String API_HOST = "api.newrelic.com";
    public static final String EUROPEAN_API_HOST = "api.eu.newrelic.com";

    public static final String NERD_GRAPH_ENDPOINT = "/graphql";

    public static final String DEPLOYMENT_ENDPOINT = "/deployments.json";

    public static final String APPLICATIONS_ENDPOINT = "/v2/applications.json";

    public static final String PAGE_PARAMETER = "page";
    public static final String GRAPHQL_URL = API_HOST;
    public static final String EUROPEAN_GRAPHQL_URL = EUROPEAN_API_HOST;

    public static final int PAGE_SIZE = 200;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Application> getApplications(String apiKey, boolean european) throws IOException {
        List<Application> result = new LinkedList<>();

        CloseableHttpClient client = getHttpClient(european ? EUROPEAN_API_HOST : API_HOST);

        HttpGet request = new HttpGet();
        setHeaders(request, apiKey);

        ResponseHandler<ApplicationList> rh = getApplicationsHandler();

        try {
            int page = 1;
            ApplicationList response = null;
            //NewRelic pages appservice with 200 objects max.
            //The other way is making always an extra request to check for an empty list. Or parse "Link" Response header
            while(page == 1 || response.getApplications().size() == PAGE_SIZE) {
                request.setURI(getEndpointURI(APPLICATIONS_ENDPOINT, page++, european ? EUROPEAN_API_HOST : API_HOST));
                response = client.execute(request, rh);
                result.addAll(response.getApplications());
            }

        } finally {
            if (client != null) {
                client.close();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendNotification(String apiKey, String applicationId, String description, String revision,
                                    String changelog, String user, boolean european) throws IOException {
        String appUrl = "/v2/applications/" + applicationId;

        URI url = getEndpointURI(appUrl + DEPLOYMENT_ENDPOINT, null, european ? EUROPEAN_API_HOST : API_HOST);

        HttpPost request = new HttpPost(url);
        setHeaders(request, apiKey);

        JsonObject deployment = new JsonObject();
        deployment.addProperty("revision", revision);
        deployment.addProperty("changelog", changelog);
        deployment.addProperty("description", description);
        deployment.addProperty("user", user);
        JsonObject root = new JsonObject();
        root.add("deployment", deployment);

        StringEntity entity = new StringEntity(root.toString());
        request.setEntity(entity);
        entity.setContentType("application/json");

        try (CloseableHttpClient client = getHttpClient(european ? EUROPEAN_API_HOST : API_HOST)) {
            CloseableHttpResponse response = client.execute(request);
            StatusLine statusLine = response.getStatusLine();
            if (HttpStatus.SC_CREATED != statusLine.getStatusCode()) {
                String responseBody = null;
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    try (InputStream content = responseEntity.getContent()) {
                        responseBody = IOUtils.toString(content, Charset.defaultCharset());
                    }
                }
                throw new HttpResponseException(
                        statusLine.getStatusCode(),
                        statusLine.getReasonPhrase() + (responseBody != null ? "; Body = " + responseBody : "")
                );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendNotificationV2(
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
    ) throws IOException {
        URI url;
        CloseableHttpClient client;
        url = getEndpointURI(NERD_GRAPH_ENDPOINT, null, european ? EUROPEAN_GRAPHQL_URL : GRAPHQL_URL);
        client = getHttpClient(european ? EUROPEAN_GRAPHQL_URL : GRAPHQL_URL);
        if (european) {
            client = getHttpClient(EUROPEAN_GRAPHQL_URL);
            url = getEndpointURI(NERD_GRAPH_ENDPOINT, null, EUROPEAN_GRAPHQL_URL);
        }

        HttpPost request = new HttpPost(url);
        setHeaders(request, apiKey);

        String strPayload = makePayload(
                changelog,
                commit,
                deepLink,
                deploymentType,
                description,
                entityGuid,
                groupId,
                timestamp,
                user,
                version
        );

        StringEntity entity = new StringEntity(strPayload);
        request.setEntity(entity);
        entity.setContentType("application/json");

        try {
            int tries = 0;
            while (tries++ < 3) {
                CloseableHttpResponse response = client.execute(request);
                StatusLine statusLine = response.getStatusLine();
                if (HttpStatus.SC_OK != statusLine.getStatusCode()) {
                    String responseBody = null;
                    HttpEntity responseEntity = response.getEntity();
                    if (responseEntity != null) {
                        try (InputStream content = responseEntity.getContent()) {
                            responseBody = IOUtils.toString(content, Charset.defaultCharset());
                        }
                    }
                    responseBody += ", requestBody: " + strPayload;
                    throw new HttpResponseException(
                            statusLine.getStatusCode(),
                            statusLine.getReasonPhrase() + (responseBody != null ? "; Body = " + responseBody : "")
                    );
                } else {
                    String responseBody = null;
                    HttpEntity responseEntity = response.getEntity();
                    if (responseEntity != null) {
                        try (InputStream content = responseEntity.getContent()) {
                            responseBody = IOUtils.toString(content, Charset.defaultCharset());
                        }
                    }
                    responseBody += ", requestBody: " + strPayload;
                    String deploymentId = parseResponseBody(responseBody);
                    if (deploymentId != null && deploymentId.length() == 36) {
                        listener.getLogger().println("Notified New Relic. New Relic Deployment ID: " + deploymentId);
                        break;
                    } else {
                        if (tries == 3) {
                            listener.getLogger().println("Unable to reach New Relic to record the Deployment Id");
                        } else {
                            listener.getLogger().println("Retrying calling New Relic API...");
                        }
                    }
                }
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }

    }

    public String parseResponseBody(String str) {
        if (str == null || !str.contains("deploymentId\":\"")) {
            return null;
        }
        int start = str.indexOf("deploymentId\":\"");
        String subString = str.substring(start + "deploymentId\":\"".length());
        return subString.substring(0, 36);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getApiEndpoint() {
        return API_SCHEME + API_HOST;
    }

    @Override
    public String getApiEndpoint(boolean european) {
        if (european)
            return API_SCHEME + EUROPEAN_API_HOST;
        return getApiEndpoint();
    }

    public String makePayload(
            String changelog,
            String commit,
            String deepLink,
            String deploymentType,
            String description,
            String entityGuid,
            String groupId,
            String timestamp,
            String user,
            String version) {

        JsonObject payload = new JsonObject();
        payload.addProperty("commit", commit);
        payload.addProperty("changelog", changelog);
        payload.addProperty("description", description);
        payload.addProperty("user", user);
        payload.addProperty("deepLink", deepLink);
        payload.addProperty("deploymentType", deploymentType);
        payload.addProperty("entityGuid", entityGuid);
        payload.addProperty("user", user);
        payload.addProperty("version", version);
        payload.addProperty("groupId", groupId);
        payload.addProperty("timestamp", timestamp);

        StringBuilder header = new StringBuilder();

        payload.entrySet().forEach(e -> {
            if (e.getValue() != null && e.getValue().toString().trim().length() > 2) {
                if (header.length() > 0) {
                    header.append(", ");
                }
                String value = e.getValue().toString();
                if (e.getKey().equalsIgnoreCase("deploymentType") || e.getKey().equalsIgnoreCase("timestamp")) {
                    value = value.replace("\"", "");
                    header.append(e.getKey()).append(": ").append(value);
                } else {
                    value = value.substring(0, value.length() - 1);
                    value += "\\\"";
                    header.append(e.getKey()).append(": \\").append(value);
                }
            }
        });

        return "{\"query\":"
                + "\"mutation {changeTrackingCreateDeployment(deployment: {"
                + header
                + "}) {"
                + "deploymentId"
                + "}}\"}";
    }

    protected CloseableHttpClient getHttpClient(String host) {
        HttpClientBuilder builder = HttpClientBuilder.create();
        Jenkins instance = Jenkins.getInstanceOrNull();

        if (instance != null) {
            ProxyConfiguration proxyConfig = instance.proxy;
            if (proxyConfig != null) {
                Proxy proxy = proxyConfig.createProxy(host);
                if (proxy != null && proxy.type() == Proxy.Type.HTTP) {
                    SocketAddress addr = proxy.address();
                    if (addr instanceof InetSocketAddress proxyAddr) {
	                    HttpHost proxyHost = new HttpHost(proxyAddr.getAddress().getHostAddress(), proxyAddr.getPort());
                        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxyHost);
                        builder = builder.setRoutePlanner(routePlanner);

                        String proxyUser = proxyConfig.getUserName();
                        if (proxyUser != null) {
                            String proxyPass = proxyConfig.getSecretPassword().getPlainText();
                            CredentialsProvider cred = new BasicCredentialsProvider();
                            cred.setCredentials(new AuthScope(proxyHost),
                                    new UsernamePasswordCredentials(proxyUser, proxyPass));
                            builder = builder
                                    .setDefaultCredentialsProvider(cred)
                                    .setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
                        }
                    }
                }
            }
        }

        return builder.build();
    }

    private void setHeaders(HttpRequest request, String apiKey) {
        request.addHeader("X-Api-Key", apiKey);
        request.addHeader("Accept", "application/json");
        request.addHeader("NewRelic-Requesting-Services", "newrelic-jenkins-plugin");
    }

    /**
     * Retrieves the URI object for the given endpoint and page if the content wanted is paged.
     * @param endpoint
     * @param page
     * @return
     */
    private URI getEndpointURI(String endpoint, Integer page, String host) {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme(API_SCHEME);
        uriBuilder.setHost(host);
        uriBuilder.setPath(endpoint);
        if (page != null)
            uriBuilder.setParameter(PAGE_PARAMETER, page.toString());

        try {
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            // no need to handle this
            return null;
        }

    }

    private ResponseHandler<ApplicationList> getApplicationsHandler() {
        return response -> {
	        StatusLine statusLine = response.getStatusLine();
	        if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
		        throw new HttpResponseException(
				        statusLine.getStatusCode(),
				        statusLine.getReasonPhrase()
		        );
	        }
	        HttpEntity entity = response.getEntity();
	        if (entity == null) {
		        throw new ClientProtocolException("Response contains no content");
	        }
	        Gson gson = new GsonBuilder().create();
	        Reader reader = new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8);
	        return gson.fromJson(reader, ApplicationList.class);
        };
    }
}
