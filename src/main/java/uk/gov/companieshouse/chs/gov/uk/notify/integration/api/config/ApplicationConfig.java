package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;
import uk.gov.service.notify.NotificationClient;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;

@Configuration
public class ApplicationConfig {

    @Value("${gov.uk.notify.api.key}")
    private String apiKey;

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

    /**
     * Creates NotificationClient that provides communication with
     * the Gov Notify service. Only used in production environments
     * or when the mock is not active in other environments.
     *
     * @return the notification client
     */
    @Bean
    @Profile("!local & !dev & !test")
    public NotificationClient getNotificationClient() {
        return new NotificationClient(apiKey);
    }
}
