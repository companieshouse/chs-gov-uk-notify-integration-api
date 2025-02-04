package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

@Tag("unit-test")
class WebClientConfigTest {

    @Test
    void webClientIsCreatedCorrectly() {
        Assertions.assertTrue(WebClient.class.isAssignableFrom(new WebClientConfig().acspWebClient().getClass()));
    }

}
