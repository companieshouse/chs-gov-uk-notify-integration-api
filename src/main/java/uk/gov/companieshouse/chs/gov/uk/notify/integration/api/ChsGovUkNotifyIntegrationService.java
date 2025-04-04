package uk.gov.companieshouse.chs.gov.uk.notify.integration.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This is the entry point for this service
 */
@SpringBootApplication
public class ChsGovUkNotifyIntegrationService {

    public static final String APPLICATION_NAMESPACE = "chs-gov-uk-notify-integration-api";

    public static void main(String[] args) {
        SpringApplication.run(ChsGovUkNotifyIntegrationService.class, args);
    }

}