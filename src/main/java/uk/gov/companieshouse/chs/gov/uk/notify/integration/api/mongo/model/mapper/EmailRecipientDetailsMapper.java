package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.mapper;

import uk.gov.companieshouse.api.chs.notification.model.RecipientDetailsEmail;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRecipientDetailsDao;

public class EmailRecipientDetailsMapper {
    private EmailRecipientDetailsMapper() {
        // Private constructor to prevent instantiation
    }

    public static EmailRecipientDetailsDao toDao(RecipientDetailsEmail src) {
        if (src == null) {
            return null;
        }
        EmailRecipientDetailsDao dest = new EmailRecipientDetailsDao();
        dest.setName(src.getName());
        dest.setEmailAddress(src.getEmailAddress());
        return dest;
    }

    public static RecipientDetailsEmail fromDao(EmailRecipientDetailsDao src) {
        if (src == null) {
            return null;
        }
        RecipientDetailsEmail dest = new RecipientDetailsEmail();
        dest.setName(src.getName());
        dest.setEmailAddress(src.getEmailAddress());
        return dest;
    }
}
