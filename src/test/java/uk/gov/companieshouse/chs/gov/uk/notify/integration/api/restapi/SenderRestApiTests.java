package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import org.json.JSONObject;
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
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.SenderDetails;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.letterdispatcher.LetterDispatcher;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.letterdispatcher.LetterReference;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService.EmailResp;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.Postage;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.service.notify.LetterResponse;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class SenderRestApiTests {
    private static final String VALID_TEMPLATE_ID = "valid-template-id";
    private static final String VALID_REFERENCE = "valid-reference";
    private static final Map<String, String> VALID_PERSONALISATION = Map.of(
            "name", "Test User",
            "verification_due_date", "15 February 2024",
            "welsh_verification_due_date", "15 Chwefror 2024"
    );
    private static final String REQUEST_BODY_PERSONALISATION = new JSONObject().put("name", "Test User").put("verification_due_date", "15 February 2024").toString();
    private static final String XHEADER = "1";
    private static final String APP_ID = "chips";

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

    @Test
    void whenEmailRequestIsValidExpectEmailMessageIsSentSuccessfully(){
        EmailRequestDao emailRequest = mockEmailRequest();
        String emaiAddress = emailRequest.getRecipientDetails().getEmailAddress();
        String templateId = emailRequest.getEmailDetails().getTemplateId();
        String reference = emailRequest.getSenderDetails().getReference();

        when(govUKNotifyEmailFacade.sendEmail(emaiAddress, templateId, reference,
                VALID_PERSONALISATION)).thenReturn(new GovUkNotifyService.EmailResp(true, null));

        GovUkEmailDetailsRequest req = createSampleEmailRequest(emailRequest);
        ResponseEntity<Void> response = notifyIntegrationSenderController.sendEmail(req, XHEADER);

        verify(govUKNotifyEmailFacade).sendEmail(emaiAddress, templateId, reference,
                VALID_PERSONALISATION);
        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertNotNull(response);
    }

    @Test
    void whenEmailRequestNotInDbExpectNotFoundErrorResponse(){
        EmailRequestDao emailRequest = TestUtils.createEmailRequest();
        String appId = emailRequest.getSenderDetails().getAppId();
        String reference = emailRequest.getSenderDetails().getReference();
        GovUkEmailDetailsRequest req = createSampleEmailRequest(emailRequest);
        when(notificationDatabaseService.getEmail(appId, reference)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendEmail(req, XHEADER);

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        assertNotNull(response);

        verifyNoInteractions(govUKNotifyEmailFacade);
        verify(notificationDatabaseService, never()).storeResponse(any(EmailResp.class));
    }

    @Test
    void whenEmailContainsBadDateVariablesExpectFailedToPublishWelshDatesError(){
        EmailRequestDao emailRequest = mockEmailRequest();
        emailRequest.getEmailDetails().setPersonalisationDetails(new JSONObject()
                .put("name", "Test User")
                .put("verification_due_date", "15  2024")
                .toString());

        GovUkEmailDetailsRequest req = createSampleEmailRequest(emailRequest);
        ResponseEntity<Void> response = notifyIntegrationSenderController.sendEmail(req, XHEADER);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertNotNull(response);
    }

    @Test
    void whenEmailRequestIsInValidExpectInternalSeverErrorResponse(){
        EmailRequestDao emailRequest = mockEmailRequest();
        String emaiAddress = emailRequest.getRecipientDetails().getEmailAddress();
        String templateId = emailRequest.getEmailDetails().getTemplateId();
        String reference = emailRequest.getSenderDetails().getReference();
        when(govUKNotifyEmailFacade.sendEmail(emaiAddress, templateId, reference,
                VALID_PERSONALISATION)).thenReturn(new GovUkNotifyService.EmailResp(false, null));

        GovUkEmailDetailsRequest req = createSampleEmailRequest(emailRequest);
        ResponseEntity<Void> response = notifyIntegrationSenderController.sendEmail(req, XHEADER);

        assertThat(response.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertNotNull(response);
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
        EmailRequestDao emailRequest = mockEmailRequest();
        emailRequest.setEmailDetails(null);

        GovUkEmailDetailsRequest req = createSampleEmailRequest(emailRequest);
        assertThrowsExactly(NullPointerException.class, () ->
                notifyIntegrationSenderController.sendEmail(req, XHEADER)
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
        LetterRequestDao letterRequest = mockLetterRequest(letterId, templateId);
        var senderDetails = letterRequest.getSenderDetails();
        var letterDetails = letterRequest.getLetterDetails();
        var address = letterRequest.getRecipientDetails().getPhysicalAddress();
        LetterReference reference = new LetterReference(senderDetails.getAppId(), letterId,
                senderDetails.getReference());
        Mockito.when(
                letterDispatcher.sendLetter(Postage.ECONOMY, reference,
                letterDetails.getTemplateId(), address,
                letterDetails.getPersonalisationDetails(), contextId))
                .thenReturn(new GovUkNotifyService.LetterResp(true, null));

        GovUkLetterDetailsRequest req = createSampleLetterRequest(letterRequest);
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
        LetterRequestDao letterRequest = mockLetterRequest(letterId, templateId);
        var senderDetails = letterRequest.getSenderDetails();
        var letterDetails = letterRequest.getLetterDetails();
        var address = letterRequest.getRecipientDetails().getPhysicalAddress();
        LetterReference reference = new LetterReference(senderDetails.getAppId(), letterId,
                senderDetails.getReference());
        Mockito.when(letterDispatcher.sendLetter(Postage.SECOND_CLASS, reference,
                letterDetails.getTemplateId(), address,
                letterDetails.getPersonalisationDetails(), contextId))
                .thenReturn(new GovUkNotifyService.LetterResp(true, null));

        GovUkLetterDetailsRequest req = createSampleLetterRequest(letterRequest);
        ResponseEntity<Void> response = notifyIntegrationSenderController.sendLetter(req, "context5678");

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
    }

    @Test
    void sendLetter_shouldReturnNotFound_requestNotFoundInDb() {
        LetterRequestDao letterRequest = TestUtils.createLetterRequest();
        String appId = letterRequest.getSenderDetails().getAppId();
        String reference = letterRequest.getSenderDetails().getReference();
        GovUkLetterDetailsRequest req = createSampleLetterRequest(letterRequest);
        when(notificationDatabaseService.getLetter(appId, reference)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendLetter(req, "context9999");

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        verifyNoInteractions(letterDispatcher);
    }

    @Test
    void sendLetter_shouldReturnInternalServerError_onDispatcherFailure() throws Exception {
        LetterRequestDao letterRequest = mockLetterRequest("letterId", "other");
        GovUkLetterDetailsRequest req = createSampleLetterRequest(letterRequest);
        Mockito.when(letterDispatcher.sendLetter(any(), any(), any(), any(), any(), any()))
                .thenReturn(new GovUkNotifyService.LetterResp(false, new LetterResponse("{ id: bff67204-a33f-4dcf-8ec3-49fa5fce0321 }")));

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendLetter(req, "context9999");

        assertThat(response.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
    }

    @Test
    void sendLetter_shouldReturnInternalServerError_onIOException() throws Exception {
        LetterRequestDao letterRequest = mockLetterRequest("letterId", "other");
        GovUkLetterDetailsRequest req = createSampleLetterRequest(letterRequest);
        Mockito.when(letterDispatcher.sendLetter(any(), any(), any(), any(), any(), any()))
                .thenThrow(new IOException("PDF error"));

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendLetter(req, "context0000");

        assertThat(response.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
    }

    private GovUkLetterDetailsRequest createSampleLetterRequest(LetterRequestDao letterRequest) {
        String appId = letterRequest.getSenderDetails().getAppId();
        String reference = letterRequest.getSenderDetails().getReference();
        SenderDetails senderDetails = new SenderDetails(appId, reference);
        return new GovUkLetterDetailsRequest()
                .senderDetails(senderDetails)
                .createdAt(OffsetDateTime.now());
    }

    private GovUkEmailDetailsRequest createSampleEmailRequest(EmailRequestDao letterRequest) {
        String appId = letterRequest.getSenderDetails().getAppId();
        String reference = letterRequest.getSenderDetails().getReference();
        SenderDetails senderDetails = new SenderDetails(appId, reference);
        return new GovUkEmailDetailsRequest()
                .senderDetails(senderDetails)
                .createdAt(OffsetDateTime.now());
    }

    private LetterRequestDao mockLetterRequest(String letterId, String templateId) {
        String reference = VALID_REFERENCE;
        String appId = APP_ID;
        LetterRequestDao letterRequest = TestUtils.createLetterRequest();
        letterRequest.getSenderDetails().setAppId(appId);
        letterRequest.getSenderDetails().setReference(reference);
        letterRequest.getLetterDetails().setLetterId(letterId);
        letterRequest.getLetterDetails().setTemplateId(templateId);
        NotificationLetterRequest notificationRequest = new NotificationLetterRequest(null, null,
                letterRequest, null);
        when(notificationDatabaseService.getLetter(appId, reference))
                .thenReturn(Optional.of(notificationRequest));
        return letterRequest;
    }

    private EmailRequestDao mockEmailRequest() {
        String appId = APP_ID;
        String reference = VALID_REFERENCE;
        String templateId = VALID_TEMPLATE_ID;
        EmailRequestDao emailRequest = TestUtils.createEmailRequest();
        emailRequest.getSenderDetails().setAppId(appId);
        emailRequest.getSenderDetails().setReference(reference);
        emailRequest.getEmailDetails().setTemplateId(templateId);
        emailRequest.getEmailDetails().setPersonalisationDetails(REQUEST_BODY_PERSONALISATION);
        NotificationEmailRequest notificationRequest = new NotificationEmailRequest(null, null,
                emailRequest, null);
        when(notificationDatabaseService.getEmail(appId, reference))
                .thenReturn(Optional.of(notificationRequest));
        return emailRequest;
    }

}
