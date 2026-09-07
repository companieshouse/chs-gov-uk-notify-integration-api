package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import static java.lang.String.format;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.createLogMap;

import com.google.common.base.Preconditions;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.chs.notification.integration.model.EmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.AlreadyProcessedException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.EmailClientException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.EmailNotFoundException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.EmailValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.RequestStatus;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation.WelshDatesPublisher;
import uk.gov.companieshouse.logging.Logger;

@Service
public class EmailService {

    private final NotificationDatabaseService notificationDatabaseService;
    private final GovUkNotifyService govUkNotifyService;
    private final Logger logger;

    public EmailService(NotificationDatabaseService notificationDatabaseService,
                        GovUkNotifyService govUkNotifyService,
                        Logger logger) {
        this.notificationDatabaseService = notificationDatabaseService;
        this.govUkNotifyService = govUkNotifyService;
        this.logger = logger;
    }

    public NotificationEmailRequest validateEmailRequest(@Nullable String contextId,
                                                         @NonNull EmailRequest emailRequest) {
        Preconditions.checkNotNull(emailRequest, "emailRequest is marked non-null but is null");
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

    public void sendEmail(String xHeaderId, NotificationEmailRequest emailRequest) {
        emailRequest.setStatus(RequestStatus.PROCESSING);
        emailRequest = notificationDatabaseService.saveEmail(emailRequest);

        logger.infoContext(xHeaderId, "Sending email to " + emailRequest.getRequest().getRecipientDetails().getEmailAddress(),
                createLogMap(xHeaderId, "send_email"));

        var emailResp = govUkNotifyService.sendEmail(
                emailRequest.getRequest().getRecipientDetails().getEmailAddress(),
                emailRequest.getRequest().getEmailDetails().getTemplateId(),
                emailRequest.getRequest().getSenderDetails().getReference(),
                emailRequest.getRequest().getEmailDetails().getPersonalisationDetails());

        logger.debugContext(xHeaderId, "Storing email response in database", createLogMap(xHeaderId, "store_response"));
        notificationDatabaseService.storeResponse(emailResp);

        if (emailResp.success()) {
            emailRequest.setStatus(RequestStatus.SENT);
            notificationDatabaseService.saveEmail(emailRequest);
            logger.infoContext(xHeaderId, "Email sent successfully", createLogMap(xHeaderId, "email_success"));
        } else {
            throw new EmailClientException(format("Failed to send email for request: %s %s", xHeaderId, emailRequest.getRequest()));
        }

    }
}
