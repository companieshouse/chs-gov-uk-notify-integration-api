package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.io.IOException;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.api.chs.notification.integration.model.EmailRequest;
import uk.gov.companieshouse.api.chs.notification.integration.model.LetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.letterdispatcher.LetterDispatcher;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.letterdispatcher.LetterReference;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.RequestStatus;
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
        NotificationEmailRequest notificationRequest = mockEmailRequest();
        EmailRequestDao emailRequest = notificationRequest.getRequest();
        String emaiAddress = emailRequest.getRecipientDetails().getEmailAddress();
        String templateId = emailRequest.getEmailDetails().getTemplateId();
        String reference = emailRequest.getSenderDetails().getReference();

        when(govUKNotifyEmailFacade.sendEmail(emaiAddress, templateId, reference,
                VALID_PERSONALISATION)).thenReturn(new GovUkNotifyService.EmailResp(true, null));

        when(notificationDatabaseService.saveEmail(notificationRequest))
                .thenReturn(notificationRequest);

        EmailRequest req = createSampleEmailRequest(emailRequest);
        ResponseEntity<Void> response = notifyIntegrationSenderController.sendEmail(req, XHEADER);

        verify(govUKNotifyEmailFacade).sendEmail(emaiAddress, templateId, reference,
                VALID_PERSONALISATION);
        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertNotNull(response);

        verify(notificationDatabaseService, times(2)).saveEmail(notificationRequest);
        assertThat(notificationRequest.getStatus()).isEqualTo(RequestStatus.SENT);
    }

    @Test
    void whenEmailRequestNotInDbExpectNotFoundErrorResponse(){
        EmailRequestDao emailRequest = TestUtils.createEmailRequest();
        String appId = emailRequest.getSenderDetails().getAppId();
        String reference = emailRequest.getSenderDetails().getReference();
        EmailRequest req = createSampleEmailRequest(emailRequest);
        when(notificationDatabaseService.getEmail(appId, reference)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendEmail(req, XHEADER);

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        assertNotNull(response);

        verifyNoInteractions(govUKNotifyEmailFacade);
        verify(notificationDatabaseService, never()).storeResponse(any(EmailResp.class));
    }

    @Test
    void whenEmailContainsBadDateVariablesExpectFailedToPublishWelshDatesError(){
        NotificationEmailRequest notificationRequest = mockEmailRequest();
        EmailRequestDao emailRequest = notificationRequest.getRequest();
        emailRequest.getEmailDetails().setPersonalisationDetails(new JSONObject()
                .put("name", "Test User")
                .put("verification_due_date", "15  2024")
                .toString());

        when(notificationDatabaseService.saveEmail(notificationRequest))
                .thenReturn(notificationRequest);

        EmailRequest req = createSampleEmailRequest(emailRequest);
        ResponseEntity<Void> response = notifyIntegrationSenderController.sendEmail(req, XHEADER);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertNotNull(response);

        verify(notificationDatabaseService).saveEmail(notificationRequest);
        assertThat(notificationRequest.getStatus()).isEqualTo(RequestStatus.PROCESSING);
    }

    @Test
    void whenEmailRequestIsInValidExpectInternalSeverErrorResponse(){
        NotificationEmailRequest notificationRequest = mockEmailRequest();
        EmailRequestDao emailRequest = notificationRequest.getRequest();
        String emaiAddress = emailRequest.getRecipientDetails().getEmailAddress();
        String templateId = emailRequest.getEmailDetails().getTemplateId();
        String reference = emailRequest.getSenderDetails().getReference();
        when(govUKNotifyEmailFacade.sendEmail(emaiAddress, templateId, reference,
                VALID_PERSONALISATION)).thenReturn(new GovUkNotifyService.EmailResp(false, null));

        when(notificationDatabaseService.saveEmail(notificationRequest))
                .thenReturn(notificationRequest);

        EmailRequest req = createSampleEmailRequest(emailRequest);
        ResponseEntity<Void> response = notifyIntegrationSenderController.sendEmail(req, XHEADER);

        assertThat(response.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertNotNull(response);

        verify(notificationDatabaseService).saveEmail(notificationRequest);
        assertThat(notificationRequest.getStatus()).isEqualTo(RequestStatus.PROCESSING);
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
        NotificationLetterRequest notificationRequest = mockLetterRequest(letterId, templateId);
        var letterRequest = notificationRequest.getRequest();
        var senderDetails = letterRequest.getSenderDetails();
        var letterDetails = letterRequest.getLetterDetails();
        var address = letterRequest.getRecipientDetails().getPhysicalAddress();
        LetterReference reference = new LetterReference(senderDetails.getAppId(), letterId,
                senderDetails.getReference());
        when(
                letterDispatcher.sendLetter(Postage.ECONOMY, reference,
                letterDetails.getTemplateId(), address,
                letterDetails.getPersonalisationDetails(), contextId))
                .thenReturn(new GovUkNotifyService.LetterResp(true, null));

        when(notificationDatabaseService.saveLetter(notificationRequest))
                .thenReturn(notificationRequest);

        LetterRequest req = createSampleLetterRequest(letterRequest);
        ResponseEntity<Void> response = notifyIntegrationSenderController.sendLetter(req, contextId);

        assertThat(response.getStatusCode()).isEqualTo(CREATED);

        verify(notificationDatabaseService, times(2)).saveLetter(notificationRequest);
        assertThat(notificationRequest.getStatus()).isEqualTo(RequestStatus.SENT);
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
        NotificationLetterRequest notificationRequest = mockLetterRequest(letterId, templateId);
        var letterRequest = notificationRequest.getRequest();
        var senderDetails = letterRequest.getSenderDetails();
        var letterDetails = letterRequest.getLetterDetails();
        var address = letterRequest.getRecipientDetails().getPhysicalAddress();
        LetterReference reference = new LetterReference(senderDetails.getAppId(), letterId,
                senderDetails.getReference());
        when(letterDispatcher.sendLetter(Postage.SECOND_CLASS, reference,
                letterDetails.getTemplateId(), address,
                letterDetails.getPersonalisationDetails(), contextId))
                .thenReturn(new GovUkNotifyService.LetterResp(true, null));

        LetterRequest req = createSampleLetterRequest(letterRequest);
        ResponseEntity<Void> response = notifyIntegrationSenderController.sendLetter(req, "context5678");

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
    }

    @Test
    void sendLetter_shouldReturnNotFound_requestNotFoundInDb() {
        LetterRequestDao letterRequest = TestUtils.createLetterRequest();
        String appId = letterRequest.getSenderDetails().getAppId();
        String reference = letterRequest.getSenderDetails().getReference();
        LetterRequest req = createSampleLetterRequest(letterRequest);
        when(notificationDatabaseService.getLetter(appId, reference)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendLetter(req, "context9999");

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        verifyNoInteractions(letterDispatcher);
    }

    @Test
    void sendLetter_shouldReturnInternalServerError_onDispatcherFailure() throws Exception {
        NotificationLetterRequest letterRequest = mockLetterRequest("letterId", "other");
        LetterRequest req = createSampleLetterRequest(letterRequest.getRequest());
        when(letterDispatcher.sendLetter(any(), any(), any(), any(), any(), any()))
                .thenReturn(new GovUkNotifyService.LetterResp(false, new LetterResponse("{ id: bff67204-a33f-4dcf-8ec3-49fa5fce0321 }")));

        when(notificationDatabaseService.saveLetter(letterRequest)).thenReturn(letterRequest);

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendLetter(req, "context9999");

        assertThat(response.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
        verify(notificationDatabaseService).saveLetter(letterRequest);
        assertThat(letterRequest.getStatus()).isEqualTo(RequestStatus.PROCESSING);
    }

    @Test
    void sendLetter_shouldReturnInternalServerError_onIOException() throws Exception {
        NotificationLetterRequest letterRequest = mockLetterRequest("letterId", "other");
        LetterRequest req = createSampleLetterRequest(letterRequest.getRequest());
        when(letterDispatcher.sendLetter(any(), any(), any(), any(), any(), any()))
                .thenThrow(new IOException("PDF error"));

        when(notificationDatabaseService.saveLetter(letterRequest)).thenReturn(letterRequest);

        ResponseEntity<Void> response = notifyIntegrationSenderController.sendLetter(req, "context0000");

        assertThat(response.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
        verify(notificationDatabaseService).saveLetter(letterRequest);
        assertThat(letterRequest.getStatus()).isEqualTo(RequestStatus.PROCESSING);
    }

    private LetterRequest createSampleLetterRequest(LetterRequestDao letterRequest) {
        String appId = letterRequest.getSenderDetails().getAppId();
        String reference = letterRequest.getSenderDetails().getReference();
        return new LetterRequest(appId, reference);
    }

    private EmailRequest createSampleEmailRequest(EmailRequestDao letterRequest) {
        String appId = letterRequest.getSenderDetails().getAppId();
        String reference = letterRequest.getSenderDetails().getReference();
        return new EmailRequest(appId, reference);
    }

    private NotificationLetterRequest mockLetterRequest(String letterId, String templateId) {
        String reference = VALID_REFERENCE;
        String appId = APP_ID;
        LetterRequestDao letterRequest = TestUtils.createLetterRequest();
        letterRequest.getSenderDetails().setAppId(appId);
        letterRequest.getSenderDetails().setReference(reference);
        letterRequest.getLetterDetails().setLetterId(letterId);
        letterRequest.getLetterDetails().setTemplateId(templateId);
        NotificationLetterRequest notificationRequest = new NotificationLetterRequest(
                letterRequest);
        when(notificationDatabaseService.getLetter(appId, reference))
                .thenReturn(Optional.of(notificationRequest));
        return notificationRequest;
    }

    private NotificationEmailRequest mockEmailRequest() {
        String appId = APP_ID;
        String reference = VALID_REFERENCE;
        String templateId = VALID_TEMPLATE_ID;
        EmailRequestDao emailRequest = TestUtils.createEmailRequest();
        emailRequest.getSenderDetails().setAppId(appId);
        emailRequest.getSenderDetails().setReference(reference);
        emailRequest.getEmailDetails().setTemplateId(templateId);
        emailRequest.getEmailDetails().setPersonalisationDetails(REQUEST_BODY_PERSONALISATION);
        NotificationEmailRequest notificationRequest = new NotificationEmailRequest(
                emailRequest);
        when(notificationDatabaseService.getEmail(appId, reference))
                .thenReturn(Optional.of(notificationRequest));
        return notificationRequest;
    }

}
