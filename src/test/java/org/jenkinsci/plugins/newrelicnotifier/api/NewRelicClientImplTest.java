package org.jenkinsci.plugins.newrelicnotifier.api;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.TaskListener;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.util.LinkedList;
import java.util.List;

import static org.jenkinsci.plugins.newrelicnotifier.api.NewRelicClientImpl.API_HOST;
import static org.jenkinsci.plugins.newrelicnotifier.api.NewRelicClientImpl.EUROPEAN_API_HOST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class NewRelicClientImplTest {

    private NewRelicClientStub nrClient;
    private final HttpClientStub httpClient = mock(HttpClientStub.class);

    @Before
    public void setup() {
        nrClient = new NewRelicClientStub();
        nrClient.setHttpClient(httpClient);

    }

    @Test
    public void getMultiplePagesApplications() throws IOException {
        int expectedSize = NewRelicClientImpl.PAGE_SIZE + 50;
        when(httpClient.execute(any(HttpUriRequest.class), any(ResponseHandler.class)))
            .thenAnswer(getAnswerForAppSize(expectedSize));

        try {
            List<Application> apps = nrClient.getApplications("someapikey", false);
            assertEquals(expectedSize, apps.size());
            verify(httpClient, times(2)).execute(any(HttpUriRequest.class), any(ResponseHandler.class));
        } catch (IOException e) {
            fail("Did not expect an exception.");
        }
    }

    @Test
    public void test_getApplications() throws IOException {
        test_getApplicationsParmeterized(false, API_HOST);
        test_getApplicationsParmeterized(true, EUROPEAN_API_HOST);
    }

    private void test_getApplicationsParmeterized(boolean european, String expectedApiHost) throws IOException {

        int expectedSize = NewRelicClientImpl.PAGE_SIZE - 50;
        when(httpClient.execute(any(HttpUriRequest.class), any(ResponseHandler.class)))
            .thenAnswer(getAnswerForAppSize(expectedSize));

        try {
            List<Application> apps = nrClient.getApplications("someapikey", european);
            assertEquals(expectedSize, apps.size());
            ArgumentCaptor<HttpUriRequest> httpUriRequestArgumentCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
            verify(httpClient, times(1)).execute(httpUriRequestArgumentCaptor.capture(), any(ResponseHandler.class));
            HttpUriRequest httpUriRequest = httpUriRequestArgumentCaptor.getValue();
            assertEquals(expectedApiHost, httpUriRequest.getURI().getHost());
            reset(httpClient);
        } catch (IOException e) {
            fail("Did not expect an exception.");
        }
    }

    @Test
    public void testFailureScenario() throws IOException {
        testFailureScenarioParameterized(false, API_HOST);
        testFailureScenarioParameterized(true, EUROPEAN_API_HOST);
    }

    public void testFailureScenarioParameterized(boolean european, String expectedApiHost) throws IOException {
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
                    european,
                    new TaskListener() {
                        @NonNull
                        @Override
                        public PrintStream getLogger() {
                            return System.out;
                        }
                    });
            ArgumentCaptor<HttpUriRequest> httpUriRequestArgumentCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
            verify(httpClient, times(3)).execute(httpUriRequestArgumentCaptor.capture());
            assertEquals(expectedApiHost, httpUriRequestArgumentCaptor.getValue().getURI().getHost());
            reset(httpClient);
        } catch (IOException e) {
            fail("Did not expect an exception.");
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

    @Test
    public void getOnePageOfApplications() throws IOException {
        int expectedSize = NewRelicClientImpl.PAGE_SIZE - 50;
        when(httpClient.execute(any(HttpUriRequest.class), any(ResponseHandler.class)))
            .thenAnswer(getAnswerForAppSize(expectedSize));

        try {
            List<Application> apps = nrClient.getApplications("someapikey", false);
            assertEquals(expectedSize, apps.size());
            verify(httpClient).execute(any(HttpUriRequest.class), any(ResponseHandler.class));
        } catch (IOException e) {
            fail("Did not expect an exception.");
        }
    }

    private Answer<ApplicationList> getAnswerForAppSize(final int size) {
        return new Answer<>() {
	        private int count = 0;
	        private final int fullPages = size / NewRelicClientImpl.PAGE_SIZE;
	        private final int rest = size % NewRelicClientImpl.PAGE_SIZE;

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
