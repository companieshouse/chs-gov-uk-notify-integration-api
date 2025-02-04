package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.interceptor;

import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.UserContext;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class AuthorizationAndInternalUserInterceptors implements HandlerInterceptor {


    private static final Logger LOGGER = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);
    private static final String X_REQUEST_ID = "X-Request-Id";

    private final InternalUserInterceptor internalUserInterceptor;
    private final InterceptorHelper interceptorHelper;

    public AuthorizationAndInternalUserInterceptors(InterceptorHelper interceptorHelper) {
        this.interceptorHelper = interceptorHelper;
        this.internalUserInterceptor = new InternalUserInterceptor(StaticPropertyUtil.APPLICATION_NAMESPACE);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        final var xRequestId = request.getHeader(X_REQUEST_ID);
        final var ericIdentityType = request.getHeader(ERIC_IDENTITY_TYPE);

        if (Objects.isNull(ericIdentityType)) {
            LOGGER.errorContext(xRequestId, new Exception("ERIC-Identity-Type not provided"), null);
            response.setStatus(401);
            return false;
        }

        if (ericIdentityType.equals("oauth2")) {
            return interceptorHelper.isOauth2User(request, response) && interceptorHelper.isValidAuthorisedUser(request, response);
        } else if (ericIdentityType.equals("key")) {
            try {
                return internalUserInterceptor.preHandle(request, response, handler);
            } catch (IOException e) {
                response.setStatus(401);
                return false;
            }
        }

        LOGGER.errorContext(xRequestId, new Exception(String.format("Invalid ericIdentityType provided: %s", ericIdentityType)), null);
        response.setStatus(401);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }

}
