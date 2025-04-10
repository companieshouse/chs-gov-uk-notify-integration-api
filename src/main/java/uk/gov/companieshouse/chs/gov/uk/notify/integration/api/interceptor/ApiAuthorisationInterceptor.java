package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.util.DataMap;

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static uk.gov.companieshouse.api.util.security.AuthorisationUtil.hasInternalUserRole;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.API_KEY_IDENTITY_TYPE;

@Component
public class ApiAuthorisationInterceptor implements HandlerInterceptor {

    private static final String X_REQUEST_ID = "X-Request-ID";

    private final Logger logger;

    public ApiAuthorisationInterceptor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        final var identityType = getIdentityType(request);
        var isApiKeyRequest = API_KEY_IDENTITY_TYPE.equals(identityType);
        if (isApiKeyRequest) {
            return validateApi(request, response);
        }
        var logMap = new DataMap.Builder()
                .requestId(request.getHeader(X_REQUEST_ID))
                .build().getLogMap();
        logger.error("Unrecognised identity type: " + identityType + ".", logMap);
        response.setStatus(UNAUTHORIZED.value());
        return false;
    }

    private boolean validateApi(HttpServletRequest request, HttpServletResponse response) {
        final var builder = new DataMap.Builder()
                .requestId(request.getHeader(X_REQUEST_ID));

        if (hasInternalUserRole(request) && (POST.matches(request.getMethod()))) {
            logger.info("Internal API is permitted to create the resource.",
                    builder.build().getLogMap());
            return true;
        } else {
            builder.status(UNAUTHORIZED.getReasonPhrase());
            logger.error("API is not permitted to perform a "
                    + request.getMethod() + ".", builder.build().getLogMap());
            response.setStatus(UNAUTHORIZED.value());
            return false;
        }
    }

    private static String getIdentityType(HttpServletRequest request) {
        return request.getHeader(ERIC_IDENTITY_TYPE);
    }

}
