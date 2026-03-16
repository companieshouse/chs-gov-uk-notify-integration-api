package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import uk.gov.companieshouse.api.chs.notification.model.EmailDetails;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.RecipientDetailsEmail;
import uk.gov.companieshouse.api.chs.notification.model.SenderDetails;

public class EmailRequestMapper {

    private EmailRequestMapper() {
        // Private constructor to prevent instantiation
    }

    public static EmailRequestDao toDao(GovUkEmailDetailsRequest src) {
        if (src == null) {
            return null;
        }
        EmailRequestDao dest = new EmailRequestDao();
        dest.setSenderDetails(toDao(src.getSenderDetails()));
        dest.setRecipientDetails(toDao(src.getRecipientDetails()));
        dest.setEmailDetails(toDao(src.getEmailDetails()));
        dest.setCreatedAt(src.getCreatedAt());
        return dest;
    }

    public static GovUkEmailDetailsRequest fromDao(EmailRequestDao src) {
        if (src == null) {
            return null;
        }
        GovUkEmailDetailsRequest dest = new GovUkEmailDetailsRequest();
        dest.setSenderDetails(fromDao(src.getSenderDetails()));
        dest.setRecipientDetails(fromDao(src.getRecipientDetails()));
        dest.setEmailDetails(fromDao(src.getEmailDetails()));
        dest.setCreatedAt(src.getCreatedAt());
        return dest;
    }

    public static SenderDetailsDao toDao(SenderDetails src) {
        if (src == null) {
            return null;
        }
        SenderDetailsDao dest = new SenderDetailsDao();
        dest.setAppId(src.getAppId());
        dest.setReference(src.getReference());
        dest.setName(src.getName());
        dest.setUserId(src.getUserId());
        dest.setEmailAddress(src.getEmailAddress());
        return dest;
    }

    public static SenderDetails fromDao(SenderDetailsDao src) {
        if (src == null) {
            return null;
        }
        SenderDetails dest = new SenderDetails();
        dest.setAppId(src.getAppId());
        dest.setReference(src.getReference());
        dest.setName(src.getName());
        dest.setUserId(src.getUserId());
        dest.setEmailAddress(src.getEmailAddress());
        return dest;
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

    public static EmailDetailsDao toDao(EmailDetails src) {
        if (src == null) {
            return null;
        }
        EmailDetailsDao dest = new EmailDetailsDao();
        dest.setTemplateId(src.getTemplateId());
        dest.setPersonalisationDetails(src.getPersonalisationDetails());
        return dest;
    }

    public static EmailDetails fromDao(EmailDetailsDao src) {
        if (src == null) {
            return null;
        }
        EmailDetails dest = new EmailDetails();
        dest.setTemplateId(src.getTemplateId());
        dest.setPersonalisationDetails(src.getPersonalisationDetails());
        return dest;
    }
}
