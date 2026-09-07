package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailDetailsBuilder.emailDetailsBuilder;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRecipientDetailsBuilder.emailRecipientDetailsBuilder;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.SenderDetailsBuilder.senderDetailsBuilder;

import java.time.OffsetDateTime;

/**
 * Builder class for creating instances of EmailRequestDao for testing purposes.
 */
public class EmailRequestDaoBuilder {

    private SenderDetailsDao senderDetails = senderDetailsBuilder().build();
    private EmailRecipientDetailsDao recipientDetails = emailRecipientDetailsBuilder().build();
    private EmailDetailsDao emailDetails = emailDetailsBuilder().build();

    public static EmailRequestDaoBuilder emailRequestDaoBuilder() {
        return new EmailRequestDaoBuilder();
    }

    public EmailRequestDao build() {
        EmailRequestDao emailRequest = new EmailRequestDao();
        emailRequest.setSenderDetails(senderDetails);
        emailRequest.setRecipientDetails(recipientDetails);
        emailRequest.setEmailDetails(emailDetails);
        emailRequest.setCreatedAt(OffsetDateTime.now());
        return emailRequest;
    }

    public EmailRequestDaoBuilder withSenderDetails(SenderDetailsDao senderDetailsDao) {
        this.senderDetails = senderDetailsDao;
        return this;
    }

    public EmailRequestDaoBuilder withEmailDetails(EmailDetailsDao emailDetailsDao) {
        this.emailDetails = emailDetailsDao;
        return this;
    }
}
