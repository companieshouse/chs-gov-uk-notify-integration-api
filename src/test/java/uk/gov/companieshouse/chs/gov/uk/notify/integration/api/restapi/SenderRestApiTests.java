package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import java.io.IOException;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.api.chs.notification.model.EmailDetails;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.RecipientDetailsEmail;
import uk.gov.companieshouse.api.chs.notification.model.SenderDetails;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.letterdispatcher.LetterDispatcher;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.Postage;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.service.notify.LetterResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createSampleLetterRequestWithTemplateId;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class SenderRestApiTests {

    @Mock
    private GovUkNotifyService govUKNotifyEmailFacade;

    @Mock
    private NotificationDatabaseService notificationDatabaseService;

    @Mock
    private LetterDispatcher letterDispatcher;

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
    private static final Map<String, String> VALID_PERSONALISATION = Map.of(
            "name", "Test User",
            "verification_due_date", "15 February 2024",
            "welsh_verification_due_date", "15 Chwefror 2024"
    );
    private static final String REQUEST_BODY_PERSONALISATION = new JSONObject().put("name", "Test User").put("verification_due_date", "15 February 2024").toString();
    private static final String XHEADER = "1";

    @Test
    void whenEmailRequestIsValidExpectEmailMessageIsSentSuccessfully(){
        EmailDetails emailDetails = new EmailDetails();
        RecipientDetailsEmail recipientDetailsEmail = new RecipientDetailsEmail();
        SenderDetails senderDetails = new SenderDetails();
        GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();
        govUkEmailDetailsRequest.setSenderDetails(senderDetails
                .emailAddress("john.doe@example.com")
                .userId("9876543")
                .name("John Doe")
                .reference(VALID_REFERENCE)
                .appId("chips"));
        govUkEmailDetailsRequest.setEmailDetails(emailDetails
                .templateId(VALID_TEMPLATE_ID).
                personalisationDetails(REQUEST_BODY_PERSONALISATION)
        );
        govUkEmailDetailsRequest.setRecipientDetails(recipientDetailsEmail
                .emailAddress(VALID_EMAIL)
                .name("john doe"));

        when(govUKNotifyEmailFacade.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_REFERENCE, VALID_PERSONALISATION)).thenReturn(new GovUkNotifyService.EmailResp(true, null));

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendEmail(govUkEmailDetailsRequest, XHEADER);

        verify(notificationDatabaseService).storeEmail(govUkEmailDetailsRequest);
        verify(govUKNotifyEmailFacade).sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_REFERENCE, VALID_PERSONALISATION);
        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        Assertions.assertNotNull(response);
    }

    @Test
    void whenEmailContainsBadDateVariablesExpectFailedToPublishWelshDatesError(){
        EmailDetails emailDetails = new EmailDetails();
        RecipientDetailsEmail recipientDetailsEmail = new RecipientDetailsEmail();
        SenderDetails senderDetails = new SenderDetails();
        GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();
        govUkEmailDetailsRequest.setSenderDetails(senderDetails
                .emailAddress("john.doe@example.com")
                .userId("9876543")
                .name("John Doe")
                .reference(VALID_REFERENCE)
                .appId("chips"));
        govUkEmailDetailsRequest.setEmailDetails(emailDetails
                .templateId(VALID_TEMPLATE_ID)
                .personalisationDetails(
                        new JSONObject()
                                .put("name", "Test User")
                                .put("verification_due_date", "15  2024")
                                .toString()
                )
        );
        govUkEmailDetailsRequest.setRecipientDetails(recipientDetailsEmail
                .emailAddress(VALID_EMAIL)
                .name("john doe"));

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendEmail(govUkEmailDetailsRequest, XHEADER);


        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertNotNull(response);

    }

    @Test
    void whenEmailRequestIsInValidExpectInternalSeverErrorResponse(){
        EmailDetails emailDetails = new EmailDetails();
        RecipientDetailsEmail recipientDetailsEmail = new RecipientDetailsEmail();
        SenderDetails senderDetails = new SenderDetails();
        GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();
        govUkEmailDetailsRequest.setSenderDetails(senderDetails
                .emailAddress("john.doe@example.com")
                .userId("9876543")
                .name("John Doe")
                .reference(VALID_REFERENCE)
                .appId("chips"));
        govUkEmailDetailsRequest.setEmailDetails(emailDetails
                .templateId(VALID_TEMPLATE_ID)
                .personalisationDetails(REQUEST_BODY_PERSONALISATION)
        );
        govUkEmailDetailsRequest.setRecipientDetails(recipientDetailsEmail
                .emailAddress(VALID_EMAIL)
                .name("john doe"));

        when(govUKNotifyEmailFacade.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_REFERENCE, VALID_PERSONALISATION)).thenReturn(new GovUkNotifyService.EmailResp(false, null));

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendEmail(govUkEmailDetailsRequest, XHEADER);

        assertThat(response.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
        Assertions.assertNotNull(response);

    }

    @Test
    void testGovUkEmailDetailsRequestValidation() {
        GovUkEmailDetailsRequest request =  new GovUkEmailDetailsRequest();

        assertThrowsExactly(NullPointerException.class, () ->
                notifyIntegrationSenderController.sendEmail(request, XHEADER)
        );
    }

    @Test
    void testGovUkEmailDetailsRequestValidationMissingDetails() {
        RecipientDetailsEmail recipientDetailsEmail = new RecipientDetailsEmail();
        SenderDetails senderDetails = new SenderDetails();
        GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();
        govUkEmailDetailsRequest.setSenderDetails(senderDetails
                .emailAddress("john.doe@example.com")
                .userId("9876543")
                .name("John Doe")
                .reference("ref")
                .appId("chips"));
        govUkEmailDetailsRequest.setRecipientDetails(recipientDetailsEmail
                .emailAddress(VALID_EMAIL)
                .name("john doe"));

        assertThrowsExactly(NullPointerException.class, () ->
                notifyIntegrationSenderController.sendEmail(govUkEmailDetailsRequest, XHEADER)
        );
    }

    @Test
    void testGovUkEmailDetailsRequestValidationMissingHeader() {
        GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();
        assertThrowsExactly(NullPointerException.class, () ->
                notifyIntegrationSenderController.sendEmail(govUkEmailDetailsRequest, null)
        );
    }

    @ParameterizedTest
    @CsvSource(value = { 
            "ANY,version",
            "null,other", 
            "null,CSIDVDEFLET_v1", 
            "null,CSIDVDEFLET_v1.1",
            "null,IDVPSCDEFAULT_v1", 
            "null,IDVPSCDEFAULT_v1.1", 
            "IDVPSCDEFAULT,v1.0",
            "CSIDVDEFLET,v1.0"}, nullValues = { "null" })
    void sendLetter_shouldReturnCreated_defaultPostage(String letterId, String templateId) throws Exception {
        String contextId = "context1234";
        GovUkLetterDetailsRequest req = createSampleLetterRequestWithTemplateId("chips", letterId, templateId);

        var senderDetails = req.getSenderDetails();
        var letterDetails = req.getLetterDetails();
        Mockito.when(letterDispatcher.sendLetter(Postage.ECONOMY, senderDetails.getReference(),
                senderDetails.getAppId(), letterDetails.getLetterId(),
                letterDetails.getTemplateId(), req.getRecipientDetails().getPhysicalAddress(),
                letterDetails.getPersonalisationDetails(), contextId))
                .thenReturn(new GovUkNotifyService.LetterResp(true, null));

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendLetter(req, contextId);

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
    }

    @CsvSource(value = { 
            "null,direction_letter_v1",
            "null,new_psc_direction_letter_v1",
            "null,transitional_non_director_psc_information_letter_v1",
            "null,extension_acceptance_letter_v1",
            "null,second_extension_acceptance_letter_v1",
            "IDVPSCEXT1,v1.0",
            "IDVPSCEXT2,v1.0",
            "IDVPSCDIRNEW,v1.0",
            "IDVPSCDIRTRAN,v1.0"}, nullValues = { "null" })
    void sendLetter_shouldReturnCreated_whenSecondClassPostage(String letterId, String templateId) throws Exception {
        String contextId = "context5678";
        GovUkLetterDetailsRequest req = createSampleLetterRequestWithTemplateId("chips", letterId, templateId);

        var senderDetails = req.getSenderDetails();
        var letterDetails = req.getLetterDetails();
        Mockito.when(letterDispatcher.sendLetter(Postage.SECOND_CLASS, senderDetails.getReference(),
                senderDetails.getAppId(), letterDetails.getLetterId(),
                letterDetails.getTemplateId(), req.getRecipientDetails().getPhysicalAddress(),
                letterDetails.getPersonalisationDetails(), contextId))
                .thenReturn(new GovUkNotifyService.LetterResp(true, null));

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendLetter(req, "context5678");

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
    }

    @Test
    void sendLetter_shouldReturnInternalServerError_onDispatcherFailure() throws Exception {
        GovUkLetterDetailsRequest req = createSampleLetterRequestWithTemplateId("chips", "letterId", "other");
        Mockito.when(letterDispatcher.sendLetter(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new GovUkNotifyService.LetterResp(false, new LetterResponse("{ id: bff67204-a33f-4dcf-8ec3-49fa5fce0321 }")));

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendLetter(req, "context9999");

        assertThat(response.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
    }

    @Test
    void sendLetter_shouldReturnInternalServerError_onIOException() throws Exception {
        GovUkLetterDetailsRequest req = createSampleLetterRequestWithTemplateId("chips", "letterId", "other");
        Mockito.when(letterDispatcher.sendLetter(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IOException("PDF error"));

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendLetter(req, "context0000");

        assertThat(response.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
    }


}
