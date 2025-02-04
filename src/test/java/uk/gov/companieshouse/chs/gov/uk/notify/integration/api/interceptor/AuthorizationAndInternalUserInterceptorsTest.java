package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AuthorizationAndInternalUserInterceptorsTest {

    @Mock
    UsersService usersService;

    private AuthorizationAndInternalUserInterceptors authorizationAndInternalUserInterceptors;

    @BeforeEach
    void setup() {
        authorizationAndInternalUserInterceptors = new AuthorizationAndInternalUserInterceptors(new InterceptorHelper(usersService));
    }

    @Test
    void preHandleWithoutEricHeadersReturnsUnauthorised() {
        final var request = new MockHttpServletRequest();

        final var response = new MockHttpServletResponse();
        assertFalse(authorizationAndInternalUserInterceptors.preHandle(request, response, null));
        assertEquals(401, response.getStatus());
    }

    @Test
    void preHandleWithOAuth2RequestUsesAuthorizationInterceptor() {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "111");
        request.addHeader("Eric-identity-type", "oauth2");

        final var bruce = new User()
                .userId("111")
                .email("bruce.wayne@gotham.city")
                .displayName("Batman");

        Mockito.doReturn(bruce).when(usersService).fetchUserDetails("111");

        final var response = new MockHttpServletResponse();
        assertTrue(authorizationAndInternalUserInterceptors.preHandle(request, response, null));
    }

    @Test
    void preHandleWithApiKeyRequestUsesInternalUserInterceptor() {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "111");
        request.addHeader("Eric-identity-type", "key");
        request.addHeader("ERIC-Authorised-Key-Roles", "*");

        final var response = new MockHttpServletResponse();
        assertTrue(authorizationAndInternalUserInterceptors.preHandle(request, response, null));
    }

    @Test
    void preHandleWithMalformedEricIdentityTypeReturnsUnauthorised() {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "111");
        request.addHeader("Eric-identity-type", "fingerprint");
        request.addHeader("ERIC-Authorised-Key-Roles", "*");

        final var response = new MockHttpServletResponse();
        assertFalse(authorizationAndInternalUserInterceptors.preHandle(request, response, null));
        assertEquals(401, response.getStatus());
    }

}
