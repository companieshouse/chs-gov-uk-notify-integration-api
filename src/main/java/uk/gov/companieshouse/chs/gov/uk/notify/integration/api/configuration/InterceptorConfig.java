package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.api.interceptor.TokenPermissionsInterceptor;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.interceptor.*;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    private static final String OAUTH_PROTECTED_ENDPOINTS = "/user/**";
    private static final String OAUTH_AND_KEY_PROTECTED_ENDPOINTS = "/acsps/**";
    private static final String HEALTH_CHECK_ENDPOINT = "/*/healthcheck";

    private final LoggingInterceptor loggingInterceptor;
    private final AuthorizationInterceptor authorizationInterceptor;
    private final AuthorizationAndInternalUserInterceptors authorizationAndInternalUserInterceptors;
    private final SessionValidityInterceptor sessionValidityInterceptor;

    public InterceptorConfig(final LoggingInterceptor loggingInterceptor, @Qualifier("authorizationInterceptor") final AuthorizationInterceptor authorizationInterceptor, final AuthorizationAndInternalUserInterceptors authorizationAndInternalUserInterceptors, final SessionValidityInterceptor sessionValidityInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
        this.authorizationInterceptor = authorizationInterceptor;
        this.authorizationAndInternalUserInterceptors = authorizationAndInternalUserInterceptors;
        this.sessionValidityInterceptor = sessionValidityInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull final InterceptorRegistry registry) {
        addLoggingInterceptor(registry);
        addEricInterceptors(registry);
    }

    private void addLoggingInterceptor(final InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor);
    }

    private void addEricInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor)
                .addPathPatterns(OAUTH_PROTECTED_ENDPOINTS)
                .excludePathPatterns(HEALTH_CHECK_ENDPOINT, OAUTH_AND_KEY_PROTECTED_ENDPOINTS);

        registry.addInterceptor(authorizationAndInternalUserInterceptors)
                .addPathPatterns(OAUTH_AND_KEY_PROTECTED_ENDPOINTS)
                .excludePathPatterns(HEALTH_CHECK_ENDPOINT, OAUTH_PROTECTED_ENDPOINTS);

        registry.addInterceptor(new AdminPermissionsInterceptor())
                .addPathPatterns(OAUTH_PROTECTED_ENDPOINTS, OAUTH_AND_KEY_PROTECTED_ENDPOINTS)
                .excludePathPatterns(HEALTH_CHECK_ENDPOINT);

        registry.addInterceptor(new TokenPermissionsInterceptor())
                .addPathPatterns(OAUTH_AND_KEY_PROTECTED_ENDPOINTS)
                .excludePathPatterns(HEALTH_CHECK_ENDPOINT, OAUTH_PROTECTED_ENDPOINTS);

        registry.addInterceptor(sessionValidityInterceptor)
                .addPathPatterns(OAUTH_AND_KEY_PROTECTED_ENDPOINTS)
                .excludePathPatterns(HEALTH_CHECK_ENDPOINT, OAUTH_PROTECTED_ENDPOINTS);

        registry.addInterceptor(new AcspDataRetrievalPermissionInterceptor())
                .addPathPatterns(OAUTH_AND_KEY_PROTECTED_ENDPOINTS)
                .excludePathPatterns(HEALTH_CHECK_ENDPOINT, OAUTH_PROTECTED_ENDPOINTS);

    }

}
