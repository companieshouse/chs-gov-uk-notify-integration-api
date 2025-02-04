package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.interceptor.TokenPermissionsInterceptor;
import uk.gov.companieshouse.api.util.security.InvalidTokenPermissionException;

import java.util.stream.Stream;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.RequestContextUtil.*;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class RequestContextUtilTest {

    @Test
    void isOAuth2RequestReturnsTrueIfEricIdentityTypeIsOAuth2() {
        final var request = new MockHttpServletRequest();
        request.addHeader(ERIC_IDENTITY_TYPE, "oauth2");

        final var requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Assertions.assertTrue(isOAuth2Request());
    }

    @Test
    void isOAuth2RequestReturnsFalseIfEricIdentityTypeIsNotOAuth2() {
        final var request = new MockHttpServletRequest();
        request.addHeader(ERIC_IDENTITY_TYPE, "key");

        final var requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Assertions.assertFalse(isOAuth2Request());
    }

    @Test
    void requestingUserIsPermittedToRetrieveAcspDataWithPermissionReturnsTrue() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Authorised-Token-Permissions", "acsp_members=read");
        new TokenPermissionsInterceptor().preHandle(request, null, null);

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Assertions.assertTrue(requestingUserIsPermittedToRetrieveAcspData());
    }

    @Test
    void requestingUserIsPermittedToRetrieveAcspDataWithoutPermissionReturnsFalse() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Authorised-Token-Permissions", "");
        new TokenPermissionsInterceptor().preHandle(request, null, null);

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Assertions.assertFalse(requestingUserIsPermittedToRetrieveAcspData());
    }

    @Test
    void requestingUserIsActiveMemberOfAcspWithoutPermissionReturnsFalse() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Authorised-Token-Permissions", "");
        new TokenPermissionsInterceptor().preHandle(request, null, null);

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Assertions.assertFalse(requestingUserIsActiveMemberOfAcsp("WITA001"));
    }

    @Test
    void requestingUserIsActiveMemberOfAcspWithPermissionReturnsTrue() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Authorised-Token-Permissions", "acsp_number=WITA001");
        new TokenPermissionsInterceptor().preHandle(request, null, null);

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Assertions.assertTrue(requestingUserIsActiveMemberOfAcsp("WITA001"));
    }

    @Test
    void requestingUserIsPermittedToCreateMembershipWithWithoutPermissionReturnsFalse() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Authorised-Token-Permissions", "");
        new TokenPermissionsInterceptor().preHandle(request, null, null);

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Assertions.assertFalse(requestingUserIsPermittedToCreateMembershipWith(UserRoleEnum.STANDARD));
    }

    @Test
    void requestingUserIsPermittedToCreateMembershipWithWithPermissionReturnsTrue() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Authorised-Token-Permissions", "acsp_members_standard=create");
        new TokenPermissionsInterceptor().preHandle(request, null, null);

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Assertions.assertTrue(requestingUserIsPermittedToCreateMembershipWith(UserRoleEnum.STANDARD));
    }

    @Test
    void requestingUserIsPermittedToUpdateUsersWithWithoutPermissionReturnsFalse() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Authorised-Token-Permissions", "");
        new TokenPermissionsInterceptor().preHandle(request, null, null);

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Assertions.assertFalse(requestingUserIsPermittedToUpdateUsersWith(UserRoleEnum.STANDARD));
    }

    @Test
    void requestingUserIsPermittedToUpdateUsersWithWithPermissionReturnsTrue() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Authorised-Token-Permissions", "acsp_members_admin=update");
        new TokenPermissionsInterceptor().preHandle(request, null, null);

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Assertions.assertFalse(requestingUserIsPermittedToUpdateUsersWith(UserRoleEnum.ADMIN));
    }

    @Test
    void requestingUserIsPermittedToRemoveUsersWithWithoutPermissionReturnsFalse() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Authorised-Token-Permissions", "");
        new TokenPermissionsInterceptor().preHandle(request, null, null);

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Assertions.assertFalse(requestingUserIsPermittedToRemoveUsersWith(UserRoleEnum.OWNER));
    }

    @Test
    void requestingUserIsPermittedToRemoveUsersWithWithPermissionReturnsTrue() throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Authorised-Token-Permissions", "acsp_members_owner=delete");
        new TokenPermissionsInterceptor().preHandle(request, null, null);

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Assertions.assertFalse(requestingUserIsPermittedToRemoveUsersWith(UserRoleEnum.OWNER));
    }

    static Stream<Arguments> fetchRequestingUsersActiveAcspNumberTestData() {
        return Stream.of(
                Arguments.of("", null),
                Arguments.of("acsp_number=TSA001", "TSA001"),
                Arguments.of("xacsp_number=TSA001", null),
                Arguments.of("acspx_number=TSA001", null),
                Arguments.of("acsp_numberx=TSA001", null),
                Arguments.of("acsp_number=$$$", null),
                Arguments.of("acsp_members_owner=create", null),
                Arguments.of("acsp_number=TSA001 acsp_members_owner=create", "TSA001"),
                Arguments.of("acsp_members_owner=create acsp_number=TSA001", "TSA001"),
                Arguments.of("acsp_members_owner=create acsp_number=TSA001 acsp_members_admin=create", "TSA001")
        );
    }

    @ParameterizedTest
    @MethodSource("fetchRequestingUsersActiveAcspNumberTestData")
    void fetchRequestingUsersActiveAcspNumberRetrievesAcspNumberFromSession(final String ericAuthorisedTokenPermissions, final String expectedAcspNumber) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions);

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Assertions.assertEquals(expectedAcspNumber, fetchRequestingUsersActiveAcspNumber());
    }

    static Stream<Arguments> fetchRequestingUsersRoleTestData() {
        return Stream.of(
                Arguments.of(UserRoleEnum.OWNER, "acsp_number=TSA001 acsp_members=read acsp_members_owners=create,update,delete acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete"),
                Arguments.of(UserRoleEnum.ADMIN, "acsp_number=TSA001 acsp_members=read acsp_members_admins=create,update,delete acsp_members_standard=create,update,delete"),
                Arguments.of(UserRoleEnum.STANDARD, "acsp_number=TSA001 acsp_members=read"),
                Arguments.of(null, "")
        );
    }

    @ParameterizedTest
    @MethodSource("fetchRequestingUsersRoleTestData")
    void fetchRequestingUsersRoleRetrievesRoleFromSession(final UserRoleEnum expectedRole, final String ericAuthorisedTokenPermissions) throws InvalidTokenPermissionException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Authorised-Token-Permissions", ericAuthorisedTokenPermissions);
        new TokenPermissionsInterceptor().preHandle(request, null, null);

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        Assertions.assertEquals(expectedRole, fetchRequestingUsersRole());
    }

}
