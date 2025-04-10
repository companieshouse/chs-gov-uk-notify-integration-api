package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.interceptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.API_KEY_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.INTERNAL_USER_ROLE;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiAuthorisationInterceptorTest {

    @InjectMocks
    private ApiAuthorisationInterceptor authorisationInterceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Logger logger;

    private static final String INVALID_IDENTITY_TYPE_VALUE = "test";
    private static final String NON_INTERNAL_USER_ROLES = "role1 role2";
    private static final String X_REQUEST_ID = "X-Request-ID";
    private static final String TOKEN_REQUEST_ID = "token-request-id";
    private static final Object UNUSED_HANDLER = new Object();

    @Test
    @DisplayName("Authorises POST request with an internal API key")
    void willAuthoriseIfRequestIsPostAndInternalApiKey() {
        when(request.getMethod()).thenReturn(HttpMethod.POST.toString());
        doReturn(TOKEN_REQUEST_ID).when(request).getHeader(X_REQUEST_ID);
        doReturn(API_KEY_IDENTITY_TYPE).when(request).getHeader(ERIC_IDENTITY_TYPE);
        doReturn(INTERNAL_USER_ROLE).when(request).getHeader(ERIC_AUTHORISED_KEY_ROLES);
        assertTrue(authorisationInterceptor.preHandle(request, response, UNUSED_HANDLER));
    }

    @Test
    @DisplayName("Does not authorise if an external API key is used")
    void willNotAuthoriseIfRequestIsPutAndExternalApiKey() {
        when(request.getMethod()).thenReturn(HttpMethod.POST.toString());
        doReturn(TOKEN_REQUEST_ID).when(request).getHeader(X_REQUEST_ID);
        when(request.getHeader(ERIC_IDENTITY_TYPE)).thenReturn(API_KEY_IDENTITY_TYPE);
        doReturn(NON_INTERNAL_USER_ROLES).when(request).getHeader(ERIC_AUTHORISED_KEY_ROLES);
        assertFalse(authorisationInterceptor.preHandle(request, response, UNUSED_HANDLER));
    }

    @Test
    @DisplayName("Does not authorise if unrecognised identity type provided")
    void willNotAuthoriseIfRequestIsPostAndUnrecognisedIdentity() {
        doReturn(TOKEN_REQUEST_ID).when(request).getHeader(X_REQUEST_ID);
        when(request.getHeader(ERIC_IDENTITY_TYPE)).thenReturn(INVALID_IDENTITY_TYPE_VALUE);
        assertFalse(authorisationInterceptor.preHandle(request, response, UNUSED_HANDLER));
    }

    @Test
    @DisplayName("Does not authorise if no identity type provided")
    void willNotAuthoriseIfRequestIsPostAndNoIdentity() {
        doReturn(TOKEN_REQUEST_ID).when(request).getHeader(X_REQUEST_ID);
        when(request.getHeader(ERIC_IDENTITY_TYPE)).thenReturn(null);
        assertFalse(authorisationInterceptor.preHandle(request, response, UNUSED_HANDLER));
    }

    @Test
    @DisplayName("Does not authorise GET request with an internal API key")
    void willNotAuthoriseIfRequestIsGetWithInternalApiKey() {
        when(request.getMethod()).thenReturn(HttpMethod.GET.toString());
        doReturn(TOKEN_REQUEST_ID).when(request).getHeader(X_REQUEST_ID);
        doReturn(API_KEY_IDENTITY_TYPE).when(request).getHeader(ERIC_IDENTITY_TYPE);
        doReturn(INTERNAL_USER_ROLE).when(request).getHeader(ERIC_AUTHORISED_KEY_ROLES);
        assertFalse(authorisationInterceptor.preHandle(request, response, UNUSED_HANDLER));
    }

}
