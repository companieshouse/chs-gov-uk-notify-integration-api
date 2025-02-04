package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${api.url}")
    private String apiUrl;

    @Value("${chs.internal.api.key}")
    private String chsInternalApiKey;

    @Bean
    public WebClient acspWebClient() {
        return WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", chsInternalApiKey)
                .build();
    }

}
