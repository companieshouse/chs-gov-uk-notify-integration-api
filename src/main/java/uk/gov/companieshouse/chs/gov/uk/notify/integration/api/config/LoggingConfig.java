package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.config;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Configuration
public class LoggingConfig {

    @Bean
    Logger getLogger(){
        return LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    }

}
