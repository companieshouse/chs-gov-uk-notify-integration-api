package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import java.time.OffsetDateTime;
import java.util.HashMap;
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
    private String templateId = UUID.randomUUID().toString();

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
        emailDetails.setTemplateId(templateId);
        emailDetails.setPersonalisationDetails(personalisationDetails);

        EmailRequestDao emailRequest = new EmailRequestDao();
        emailRequest.setSenderDetails(senderDetails);
        emailRequest.setRecipientDetails(recipientDetails);
        emailRequest.setEmailDetails(emailDetails);
        emailRequest.setCreatedAt(OffsetDateTime.now());
        return emailRequest;
    }

    public EmailRequestDaoBuilder withAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public EmailRequestDaoBuilder withReference(String reference) {
        this.reference = reference;
        return this;
    }

    public EmailRequestDaoBuilder withPersonalisationDetails(Map<String, Object> personalisationDetails) {
        this.personalisationDetails = new HashMap<>(personalisationDetails);
        return this;
    }

    public EmailRequestDaoBuilder withRandomMockNotifyReference() {
        this.reference = String.format("use-mock-notify-%s", RandomStringUtils.randomNumeric(10));
        return this;
    }

    public EmailRequestDaoBuilder withTemplateId(String templateId) {
        this.templateId = templateId;
        return this;
    }
}
