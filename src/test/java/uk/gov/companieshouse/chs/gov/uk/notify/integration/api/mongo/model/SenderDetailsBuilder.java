package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

public class SenderDetailsBuilder {

    private String appId = "chips";
    private String reference = String.format("use-mock-notify-%s", RandomStringUtils.randomNumeric(10));
    private String name = "Test User";
    private String userId = "test-user-id";
    private String emailAddress = "test-user@example.com";

    public static SenderDetailsBuilder senderDetailsBuilder() {
        return new SenderDetailsBuilder();
    }

    public SenderDetailsDao build() {
        SenderDetailsDao senderDetails = new SenderDetailsDao();
        senderDetails.setAppId(appId);
        senderDetails.setReference(reference);
        senderDetails.setName(name);
        senderDetails.setUserId(userId);
        senderDetails.setEmailAddress(emailAddress);
        return senderDetails;
    }

    public SenderDetailsBuilder withEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
        return this;
    }

    public SenderDetailsBuilder withAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public SenderDetailsBuilder withReference(String reference) {
        this.reference = reference;
        return this;
    }
}
