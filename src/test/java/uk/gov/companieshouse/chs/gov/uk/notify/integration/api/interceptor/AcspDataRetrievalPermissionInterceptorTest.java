package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.interceptor;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.companieshouse.api.interceptor.TokenPermissionsInterceptor;
import uk.gov.companieshouse.api.util.security.InvalidTokenPermissionException;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AcspDataRetrievalPermissionInterceptorTest {

    private AcspDataRetrievalPermissionInterceptor acspDataRetrievalPermissionInterceptor;

    @BeforeEach
    void setup() {
        acspDataRetrievalPermissionInterceptor = new AcspDataRetrievalPermissionInterceptor();
    }

    @Test
    void prehandleWithoutPermissionReturnsFalse() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ERIC_IDENTITY_TYPE, "oauth2");
        request.addHeader("Eric-Authorised-Token-Permissions", "");

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        HttpServletResponse response = new MockHttpServletResponse();

        new TokenPermissionsInterceptor().preHandle(request, response, null);
        new AdminPermissionsInterceptor().preHandle(request, response, null);

        assertFalse(acspDataRetrievalPermissionInterceptor.preHandle(request, response, null));
        assertEquals(403, response.getStatus());
    }

    @Test
    void prehandleWithPermissionReturnsTrue() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ERIC_IDENTITY_TYPE, "oauth2");
        request.addHeader("Eric-Authorised-Token-Permissions", "acsp_members=read");

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        HttpServletResponse response = new MockHttpServletResponse();

        new TokenPermissionsInterceptor().preHandle(request, response, null);
        new AdminPermissionsInterceptor().preHandle(request, response, null);

        assertTrue(acspDataRetrievalPermissionInterceptor.preHandle(request, response, null));
        assertEquals(200, response.getStatus());
    }

    @Test
    void prehandleReturnsTrueForAdminUser() throws InvalidTokenPermissionException {
        final var request = new MockHttpServletRequest();
        request.addHeader("ERIC-Identity-Type", "oauth2");
        request.addHeader("ERIC-Authorised-Roles", "/admin/acsp/search");
        request.setMethod("GET");
        request.setRequestURI("/acsps/WITA001/memberships");

        final var requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        final var response = new MockHttpServletResponse();

        new TokenPermissionsInterceptor().preHandle(request, response, null);
        new AdminPermissionsInterceptor().preHandle(request, response, null);

        assertTrue(acspDataRetrievalPermissionInterceptor.preHandle(request, response, null));
        assertEquals(200, response.getStatus());
    }

}
