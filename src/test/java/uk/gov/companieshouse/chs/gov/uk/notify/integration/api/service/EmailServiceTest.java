package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDaoBuilder.emailRequestDaoBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.chs.notification.integration.model.EmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.AlreadyProcessedException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.EmailNotFoundException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.EmailValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.RequestStatus;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private NotificationDatabaseService notificationDatabaseService;

    @InjectMocks
    private EmailService emailService;

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
                        .withPersonalisationDetails(Map.of(
                                "name", "Test User",
                                "verification_due_date", "15  2024" // Invalid date format to trigger WelshDatesPublisher failure
                        ))
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
                .withPersonalisationDetails(Map.of(
                        "name", "Test User",
                        "verification_due_date", "15 February 2024"
                ))
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

}
