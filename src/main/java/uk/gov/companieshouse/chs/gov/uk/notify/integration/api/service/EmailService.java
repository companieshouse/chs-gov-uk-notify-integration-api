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
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailDetailsDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRecipientDetailsDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.RequestStatus;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.SenderDetailsDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation.WelshDatesPublisher;
import uk.gov.companieshouse.logging.Logger;

@Service
public class EmailService {

    private final NotificationDatabaseService notificationDatabaseService;
    private final GovUkNotifyService govUkNotifyService;
    private final AttachmentStorageService attachmentStorageService;
    private final Logger logger;

    public EmailService(NotificationDatabaseService notificationDatabaseService,
                        GovUkNotifyService govUkNotifyService,
                        AttachmentStorageService attachmentStorageService,
                        Logger logger) {
        this.notificationDatabaseService = notificationDatabaseService;
        this.govUkNotifyService = govUkNotifyService;
        this.attachmentStorageService = attachmentStorageService;
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

        EmailRequestDao emailRequestDao = emailRequest.getRequest();
        EmailRecipientDetailsDao recipientDetails = emailRequestDao.getRecipientDetails();
        EmailDetailsDao emailDetails = emailRequestDao.getEmailDetails();
        SenderDetailsDao senderDetails = emailRequestDao.getSenderDetails();

        logger.infoContext(xHeaderId, "Sending email to " + recipientDetails.getEmailAddress(),
                createLogMap(xHeaderId, "send_email"));

        GovUkNotifyService.EmailResp emailResp;
        if (emailDetails.hasAttachment()) {
            AttachmentFile attachment = attachmentStorageService.getAttachment(
                    xHeaderId,
                    emailDetails.getAttachmentId());
            emailResp = govUkNotifyService.sendEmailWithAttachment(new GovNotificationEmailRequest(
                    recipientDetails.getEmailAddress(),
                    emailDetails.getTemplateId(),
                    senderDetails.getReference(),
                    emailDetails.getPersonalisationDetails(),
                    attachment));
        } else {
            emailResp = govUkNotifyService.sendEmail(
                    recipientDetails.getEmailAddress(),
                    emailDetails.getTemplateId(),
                    senderDetails.getReference(),
                    emailDetails.getPersonalisationDetails());
        }
        logger.debugContext(xHeaderId, "Storing email response in database", createLogMap(xHeaderId, "store_response"));
        notificationDatabaseService.storeResponse(emailResp);

        if (emailResp.success()) {
            emailRequest.setStatus(RequestStatus.SENT);
            notificationDatabaseService.saveEmail(emailRequest);
            logger.infoContext(xHeaderId, "Email sent successfully", createLogMap(xHeaderId, "email_success"));
        } else {
            throw new EmailClientException(format("Failed to send email for request: %s %s", xHeaderId, emailRequestDao));
        }

    }
}
