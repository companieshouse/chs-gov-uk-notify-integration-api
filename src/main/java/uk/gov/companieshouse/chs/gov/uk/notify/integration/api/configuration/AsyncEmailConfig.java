package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@ComponentScan(basePackages = "uk.gov.companieshouse.email_producer")
public class AsyncEmailConfig {
}
