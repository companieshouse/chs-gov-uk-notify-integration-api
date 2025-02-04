package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.UserContext;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {


    private final InterceptorHelper interceptorHelper;

    public AuthorizationInterceptor(InterceptorHelper interceptorHelper) {
        this.interceptorHelper = interceptorHelper;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        return interceptorHelper.isOauth2User(request, response) && interceptorHelper.isValidAuthorisedUser(request, response);
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }
}
