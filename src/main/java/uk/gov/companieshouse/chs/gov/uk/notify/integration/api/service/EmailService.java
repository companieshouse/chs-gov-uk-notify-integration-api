package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.MessageType;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.email.*;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.StaticPropertyUtil;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.Objects;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.MessageType.*;

@Service
public class EmailService {

    @Value("${signin.url}")
    private String signinUrl;

    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    private final EmailProducer emailProducer;

    @Autowired
    public EmailService(final EmailProducer emailProducer) {
        this.emailProducer = emailProducer;
    }

    @Async
    public void sendConfirmYouAreAMemberEmail(final String xRequestId, final String recipientEmail, final String addedBy, final String acspName, final UserRoleEnum role) {
        if (Objects.isNull(recipientEmail) || Objects.isNull(addedBy) || Objects.isNull(acspName) || Objects.isNull(role)) {
            LOG.errorContext(xRequestId, new Exception("Attempted to send confirm-you-are-a-member email, with null recipientEmail, null addedBy, null acspName, or null role."), null);
            throw new IllegalArgumentException("recipientEmail, addedBy, acspName, and role must not be null.");
        }

        final MessageType messageType;
        final var emailData =
                switch (role) {
                    case UserRoleEnum.OWNER -> {
                        messageType = CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE;
                        yield new ConfirmYouAreAnOwnerMemberEmailData(recipientEmail, addedBy, acspName, signinUrl);
                    }
                    case UserRoleEnum.ADMIN -> {
                        messageType = CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE;
                        yield new ConfirmYouAreAnAdminMemberEmailData(recipientEmail, addedBy, acspName, signinUrl);
                    }
                    case UserRoleEnum.STANDARD -> {
                        messageType = CONFIRM_YOU_ARE_A_STANDARD_MEMBER_MESSAGE_TYPE;
                        yield new ConfirmYouAreAStandardMemberEmailData(recipientEmail, addedBy, acspName, signinUrl);
                    }
                    default -> {
                        LOG.errorContext(xRequestId, new Exception(String.format("Role is invalid: %s", role.getValue())), null);
                        throw new IllegalArgumentException("Role is invalid");
                    }
                };

        try {
            emailProducer.sendEmail(emailData, messageType.getValue());
            LOG.infoContext(xRequestId, emailData.toNotificationSentLoggingMessage(), null);
        } catch (Exception exception) {
            LOG.errorContext(xRequestId, new Exception(emailData.toNotificationSendingFailureLoggingMessage()), null);
            throw exception;
        }

    }

    @Async
    public void sendYourRoleAtAcspHasChangedEmail(final String xRequestId, final String recipientEmail, final String editedBy, final String acspName, final UserRoleEnum newRole) {
        if (Objects.isNull(recipientEmail) || Objects.isNull(editedBy) || Objects.isNull(acspName) || Objects.isNull(newRole)) {
            LOG.errorContext(xRequestId, new Exception("Attempted to send your-role-at-acsp-has-changed email, with null recipientEmail, null editedBy, null acspName, or null newRole."), null);
            throw new IllegalArgumentException("recipientEmail, editedBy, acspName, and newRole must not be null.");
        }

        final MessageType messageType;
        final var emailData =
                switch (newRole) {
                    case UserRoleEnum.OWNER -> {
                        messageType = YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE;
                        yield new YourRoleAtAcspHasChangedToOwnerEmailData(recipientEmail, editedBy, acspName, signinUrl);
                    }
                    case UserRoleEnum.ADMIN -> {
                        messageType = YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE;
                        yield new YourRoleAtAcspHasChangedToAdminEmailData(recipientEmail, editedBy, acspName, signinUrl);
                    }
                    case UserRoleEnum.STANDARD -> {
                        messageType = YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_STANDARD_MESSAGE_TYPE;
                        yield new YourRoleAtAcspHasChangedToStandardEmailData(recipientEmail, editedBy, acspName, signinUrl);
                    }
                    default -> {
                        LOG.errorContext(xRequestId, new Exception(String.format("Role is invalid: %s", newRole.getValue())), null);
                        throw new IllegalArgumentException("Role is invalid");
                    }
                };

        try {
            emailProducer.sendEmail(emailData, messageType.getValue());
            LOG.infoContext(xRequestId, emailData.toNotificationSentLoggingMessage(), null);
        } catch (Exception exception) {
            LOG.errorContext(xRequestId, new Exception(emailData.toNotificationSendingFailureLoggingMessage()), null);
            throw exception;
        }

    }

}
