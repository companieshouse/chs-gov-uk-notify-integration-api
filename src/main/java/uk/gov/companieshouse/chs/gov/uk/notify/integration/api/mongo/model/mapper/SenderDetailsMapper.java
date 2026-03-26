package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.mapper;

import uk.gov.companieshouse.api.chs.notification.model.SenderDetails;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.SenderDetailsDao;

public class SenderDetailsMapper {
    private SenderDetailsMapper() {
        // Private constructor to prevent instantiation
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
}
