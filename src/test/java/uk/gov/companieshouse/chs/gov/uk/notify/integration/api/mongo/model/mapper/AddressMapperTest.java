package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.AddressDao;

class AddressMapperTest {
    @Test
    void toDaoFromDao() {
        Address address = new Address();
        address.setAddressLine1("Line1");
        address.setAddressLine2("Line2");
        address.setAddressLine3("Line3");
        address.setAddressLine4("Line4");
        address.setAddressLine5("Line5");
        address.setAddressLine6("Line6");
        address.setAddressLine7("Line7");

        AddressDao dao = AddressMapper.toDao(address);
        assertNotNull(dao);
        assertEquals(address.getAddressLine1(), dao.getAddressLine1());
        assertEquals(address.getAddressLine2(), dao.getAddressLine2());
        assertEquals(address.getAddressLine3(), dao.getAddressLine3());
        assertEquals(address.getAddressLine4(), dao.getAddressLine4());
        assertEquals(address.getAddressLine5(), dao.getAddressLine5());
        assertEquals(address.getAddressLine6(), dao.getAddressLine6());
        assertEquals(address.getAddressLine7(), dao.getAddressLine7());

        Address mappedBack = AddressMapper.fromDao(dao);
        assertNotNull(mappedBack);
        assertEquals(address, mappedBack);
    }

    @Test
    void toDaoNull() {
        Address address = null;
        assertNull(AddressMapper.toDao(address));
    }

    @Test
    void fromDaoNull() {
        AddressDao dao = null;
        assertNull(AddressMapper.fromDao(dao));
    }
}

