package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailDetailsBuilder.emailDetailsBuilder;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDaoBuilder.emailRequestDaoBuilder;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationEmailRequestBuilder.notificationEmailRequestBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.chs.notification.integration.model.EmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.AlreadyProcessedException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.EmailClientException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.EmailNotFoundException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.EmailValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.RequestStatus;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private NotificationDatabaseService notificationDatabaseService;

    @Mock
    private GovUkNotifyService govUkNotifyService;

    @Mock
    private Logger logger;

    @InjectMocks
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<NotificationEmailRequest> emailRequestArgumentCaptor;

    @Test
    void shouldThrowEmailNotFoundExceptionWhenValidatingEmail() {
        // Given
        String contextId = UUID.randomUUID().toString();
        EmailRequest emailRequest = new EmailRequest("chips", UUID.randomUUID().toString());

        given(notificationDatabaseService.getEmail(emailRequest.getAppId(), emailRequest.getReference()))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> emailService.validateEmailRequest(contextId, emailRequest))
                .isInstanceOf(EmailNotFoundException.class)
                .hasMessage("""
                        Email request not found in database for request: %s class EmailRequest {
                            appId: chips
                            reference: %s
                        }""", contextId, emailRequest.getReference());
    }

    @Test
    void shouldThrowAlreadyProcessedExceptionWhenValidatingEmail() {
        // Given
        String contextId = UUID.randomUUID().toString();
        EmailRequest emailRequest = new EmailRequest("chips", UUID.randomUUID().toString());

        NotificationEmailRequest notificationEmailRequest = new NotificationEmailRequest();
        notificationEmailRequest.setStatus(RequestStatus.SENT);

        given(notificationDatabaseService.getEmail(emailRequest.getAppId(), emailRequest.getReference()))
                .willReturn(Optional.of(notificationEmailRequest));

        // When & Then
        assertThatThrownBy(() -> emailService.validateEmailRequest(contextId, emailRequest))
                .isInstanceOf(AlreadyProcessedException.class)
                .hasMessage("""
                        Email request already sent: %s class EmailRequest {
                            appId: chips
                            reference: %s
                        }""", contextId, emailRequest.getReference());
    }

    @Test
    void shouldThrowValidationWhenValidatingEmailFailsDueToWelshDateFailure() {
        // Given
        String contextId = UUID.randomUUID().toString();
        EmailRequest emailRequest = new EmailRequest("chips", UUID.randomUUID().toString());

        NotificationEmailRequest notificationEmailRequest = new NotificationEmailRequest();
        notificationEmailRequest.setStatus(RequestStatus.PENDING);
        notificationEmailRequest.setRequest(emailRequestDaoBuilder()
                        .withEmailDetails(emailDetailsBuilder()
                                .withPersonalisationDetails(Map.of(
                                "name", "Test User",
                                "verification_due_date", "15  2024" // Invalid date format to trigger WelshDatesPublisher failure
                        )).build())
                .build());

        given(notificationDatabaseService.getEmail(emailRequest.getAppId(), emailRequest.getReference()))
                .willReturn(Optional.of(notificationEmailRequest));

        // When & Then
        assertThatThrownBy(() -> emailService.validateEmailRequest(contextId, emailRequest))
                .isInstanceOf(EmailValidationException.class)
                .hasMessage("Request: %s Failed to publish Welsh dates: Unknown month '' in date '15  2024' for verification_due_date, action: welsh_dates_error", contextId);
    }

    @Test
    void shouldSubstituteWelshDatesWhenValidationIsSuccessful() {
        // Given
        String contextId = UUID.randomUUID().toString();
        EmailRequest emailRequest = new EmailRequest("chips", UUID.randomUUID().toString());

        NotificationEmailRequest notificationEmailRequest = new NotificationEmailRequest();
        notificationEmailRequest.setStatus(RequestStatus.PENDING);
        notificationEmailRequest.setRequest(emailRequestDaoBuilder()
                .withEmailDetails(emailDetailsBuilder()
                        .withPersonalisationDetails(Map.of(
                        "name", "Test User",
                        "verification_due_date", "15 February 2024"
                        )).build())
                .build());

        given(notificationDatabaseService.getEmail(emailRequest.getAppId(), emailRequest.getReference()))
                .willReturn(Optional.of(notificationEmailRequest));

        // When
        NotificationEmailRequest validatedEmailRequest = emailService.validateEmailRequest(contextId, emailRequest);

        // Then
        assertThat(validatedEmailRequest)
                .isNotNull()
                .satisfies(request -> assertThat(request.getRequest().getEmailDetails().getPersonalisationDetails())
                        .containsEntry("welsh_verification_due_date", "15 Chwefror 2024"));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenEmailRequestIsNull() {
        // Given
        String contextId = UUID.randomUUID().toString();

        // When & Then
        assertThatThrownBy(() -> emailService.validateEmailRequest(contextId, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("emailRequest is marked non-null but is null");
    }

    @Test
    void shouldSendEmailToGovNotify() {
        // Given
        EmailRequestDao emailRequest = emailRequestDaoBuilder()
                .withEmailDetails(emailDetailsBuilder()
                        .withPersonalisationDetails(Map.of(
                                "name", "Test User",
                                "verification_due_date", "15 February 2024"
                        )).build())
                .build();

        NotificationEmailRequest pendingNotificationEmailRequest = notificationEmailRequestBuilder()
                .withStatus(RequestStatus.PENDING)
                .withRequest(emailRequest)
                .build();

        NotificationEmailRequest processingNotificationEmailRequest = notificationEmailRequestBuilder()
                .withRequest(emailRequest)
                .withStatus(RequestStatus.PROCESSING)
                .build();

        given(notificationDatabaseService.saveEmail(pendingNotificationEmailRequest)).willReturn(processingNotificationEmailRequest);

        GovUkNotifyService.EmailResp successfulResponse = new GovUkNotifyService.EmailResp(true, null);
        given(govUkNotifyService.sendEmail(
                emailRequest.getRecipientDetails().getEmailAddress(),
                emailRequest.getEmailDetails().getTemplateId(),
                emailRequest.getSenderDetails().getReference(),
                emailRequest.getEmailDetails().getPersonalisationDetails())).willReturn(successfulResponse);

        // When
        emailService.sendEmail(UUID.randomUUID().toString(), pendingNotificationEmailRequest);

        // Then
        then(notificationDatabaseService).should(times(2)).saveEmail(emailRequestArgumentCaptor.capture());
        assertThat(emailRequestArgumentCaptor.getAllValues())
                .hasSize(2)
                .satisfiesExactly(
                        firstCapture -> assertThat(firstCapture.getStatus()).isEqualTo(RequestStatus.PROCESSING),
                        secondCapture -> assertThat(secondCapture.getStatus()).isEqualTo(RequestStatus.SENT));
        then(notificationDatabaseService).should().storeResponse(successfulResponse);
    }

    @Test
    void shouldThrowEmailClientExceptionGivenEmailResponseFailed() {
        // Given
        String contextId = UUID.randomUUID().toString();
        EmailRequestDao emailRequest = emailRequestDaoBuilder()
                .withEmailDetails(emailDetailsBuilder()
                        .withPersonalisationDetails(Map.of(
                                "name", "Test User"
                        )).build())
                .build();

        NotificationEmailRequest notificationEmailRequest = notificationEmailRequestBuilder()
                .withStatus(RequestStatus.PENDING)
                .withRequest(emailRequest)
                .build();

        GovUkNotifyService.EmailResp failedResponse = new GovUkNotifyService.EmailResp(false, null);
        given(notificationDatabaseService.saveEmail(notificationEmailRequest)).willReturn(notificationEmailRequest);
        given(govUkNotifyService.sendEmail(
                emailRequest.getRecipientDetails().getEmailAddress(),
                emailRequest.getEmailDetails().getTemplateId(),
                emailRequest.getSenderDetails().getReference(),
                emailRequest.getEmailDetails().getPersonalisationDetails())).willReturn(failedResponse);

        // When & Then
        assertThatThrownBy(() -> emailService.sendEmail(contextId, notificationEmailRequest))
                .isInstanceOf(EmailClientException.class)
                .hasMessage("Failed to send email for request: %s %s", contextId, notificationEmailRequest.getRequest());
        then(notificationDatabaseService).should().storeResponse(failedResponse);
    }
}
