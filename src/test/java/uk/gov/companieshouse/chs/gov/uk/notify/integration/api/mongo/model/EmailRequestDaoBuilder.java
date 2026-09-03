package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

/**
 * Builder class for creating instances of EmailRequestDao for testing purposes.
 */
public class EmailRequestDaoBuilder {

    private String appId = "chips";
    private String reference = RandomStringUtils.randomNumeric(10);
    private Map<String, Object> personalisationDetails;

    public static EmailRequestDaoBuilder emailRequestDaoBuilder() {
        return new EmailRequestDaoBuilder();
    }

    public EmailRequestDao build() {
        SenderDetailsDao senderDetails = new SenderDetailsDao();
        senderDetails.setAppId(appId);
        senderDetails.setReference(reference);
        EmailRecipientDetailsDao recipientDetails = new EmailRecipientDetailsDao();
        recipientDetails.setName("Test User");
        recipientDetails.setEmailAddress("test@example");
        EmailDetailsDao emailDetails = new EmailDetailsDao();
        emailDetails.setTemplateId(UUID.randomUUID().toString());
        emailDetails.setPersonalisationDetails(personalisationDetails);

        EmailRequestDao emailRequest = new EmailRequestDao();
        emailRequest.setSenderDetails(senderDetails);
        emailRequest.setRecipientDetails(recipientDetails);
        emailRequest.setEmailDetails(emailDetails);
        emailRequest.setCreatedAt(OffsetDateTime.now());
        return emailRequest;
    }

    public EmailRequestDaoBuilder withPersonalisationDetails(Map<String, Object> personalisationDetails) {
        this.personalisationDetails = personalisationDetails;
        return this;
    }

    public EmailRequestDaoBuilder withRandomMockNotifyReference() {
        this.reference = String.format("use-mock-notify-%s", RandomStringUtils.randomNumeric(10));
        return this;
    }
}
