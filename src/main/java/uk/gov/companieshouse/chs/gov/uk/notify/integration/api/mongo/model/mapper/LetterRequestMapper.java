package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.mapper;

import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterRequestDao;

public class LetterRequestMapper {
    private LetterRequestMapper() {
        // Private constructor to prevent instantiation
    }

    public static LetterRequestDao toDao(GovUkLetterDetailsRequest src) {
        if (src == null) {
            return null;
        }
        LetterRequestDao dest = new LetterRequestDao();
        dest.setSenderDetails(SenderDetailsMapper.toDao(src.getSenderDetails()));
        dest.setRecipientDetails(LetterRecipientDetailsMapper.toDao(src.getRecipientDetails()));
        dest.setLetterDetails(LetterDetailsMapper.toDao(src.getLetterDetails()));
        dest.setCreatedAt(src.getCreatedAt());
        return dest;
    }

    public static GovUkLetterDetailsRequest fromDao(LetterRequestDao src) {
        if (src == null) {
            return null;
        }
        GovUkLetterDetailsRequest dest = new GovUkLetterDetailsRequest();
        dest.setSenderDetails(SenderDetailsMapper.fromDao(src.getSenderDetails()));
        dest.setRecipientDetails(LetterRecipientDetailsMapper.fromDao(src.getRecipientDetails()));
        dest.setLetterDetails(LetterDetailsMapper.fromDao(src.getLetterDetails()));
        dest.setCreatedAt(src.getCreatedAt());
        return dest;
    }
}
