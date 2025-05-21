package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import java.math.BigDecimal;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.api.chs.notification.model.EmailDetails;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.RecipientDetailsEmail;
import uk.gov.companieshouse.api.chs.notification.model.SenderDetails;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;
import uk.gov.companieshouse.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class SenderRestApiTests {

    @Mock
    private GovUkNotifyService govUKNotifyEmailFacade;

    @Mock
    private NotificationDatabaseService notificationDatabaseService;

    // This allows us to see what is logged during unit test execution, assuming that is
    // thought useful, when the logger is injected. If what is logged is
    // not of interest, then just inject the logger with @Mock.
    @SuppressWarnings("java:S1068") // This field is actually used.
    private final Logger logger = mock(Logger.class, withSettings().verboseLogging());

    @InjectMocks
    private SenderRestApi notifyIntegrationSenderController;

    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_TEMPLATE_ID = "valid-template-id";
    private static final String VALID_REFERENCE = "valid-reference";
    private static final Map<String, String> VALID_PERSONALISATION = Map.of("name", "Test User");
    private static final String XHEADER = "1";

    @Test
    void When_EmailRequestIsValid_Expect_EmailMessageIsSentSuccessfully(){
        EmailDetails emailDetails = new EmailDetails();
        RecipientDetailsEmail recipientDetailsEmail = new RecipientDetailsEmail();
        SenderDetails senderDetails = new SenderDetails();
        GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();
        govUkEmailDetailsRequest.setSenderDetails(senderDetails
                .emailAddress("john.doe@example.com")
                .userId("9876543")
                .name("John Doe")
                .reference(VALID_REFERENCE)
                .appId("chips.send_email"));
        govUkEmailDetailsRequest.setEmailDetails(emailDetails
                .templateId(VALID_TEMPLATE_ID)
                .templateVersion(BigDecimal.valueOf(1))
                .personalisationDetails(new JSONObject().put("name", "Test User").toString()));
        govUkEmailDetailsRequest.setRecipientDetails(recipientDetailsEmail
                .emailAddress(VALID_EMAIL)
                .name("john doe"));

        when(govUKNotifyEmailFacade.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_REFERENCE, VALID_PERSONALISATION)).thenReturn(new GovUkNotifyService.EmailResp(true, null));

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendEmail(govUkEmailDetailsRequest, XHEADER);

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        Assertions.assertNotNull(response);
    }

    @Test
    void When_EmailRequestIsInValid_Expect_InternalSeverErrorResponse(){
        EmailDetails emailDetails = new EmailDetails();
        RecipientDetailsEmail recipientDetailsEmail = new RecipientDetailsEmail();
        SenderDetails senderDetails = new SenderDetails();
        GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();
        govUkEmailDetailsRequest.setSenderDetails(senderDetails
                .emailAddress("john.doe@example.com")
                .userId("9876543")
                .name("John Doe")
                .reference(VALID_REFERENCE)
                .appId("chips.send_email"));
        govUkEmailDetailsRequest.setEmailDetails(emailDetails
                .templateId(VALID_TEMPLATE_ID)
                .templateVersion(BigDecimal.valueOf(1))
                .personalisationDetails(new JSONObject().put("name", "Test User").toString()));
        govUkEmailDetailsRequest.setRecipientDetails(recipientDetailsEmail
                .emailAddress(VALID_EMAIL)
                .name("john doe"));

        when(govUKNotifyEmailFacade.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_REFERENCE, VALID_PERSONALISATION)).thenReturn(new GovUkNotifyService.EmailResp(false, null));

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendEmail(govUkEmailDetailsRequest, XHEADER);

        assertThat(response.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
        Assertions.assertNotNull(response);

    }

    @ParameterizedTest(name = "When request is {0}, null exception should be thrown")
    @CsvSource({
            "null, valid"
    })
    void testGovUkEmailDetailsRequestValidation() {
        GovUkEmailDetailsRequest request =  new GovUkEmailDetailsRequest();

        assertThrowsExactly(NullPointerException.class, () ->
                notifyIntegrationSenderController.sendEmail(request, XHEADER)
        );
    }

    @ParameterizedTest(name = "When request is {0}, null exception should be thrown")
    @CsvSource({
            "null, valid"
    })
    void testGovUkEmailDetailsRequestValidationMissingDetails() {
        RecipientDetailsEmail recipientDetailsEmail = new RecipientDetailsEmail();
        SenderDetails senderDetails = new SenderDetails();
        GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();
        govUkEmailDetailsRequest.setSenderDetails(senderDetails
                .emailAddress("john.doe@example.com")
                .userId("9876543")
                .name("John Doe")
                .reference("ref")
                .appId("chips.send_email"));
        govUkEmailDetailsRequest.setRecipientDetails(recipientDetailsEmail
                .emailAddress(VALID_EMAIL)
                .name("john doe"));

        assertThrowsExactly(NullPointerException.class, () ->
                notifyIntegrationSenderController.sendEmail(govUkEmailDetailsRequest, XHEADER)
        );
    }

    @ParameterizedTest(name = "When request is {0}, exception should be thrown")
    @CsvSource({
            "null, null"
    })
    void testGovUkEmailDetailsRequestValidationMissingHeader() {
        GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();
        assertThrowsExactly(NullPointerException.class, () ->
                notifyIntegrationSenderController.sendEmail(govUkEmailDetailsRequest, null)
        );
    }


}
