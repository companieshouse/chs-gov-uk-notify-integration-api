package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.api.chs.notification.model.RecipientDetailsLetter;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterRecipientDetailsDao;

class LetterRecipientDetailsMapperTest {
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

        RecipientDetailsLetter recipient = new RecipientDetailsLetter();
        recipient.setName("Recipient Name");
        recipient.setPhysicalAddress(address);

        LetterRecipientDetailsDao dao = LetterRecipientDetailsMapper.toDao(recipient);
        assertNotNull(dao);
        assertEquals(recipient.getName(), dao.getName());
        assertNotNull(dao.getPhysicalAddress());
        assertEquals(address, AddressMapper.fromDao(dao.getPhysicalAddress()));

        RecipientDetailsLetter mappedBack = LetterRecipientDetailsMapper.fromDao(dao);
        assertNotNull(mappedBack);
        assertEquals(recipient, mappedBack);
    }

    @Test
    void toDaoNull() {
        RecipientDetailsLetter recipient = null;
        assertNull(LetterRecipientDetailsMapper.toDao(recipient));
    }

    @Test
    void fromDaoNull() {
        LetterRecipientDetailsDao dao = null;
        assertNull(LetterRecipientDetailsMapper.fromDao(dao));
    }
}

