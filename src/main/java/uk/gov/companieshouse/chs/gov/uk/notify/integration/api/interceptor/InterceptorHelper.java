package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Objects;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.UserContext;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.UsersService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.util.security.AuthorisationUtil;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class InterceptorHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    private final UsersService usersService;

    public InterceptorHelper(UsersService usersService) {
        this.usersService = usersService;
    }

    protected boolean isOauth2User(HttpServletRequest request, HttpServletResponse response) {
        final var hasEricIdentity = Objects.nonNull(request.getHeader("Eric-Identity"));
        final var hasEricIdentityType = Objects.nonNull(request.getHeader("Eric-Identity-Type"));
        if (hasEricIdentity && hasEricIdentityType && AuthorisationUtil.isOauth2User(request)) {
            return true;
        }

        LOGGER.debugRequest(request, "invalid user", null);
        response.setStatus(401);
        return false;
    }

    protected boolean isValidAuthorisedUser(HttpServletRequest request, HttpServletResponse response) {
        String identity = AuthorisationUtil.getAuthorisedIdentity(request);

        try {
            final var userDetails = usersService.fetchUserDetails(identity);
            LOGGER.infoContext(userDetails.getUserId(), "User details fetched and stored in context : " + identity, null);
            UserContext.setLoggedUser(userDetails);
            return true;
        } catch (NotFoundRuntimeException e) {
            LOGGER.debugRequest(request, "no user found with identity [" + identity + "]", null);
            response.setStatus(403);
            return false;
        }

    }

}
