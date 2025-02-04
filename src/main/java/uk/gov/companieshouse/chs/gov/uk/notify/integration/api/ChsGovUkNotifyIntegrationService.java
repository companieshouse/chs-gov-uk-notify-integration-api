package uk.gov.companieshouse.chs.gov.uk.notify.integration.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.StaticPropertyUtil;

@SpringBootApplication
public class ChsGovUkNotifyIntegrationService {

    StaticPropertyUtil staticPropertyUtil;

    @Autowired
    public ChsGovUkNotifyIntegrationService(StaticPropertyUtil staticPropertyUtil) {
        this.staticPropertyUtil = staticPropertyUtil;
    }

    public static void main(String[] args) {
        SpringApplication.run(ChsGovUkNotifyIntegrationService.class, args);
    }

}