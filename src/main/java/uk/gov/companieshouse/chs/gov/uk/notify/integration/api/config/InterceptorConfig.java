package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.config;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;

@Configuration
public class InterceptorConfig {

    /**
     * Creates InternalUserInterceptor which checks the Api key has internal app
     * privileges to access the application.
     *
     * @return the internal user interceptor
     */
    @Bean
    public InternalUserInterceptor internalUserInterceptor() {
        return new InternalUserInterceptor(APPLICATION_NAMESPACE);
    }

}
