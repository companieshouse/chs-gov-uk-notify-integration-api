package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.interceptor;

import static com.mongodb.assertions.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AdminPermissionsInterceptorTest {

    private AdminPermissionsInterceptor adminPermissionsInterceptor;

    @BeforeEach
    void setup() {
        adminPermissionsInterceptor = new AdminPermissionsInterceptor();
    }

    @Test
    void preHandleSetsHasAdminPrivilegeToFalseForApiKeyRequests() {
        final var request = new MockHttpServletRequest();
        request.addHeader("ERIC-Identity-Type", "key");
        request.addHeader("ERIC-Authorised-Roles", "/admin/acsp/search");
        request.setMethod("GET");
        request.setRequestURI("/acsps/WITA001/memberships");

        final var requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        final var response = new MockHttpServletResponse();
        assertTrue(adminPermissionsInterceptor.preHandle(request, response, null));
        assertFalse((boolean) request.getAttribute("has_admin_privilege"));
    }

    @Test
    void preHandleSetsHasAdminPrivilegeToFalseWhenAppliedToUnprivilegedEndpoint() {
        final var request = new MockHttpServletRequest();
        request.addHeader("ERIC-Identity-Type", "oauth2");
        request.addHeader("ERIC-Authorised-Roles", "/admin/acsp/search");
        request.setMethod("PATCH");
        request.setRequestURI("/acsps/memberships/WIT001");

        final var requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        final var response = new MockHttpServletResponse();
        assertTrue(adminPermissionsInterceptor.preHandle(request, response, null));
        assertFalse((boolean) request.getAttribute("has_admin_privilege"));
    }

    @Test
    void preHandleSetsHasAdminPrivilegeToFalseWhenUserIsNotAdmin() {
        final var request = new MockHttpServletRequest();
        request.addHeader("ERIC-Identity-Type", "oauth2");
        request.setMethod("GET");
        request.setRequestURI("/acsps/WITA001/memberships");

        final var requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        final var response = new MockHttpServletResponse();
        assertTrue(adminPermissionsInterceptor.preHandle(request, response, null));
        assertFalse((boolean) request.getAttribute("has_admin_privilege"));
    }

    @Test
    void preHandleSetsHasAdminPrivilegeToTrueWhenAppliedToAdminRequest() {
        final var request = new MockHttpServletRequest();
        request.addHeader("ERIC-Identity-Type", "oauth2");
        request.addHeader("ERIC-Authorised-Roles", "/admin/acsp/search");
        request.setMethod("GET");
        request.setRequestURI("/acsps/WITA001/memberships");

        final var requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        final var response = new MockHttpServletResponse();
        assertTrue(adminPermissionsInterceptor.preHandle(request, response, null));
        assertTrue((boolean) request.getAttribute("has_admin_privilege"));
    }

}
