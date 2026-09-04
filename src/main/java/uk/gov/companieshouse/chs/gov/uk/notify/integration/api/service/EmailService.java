package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import static java.lang.String.format;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.chs.notification.integration.model.EmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.AlreadyProcessedException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.EmailValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.EmailNotFoundException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.RequestStatus;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation.WelshDatesPublisher;

@Service
public class EmailService {

    private final NotificationDatabaseService notificationDatabaseService;

    public EmailService(NotificationDatabaseService notificationDatabaseService) {
        this.notificationDatabaseService = notificationDatabaseService;
    }

    public NotificationEmailRequest validateEmailRequest(@Nullable String contextId,
                                                         @NonNull EmailRequest emailRequest) {
        NotificationEmailRequest notificationEmailRequest = notificationDatabaseService.getEmail(emailRequest.getAppId(), emailRequest.getReference())
                .orElseThrow(() -> new EmailNotFoundException(format("Email request not found in database for request: %s %s", contextId, emailRequest)));
        if (RequestStatus.SENT.equals(notificationEmailRequest.getStatus())) {
            throw new AlreadyProcessedException(format("Email request already sent: %s %s", contextId, emailRequest));
        }

        try {
            WelshDatesPublisher.publishWelshDates(notificationEmailRequest.getRequest().getEmailDetails().getPersonalisationDetails());
        } catch (Exception e) {
            throw new EmailValidationException(format("Request: %s Failed to publish Welsh dates: %s, action: %s", contextId, e.getMessage(), "welsh_dates_error"));
        }
        return notificationEmailRequest;
    }

}
