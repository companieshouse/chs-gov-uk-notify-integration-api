package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;


import jakarta.validation.ConstraintDeclarationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Tag("unit-test")
public class SenderRestApiValidationTests {

    @Autowired
    @Qualifier("senderRestApi")
    private SenderRestApi senderRestApi;

    @ParameterizedTest(name = "When email request is {0} and callback is {1}, exception should be thrown")
    @CsvSource({
            "null, valid",
            "null, null",
            "valid, null"
    })
    public void testEmailRequestValidation(String requestState, String callbackState) {
        GovUkEmailDetailsRequest request = "null".equals(requestState) ? null : new GovUkEmailDetailsRequest();
        Runnable callback = "null".equals(callbackState) ? null : () -> {};

        assertThrows(ConstraintDeclarationException.class, () ->
                senderRestApi.sendEmail(request, String.valueOf(callback))
        );
    }

}
