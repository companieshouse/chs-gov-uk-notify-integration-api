package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.LetterDetails;
import uk.gov.companieshouse.api.chs.notification.model.RecipientDetailsLetter;
import uk.gov.companieshouse.api.chs.notification.model.SenderDetails;

public class LetterRequestMapper {

    public static LetterRequestDao toDao(GovUkLetterDetailsRequest src) {
        if (src == null) {
            return null;
        }
        LetterRequestDao dest = new LetterRequestDao();
        dest.setSenderDetails(toDao(src.getSenderDetails()));
        dest.setRecipientDetails(toDao(src.getRecipientDetails()));
        dest.setLetterDetails(toDao(src.getLetterDetails()));
        dest.setCreatedAt(src.getCreatedAt());
        return dest;
    }

    public static GovUkLetterDetailsRequest fromDao(LetterRequestDao src) {
        if (src == null) {
            return null;
        }
        GovUkLetterDetailsRequest dest = new GovUkLetterDetailsRequest();
        dest.setSenderDetails(fromDao(src.getSenderDetails()));
        dest.setRecipientDetails(fromDao(src.getRecipientDetails()));
        dest.setLetterDetails(fromDao(src.getLetterDetails()));
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

    public static LetterRecipientDetailsDao toDao(RecipientDetailsLetter src) {
        if (src == null) {
            return null;
        }
        LetterRecipientDetailsDao dest = new LetterRecipientDetailsDao();
        dest.setName(src.getName());
        dest.setPhysicalAddress(toDao(src.getPhysicalAddress()));
        return dest;
    }

    public static RecipientDetailsLetter fromDao(LetterRecipientDetailsDao src) {
        if (src == null)
            return null;
        RecipientDetailsLetter dest = new RecipientDetailsLetter();
        dest.setName(src.getName());
        dest.setPhysicalAddress(fromDao(src.getPhysicalAddress()));
        return dest;
    }

    public static AddressDao toDao(Address src) {
        if (src == null) {
            return null;
        }
        AddressDao dest = new AddressDao();
        dest.setAddressLine1(src.getAddressLine1());
        dest.setAddressLine2(src.getAddressLine2());
        dest.setAddressLine3(src.getAddressLine3());
        dest.setAddressLine4(src.getAddressLine4());
        dest.setAddressLine5(src.getAddressLine5());
        dest.setAddressLine6(src.getAddressLine6());
        dest.setAddressLine7(src.getAddressLine7());
        return dest;
    }

    public static Address fromDao(AddressDao src) {
        if (src == null) {
            return null;
        }
        Address dest = new Address();
        dest.setAddressLine1(src.getAddressLine1());
        dest.setAddressLine2(src.getAddressLine2());
        dest.setAddressLine3(src.getAddressLine3());
        dest.setAddressLine4(src.getAddressLine4());
        dest.setAddressLine5(src.getAddressLine5());
        dest.setAddressLine6(src.getAddressLine6());
        dest.setAddressLine7(src.getAddressLine7());
        return dest;
    }

    public static LetterDetailsDao toDao(LetterDetails src) {
        if (src == null) {
            return null;
        }
        LetterDetailsDao dest = new LetterDetailsDao();
        dest.setTemplateId(src.getTemplateId());
        dest.setPersonalisationDetails(src.getPersonalisationDetails());
        dest.setLetterId(src.getLetterId());
        return dest;
    }

    public static LetterDetails fromDao(LetterDetailsDao src) {
        if (src == null) {
            return null;
        }
        LetterDetails dest = new LetterDetails();
        dest.setTemplateId(src.getTemplateId());
        dest.setPersonalisationDetails(src.getPersonalisationDetails());
        dest.setLetterId(src.getLetterId());
        return dest;
    }
}
