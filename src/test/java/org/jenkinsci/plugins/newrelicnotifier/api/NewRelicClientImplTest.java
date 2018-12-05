package org.jenkinsci.plugins.newrelicnotifier.api;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


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
