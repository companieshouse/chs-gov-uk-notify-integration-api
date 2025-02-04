package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.util.security.TokenPermissions;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_TOKEN_PERMISSIONS;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.Permission.Key.*;
import static uk.gov.companieshouse.api.util.security.Permission.Value.*;

public class RequestContextUtil {

    private static final String OAUTH2_REQUEST_TYPE = "oauth2";
    private static final String TOKEN_PERMISSIONS = "token_permissions";
    private static final Pattern SERVICE_NUMBER_PATTERN = Pattern.compile("(?<=^|\\s)acsp_number=([0-9A-Za-z-_]{0,32})(?=\\s|$)");
    private static final String X_REQUEST_ID = "X-Request-Id";
    private static final String UNKNOWN_X_REQUEST_ID = "unknown";

    private RequestContextUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static String getXRequestId() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .map(requestAttributes -> (ServletRequestAttributes) requestAttributes)
                .map(ServletRequestAttributes::getRequest)
                .map(httpServletRequest -> httpServletRequest.getHeader(X_REQUEST_ID))
                .orElse(UNKNOWN_X_REQUEST_ID);
    }

    private static String getEricIdentityType() {
        final var requestAttributes = RequestContextHolder.getRequestAttributes();
        final var servletRequestAttributes = ((ServletRequestAttributes) requestAttributes);
        final var httpServletRequest = Objects.requireNonNull(servletRequestAttributes).getRequest();
        return httpServletRequest.getHeader(ERIC_IDENTITY_TYPE);
    }

    public static boolean isOAuth2Request() {
        return getEricIdentityType().equals(OAUTH2_REQUEST_TYPE);
    }

    private static TokenPermissions getTokenPermissions() {
        final var requestAttributes = RequestContextHolder.getRequestAttributes();
        final var servletRequestAttributes = ((ServletRequestAttributes) requestAttributes);
        final var httpServletRequest = Objects.requireNonNull(servletRequestAttributes).getRequest();
        return (TokenPermissions) httpServletRequest.getAttribute(TOKEN_PERMISSIONS);
    }

    public static boolean requestingUserIsPermittedToRetrieveAcspData() {
        final var tokenPermissions = getTokenPermissions();
        return tokenPermissions.hasPermission(ACSP_MEMBERS, READ);
    }

    public static boolean requestingUserIsActiveMemberOfAcsp(final String acspNumber) {
        final var tokenPermissions = getTokenPermissions();
        return tokenPermissions.hasPermission(ACSP_NUMBER, acspNumber);
    }

    private static boolean requestingUserIsPermittedToPerformActionOnUserWithRole(final String action, final UserRoleEnum role) {
        final var tokenPermissions = getTokenPermissions();
        return switch (role) {
            case UserRoleEnum.OWNER -> tokenPermissions.hasPermission(ACSP_MEMBERS_OWNERS, action);
            case UserRoleEnum.ADMIN -> tokenPermissions.hasPermission(ACSP_MEMBERS_ADMINS, action);
            case UserRoleEnum.STANDARD -> tokenPermissions.hasPermission(ACSP_MEMBERS_STANDARD, action);
        };
    }

    public static boolean requestingUserIsPermittedToCreateMembershipWith(final UserRoleEnum role) {
        return requestingUserIsPermittedToPerformActionOnUserWithRole(CREATE, role);
    }

    public static boolean requestingUserIsPermittedToUpdateUsersWith(final UserRoleEnum role) {
        return requestingUserIsPermittedToPerformActionOnUserWithRole(UPDATE, role);
    }

    public static boolean requestingUserIsPermittedToRemoveUsersWith(final UserRoleEnum role) {
        return requestingUserIsPermittedToPerformActionOnUserWithRole(DELETE, role);
    }

    public static String fetchRequestingUsersActiveAcspNumber() {
        final var requestAttributes = RequestContextHolder.getRequestAttributes();
        final var servletRequestAttributes = ((ServletRequestAttributes) requestAttributes);
        final var httpServletRequest = Objects.requireNonNull(servletRequestAttributes).getRequest();
        final var ericAuthorisedTokenPermissions = Optional.ofNullable(httpServletRequest.getHeader(ERIC_AUTHORISED_TOKEN_PERMISSIONS)).orElse("");

        final var matcher = SERVICE_NUMBER_PATTERN.matcher(ericAuthorisedTokenPermissions);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static UserRoleEnum fetchRequestingUsersRole() {
        if (requestingUserIsPermittedToCreateMembershipWith(UserRoleEnum.OWNER)) return UserRoleEnum.OWNER;
        if (requestingUserIsPermittedToCreateMembershipWith(UserRoleEnum.ADMIN)) return UserRoleEnum.ADMIN;
        if (requestingUserIsPermittedToRetrieveAcspData()) return UserRoleEnum.STANDARD;
        return null;
    }

}
