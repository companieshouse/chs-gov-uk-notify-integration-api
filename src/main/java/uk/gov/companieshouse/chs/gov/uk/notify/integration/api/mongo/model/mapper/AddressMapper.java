package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.mapper;

import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.AddressDao;

public class AddressMapper {
    private AddressMapper() {}

    public static AddressDao toDao(Address src) {
        if (src == null) return null;
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
        if (src == null) return null;
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
}

