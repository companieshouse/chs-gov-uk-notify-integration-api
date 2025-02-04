package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.TestDataManager;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspMembersService;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.interceptor.TokenPermissionsInterceptor;
import uk.gov.companieshouse.api.util.security.InvalidTokenPermissionException;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class SessionValidityInterceptorTest {

    @Mock
    private AcspMembersService acspMembersService;

    private SessionValidityInterceptor sessionValidityInterceptor;

    private TestDataManager testDataManager = TestDataManager.getInstance();

    @BeforeEach
    void setup() {
        sessionValidityInterceptor = new SessionValidityInterceptor(acspMembersService);
    }

    @Test
    void preHandleWithAPIKeyReturnsTrue() {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        request.addHeader("Eric-identity-type", "key");
        request.addHeader("ERIC-Authorised-Key-Roles", "*");

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        final var response = new MockHttpServletResponse();

        new AdminPermissionsInterceptor().preHandle(request, response, null);

        assertTrue(sessionValidityInterceptor.preHandle(request, response, null));
    }

    @Test
    void preHandleWithoutEricIdentityReturnsForbidden() throws InvalidTokenPermissionException {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity-type", "oauth2");
        request.addHeader("ERIC-Authorised-Key-Roles", "*");
        request.addHeader("Eric-Authorised-Token-Permissions", "acsp_number=WITA001 acsp_members=read acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete");

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Mockito.doReturn(Optional.empty()).when(acspMembersService).fetchActiveAcspMembership(null, "WITA001");

        final var response = new MockHttpServletResponse();

        new TokenPermissionsInterceptor().preHandle(request, response, null);
        new AdminPermissionsInterceptor().preHandle(request, response, null);

        assertFalse(sessionValidityInterceptor.preHandle(request, response, null));
        assertEquals(403, response.getStatus());
    }

    @Test
    void preHandleWithMalformedOrNonexistentEricIdentityReturnsForbidden() throws InvalidTokenPermissionException {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "$$$");
        request.addHeader("Eric-identity-type", "oauth2");
        request.addHeader("ERIC-Authorised-Key-Roles", "*");
        request.addHeader("Eric-Authorised-Token-Permissions", "acsp_number=WITA001 acsp_members=read acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete");

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Mockito.doReturn(Optional.empty()).when(acspMembersService).fetchActiveAcspMembership("$$$", "WITA001");

        final var response = new MockHttpServletResponse();

        new TokenPermissionsInterceptor().preHandle(request, response, null);
        new AdminPermissionsInterceptor().preHandle(request, response, null);

        assertFalse(sessionValidityInterceptor.preHandle(request, response, null));
        assertEquals(403, response.getStatus());
    }

    @Test
    void preHandleWithoutEricAuthorisedTokenPermissionsReturnsForbidden() throws InvalidTokenPermissionException {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        request.addHeader("Eric-identity-type", "oauth2");
        request.addHeader("ERIC-Authorised-Key-Roles", "*");

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Mockito.doReturn(Optional.empty()).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", null);

        final var response = new MockHttpServletResponse();

        new TokenPermissionsInterceptor().preHandle(request, response, null);
        new AdminPermissionsInterceptor().preHandle(request, response, null);

        assertFalse(sessionValidityInterceptor.preHandle(request, response, null));
        assertEquals(403, response.getStatus());
    }

    @Test
    void preHandleWithMalformedOrNonexistentAcspIdEricAuthorisedTokenPermissionReturnsForbidden() throws InvalidTokenPermissionException {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        request.addHeader("Eric-identity-type", "oauth2");
        request.addHeader("ERIC-Authorised-Key-Roles", "*");
        request.addHeader("Eric-Authorised-Token-Permissions", "xacsp_number=WITA001 acsp_members=read acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete");

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Mockito.doReturn(Optional.empty()).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", null);

        final var response = new MockHttpServletResponse();

        new TokenPermissionsInterceptor().preHandle(request, response, null);
        new AdminPermissionsInterceptor().preHandle(request, response, null);

        assertFalse(sessionValidityInterceptor.preHandle(request, response, null));
        assertEquals(403, response.getStatus());
    }

    static Stream<Arguments> preHandleWhereSessionRoleDiffersTestData() {
        final var ownerSessionRole = " acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read";
        final var adminSessionRole = " acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete acsp_members=read";
        final var standardSessionRole = " acsp_members=read";
        final var noSessionRole = "";
        return Stream.of(
                Arguments.of(ownerSessionRole, UserRoleEnum.ADMIN.getValue()),
                Arguments.of(ownerSessionRole, UserRoleEnum.STANDARD.getValue()),
                Arguments.of(adminSessionRole, UserRoleEnum.OWNER.getValue()),
                Arguments.of(adminSessionRole, UserRoleEnum.STANDARD.getValue()),
                Arguments.of(standardSessionRole, UserRoleEnum.OWNER.getValue()),
                Arguments.of(standardSessionRole, UserRoleEnum.ADMIN.getValue()),
                Arguments.of(noSessionRole, UserRoleEnum.OWNER.getValue()),
                Arguments.of(noSessionRole, UserRoleEnum.ADMIN.getValue()),
                Arguments.of(noSessionRole, UserRoleEnum.STANDARD.getValue())
        );
    }

    @ParameterizedTest
    @MethodSource("preHandleWhereSessionRoleDiffersTestData")
    void preHandleWhereSessionRoleDiffersFromDatabaseRoleReturnsForbidden(final String sessionRole, final String databaseRole) throws InvalidTokenPermissionException {
        final var acspMembersDao = testDataManager.fetchAcspMembersDaos("WIT004").getFirst();
        acspMembersDao.setUserRole(databaseRole);

        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        request.addHeader("Eric-identity-type", "oauth2");
        request.addHeader("ERIC-Authorised-Key-Roles", "*");
        request.addHeader("Eric-Authorised-Token-Permissions", "acsp_number=WITA001" + sessionRole);

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Mockito.doReturn(Optional.of(acspMembersDao)).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001");

        final var response = new MockHttpServletResponse();

        new TokenPermissionsInterceptor().preHandle(request, response, null);
        new AdminPermissionsInterceptor().preHandle(request, response, null);

        assertFalse(sessionValidityInterceptor.preHandle(request, response, null));
        assertEquals(403, response.getStatus());
    }

    @Test
    void preHandleReturnsTrueWhenSessionIsValid() throws InvalidTokenPermissionException {
        final var acspMembersDao = testDataManager.fetchAcspMembersDaos("WIT004").getFirst();

        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        request.addHeader("Eric-identity-type", "oauth2");
        request.addHeader("ERIC-Authorised-Key-Roles", "*");
        request.addHeader("Eric-Authorised-Token-Permissions", "acsp_number=WITA001 acsp_members=read acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete");

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Mockito.doReturn(Optional.of(acspMembersDao)).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001");

        final var response = new MockHttpServletResponse();

        new TokenPermissionsInterceptor().preHandle(request, response, null);
        new AdminPermissionsInterceptor().preHandle(request, response, null);

        assertTrue(sessionValidityInterceptor.preHandle(request, response, null));
    }

    @Test
    void preHandleReturnsTrueForAdminUser() throws InvalidTokenPermissionException {
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

        assertTrue(sessionValidityInterceptor.preHandle(request, response, null));
    }

}
