package org.jenkinsci.plugins.newrelicnotifier.api;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.util.LinkedList;
import java.util.List;

import hudson.model.TaskListener;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.annotation.Nonnull;


public class NewRelicClientImplTest {

    private NewRelicClientStub nrClient;
    private HttpClientStub httpClient = mock(HttpClientStub.class);
    
    @Before
    public void setup() throws IOException {
        nrClient = new NewRelicClientStub();
        nrClient.setHttpClient(httpClient);

    }
   
    @SuppressWarnings("unchecked")
    @Test
    public void getMultiplePagesApplications() throws ClientProtocolException, IOException {
        int expectedSize = NewRelicClientImpl.PAGE_SIZE + 50;
        when(httpClient.execute(any(HttpUriRequest.class), any(ResponseHandler.class)))
            .thenAnswer(getAnswerForAppSize(expectedSize));

        try {
            List<Application> apps = nrClient.getApplications("someapikey");
            assertTrue(apps.size() == expectedSize);
            verify(httpClient, times(2)).execute(any(HttpUriRequest.class), any(ResponseHandler.class));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFailureScenario() throws IOException {
        when(httpClient.execute(any()))
                .thenAnswer((InvocationOnMock invocation) -> {
                    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
                    StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, HttpURLConnection.HTTP_OK, "OK");
                    when(response.getStatusLine()).thenReturn(statusLine);
                    return response;
                });

        try {
            nrClient.sendNotificationV2("1",
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
                    true,
                    new TaskListener() {
                        @Nonnull
                        @Override
                        public PrintStream getLogger() {
                            return System.out;
                        }
                    });
            verify(httpClient, times(3)).execute(any());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void makePayloadTest() {
        String result = nrClient.makePayload("","","","","","test","","","jenkins","1");
        String expected = "{\"query\":\"mutation {changeTrackingCreateDeployment(deployment: {user: \\\"jenkins\\\", entityGuid: \\\"test\\\", version: \\\"1\\\"}) {deploymentId}}\"}";
        assertEquals(expected, result);

        result = nrClient.makePayload("","","","BLUE_GREEN","","test","","","jenkins","1");
        expected = "{\"query\":\"mutation {changeTrackingCreateDeployment(deployment: {user: \\\"jenkins\\\", deploymentType: BLUE_GREEN, entityGuid: \\\"test\\\", version: \\\"1\\\"}) {deploymentId}}\"}";
        assertEquals(expected, result);
    }

    @Test
    public void parsePayload() {
        String result = nrClient.parseResponseBody("{\"data\":{\"changeTrackingCreateDeployment\":{\"deploymentId\":\"71c3f8f5-cecc-4299-aa0f-18f3fafa6313\",\"user\":\"justinlewis\"}}}");
        String expected = "71c3f8f5-cecc-4299-aa0f-18f3fafa6313";
        assertEquals(expected, result);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void getOnePageOfApplications() throws ClientProtocolException, IOException {
        int expectedSize = NewRelicClientImpl.PAGE_SIZE - 50;
        when(httpClient.execute(any(HttpUriRequest.class), any(ResponseHandler.class)))
            .thenAnswer(getAnswerForAppSize(expectedSize));

        try {
            List<Application> apps = nrClient.getApplications("someapikey");
            assertTrue(apps.size() == expectedSize);
            verify(httpClient).execute(any(HttpUriRequest.class), any(ResponseHandler.class));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private Answer<ApplicationList> getAnswerForAppSize(final int size) {
        return new Answer<ApplicationList>() {
            private int count = 0;
            private int fullPages = size / NewRelicClientImpl.PAGE_SIZE;
            private int rest = size % NewRelicClientImpl.PAGE_SIZE;
            public ApplicationList answer(InvocationOnMock invocation) {
                if (count++ < fullPages)
                    return new ApplicationList(getApplicationMocks(NewRelicClientImpl.PAGE_SIZE));

                return new ApplicationList(getApplicationMocks(rest));
            }
        };
    }
    
    private List<Application> getApplicationMocks(int size) {
        List<Application> apps = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            apps.add(mock(Application.class));
        }
        return apps;
    }
    
}
