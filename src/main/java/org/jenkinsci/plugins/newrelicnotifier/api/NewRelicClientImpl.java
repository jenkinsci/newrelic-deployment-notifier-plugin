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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * REST client implementation for the New Relic API.
 */
public class NewRelicClientImpl implements NewRelicClient {

    private static final String API_URL = "https://api.newrelic.com";

    private static final String DEPLOYMENT_ENDPOINT = "/deployments.xml";

    private static final String APPLICATIONS_ENDPOINT = "/v2/applications.json";

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Application> getApplications(String apiKey) throws IOException {
        List<Application> result = new ArrayList<Application>();
        HttpGet request = new HttpGet(API_URL + APPLICATIONS_ENDPOINT);
        setHeaders(request, apiKey);
        CloseableHttpClient client = getHttpClient();
        ResponseHandler<ApplicationList> rh = new ResponseHandler<ApplicationList>() {
            @Override
            public ApplicationList handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
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
                Reader reader = new InputStreamReader(entity.getContent());
                return gson.fromJson(reader, ApplicationList.class);
            }
        };
        try {
            ApplicationList response = client.execute(request, rh);
            result.addAll(response.getApplications());
        } finally {
            client.close();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sendNotification(String apiKey, String applicationId, String description, String revision,
                                    String changelog, String user) throws IOException {
        HttpPost request = new HttpPost(API_URL + DEPLOYMENT_ENDPOINT);
        setHeaders(request, apiKey);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("deployment[application_id]", applicationId));
        params.add(new BasicNameValuePair("deployment[description]", description));
        params.add(new BasicNameValuePair("deployment[revision]", revision));
        params.add(new BasicNameValuePair("deployment[changelog]", changelog));
        params.add(new BasicNameValuePair("deployment[user]", user));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
        request.setEntity(entity);
        CloseableHttpClient client = getHttpClient();
        try {
            HttpResponse response = client.execute(request);
            return HttpStatus.SC_CREATED == response.getStatusLine().getStatusCode();
        } finally {
            client.close();
        }
    }

    private CloseableHttpClient getHttpClient() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        return builder.build();
    }

    private void setHeaders(HttpRequest request, String apiKey) {
        request.addHeader("X-Api-Key", apiKey);
        request.addHeader("Accept", "application/json");
    }

    private class ApplicationList {
        private List<Application> applications;

        public ApplicationList(List<Application> applications) {
            this.applications = applications;
        }

        public List<Application> getApplications() {
            return applications;
        }
    }
}
