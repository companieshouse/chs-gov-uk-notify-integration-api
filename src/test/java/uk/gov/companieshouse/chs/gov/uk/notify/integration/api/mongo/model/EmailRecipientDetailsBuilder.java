package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

public class EmailRecipientDetailsBuilder {

    public static EmailRecipientDetailsBuilder emailRecipientDetailsBuilder() {
        return new EmailRecipientDetailsBuilder();
    }

    public EmailRecipientDetailsDao build() {
        EmailRecipientDetailsDao emailRecipientDetailsDao = new EmailRecipientDetailsDao();
        emailRecipientDetailsDao.setName("Test User");
        emailRecipientDetailsDao.setEmailAddress("test@example.com");
        return emailRecipientDetailsDao;
    }

}
