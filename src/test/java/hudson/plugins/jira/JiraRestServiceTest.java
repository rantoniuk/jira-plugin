package hudson.plugins.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Permissions;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import hudson.plugins.jira.extension.ExtendedJiraRestClient;
import hudson.plugins.jira.extension.ExtendedMyPermissionsRestClient;
import io.atlassian.util.concurrent.Promise;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class JiraRestServiceTest {

    private final URI JIRA_URI = URI.create("http://example.com:8080/");
    private final String USERNAME = "user";
    private final String PASSWORD = "password";
    private ExtendedJiraRestClient client;
    private SearchRestClient searchRestClient;
    private Promise promise;
    private SearchResult searchResult;

    @BeforeEach
    void createMocks() throws InterruptedException, ExecutionException, TimeoutException {
        client = mock(ExtendedJiraRestClient.class);
        searchRestClient = mock(SearchRestClient.class);
        promise = mock(Promise.class);
        searchResult = mock(SearchResult.class);

        doReturn(searchRestClient).when(client).getSearchClient();
        doReturn(promise).when(searchRestClient).searchJql(any(), any(), anyInt(), any());
        doReturn(searchResult).when(promise).get(anyLong(), any());
    }

    @Test
    void baseApiPath() {
        JiraRestService service = new JiraRestService(JIRA_URI, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT);
        assertEquals("/" + JiraRestService.BASE_API_PATH, service.getBaseApiPath());

        URI uri = URI.create("https://example.com/path/to/jira");
        service = new JiraRestService(uri, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT);
        assertEquals("/path/to/jira/" + JiraRestService.BASE_API_PATH, service.getBaseApiPath());
    }

    @Test
    void getIssuesFromJqlSearchRestException() throws InterruptedException, ExecutionException, TimeoutException {
        Throwable throwable = mock(Throwable.class);
        JiraRestService service =
                spy(new JiraRestService(JIRA_URI, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT));
        doThrow(new RestClientException("Verify Rest client exception", throwable))
                .when(promise)
                .get(Mockito.anyLong(), Mockito.any());
        assertThrows(RestClientException.class, () -> service.getIssuesFromJqlSearch("*", null));
    }

    @Test
    void getMyPermissionsSuccess() throws InterruptedException, ExecutionException, TimeoutException {
        ExtendedMyPermissionsRestClient permissionsRestClient = mock(ExtendedMyPermissionsRestClient.class);
        Promise permissionsPromise = mock(Promise.class);
        Permissions permissions = mock(Permissions.class);
        doReturn(permissionsRestClient).when(client).getExtendedMyPermissionsRestClient();
        doReturn(permissionsPromise).when(permissionsRestClient).getMyPermissions();
        doReturn(permissions).when(permissionsPromise).get(anyLong(), any());

        JiraRestService service = new JiraRestService(JIRA_URI, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT);
        assertEquals(permissions, service.getMyPermissions());
    }

    @Test
    void getMyPermissionsTimesOutInsteadOfHanging() throws InterruptedException, ExecutionException, TimeoutException {
        ExtendedMyPermissionsRestClient permissionsRestClient = mock(ExtendedMyPermissionsRestClient.class);
        Promise permissionsPromise = mock(Promise.class);
        doReturn(permissionsRestClient).when(client).getExtendedMyPermissionsRestClient();
        doReturn(permissionsPromise).when(permissionsRestClient).getMyPermissions();
        doThrow(new TimeoutException("Verify timeout")).when(permissionsPromise).get(anyLong(), any());

        JiraRestService service = new JiraRestService(JIRA_URI, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT);
        assertThrows(RestClientException.class, service::getMyPermissions);
    }

    @Test
    void getMyPermissionsReinterruptsOnInterruptedException()
            throws InterruptedException, ExecutionException, TimeoutException {
        ExtendedMyPermissionsRestClient permissionsRestClient = mock(ExtendedMyPermissionsRestClient.class);
        Promise permissionsPromise = mock(Promise.class);
        doReturn(permissionsRestClient).when(client).getExtendedMyPermissionsRestClient();
        doReturn(permissionsPromise).when(permissionsRestClient).getMyPermissions();
        doThrow(new InterruptedException("Verify interrupted"))
                .when(permissionsPromise)
                .get(anyLong(), any());

        JiraRestService service = new JiraRestService(JIRA_URI, client, USERNAME, PASSWORD, JiraSite.DEFAULT_TIMEOUT);
        try {
            assertThrows(RestClientException.class, service::getMyPermissions);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted(); // clear the interrupt flag so it doesn't leak into other tests
        }
    }
}
