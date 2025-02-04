package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspMembersService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.RequestContextUtil.*;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;

@Component
public class SessionValidityInterceptor implements HandlerInterceptor {

    private final AcspMembersService acspMembersService;

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);
    private static final String X_REQUEST_ID = "X-Request-Id";
    private static final String HAS_ADMIN_PRIVILEGE = "has_admin_privilege";

    public SessionValidityInterceptor(final AcspMembersService acspMembersService) {
        this.acspMembersService = acspMembersService;
    }

    private boolean sessionIsValid(final String requestingUserId, final String requestingUsersActiveAcspNumber) {
        final var requestingUsersActiveMembershipOptional = acspMembersService.fetchActiveAcspMembership(requestingUserId, requestingUsersActiveAcspNumber);
        if (requestingUsersActiveMembershipOptional.isEmpty()) {
            return false;
        }
        final var requestingUsersActiveMembership = requestingUsersActiveMembershipOptional.get();

        final var currentUserRole = UserRoleEnum.fromValue(requestingUsersActiveMembership.getUserRole());
        final var sessionUserRole = fetchRequestingUsersRole();
        return currentUserRole.equals(sessionUserRole);
    }

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {
        if (!isOAuth2Request() || (boolean) request.getAttribute(HAS_ADMIN_PRIVILEGE)) {
            return true;
        }

        final var requestingUserId = request.getHeader(ERIC_IDENTITY);
        final var requestingUsersActiveAcspNumber = fetchRequestingUsersActiveAcspNumber();
        if (sessionIsValid(requestingUserId, requestingUsersActiveAcspNumber)) {
            return true;
        }

        final var xRequestId = request.getHeader(X_REQUEST_ID);
        LOGGER.errorContext(xRequestId, new Exception("Session is out of sync with the database"), null);
        response.setStatus(403);
        return false;
    }

}