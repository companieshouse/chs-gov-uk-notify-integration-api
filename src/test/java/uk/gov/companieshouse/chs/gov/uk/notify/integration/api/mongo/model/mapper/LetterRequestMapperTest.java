package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.LetterDetails;
import uk.gov.companieshouse.api.chs.notification.model.RecipientDetailsLetter;
import uk.gov.companieshouse.api.chs.notification.model.SenderDetails;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.AddressDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterRequestDao;

class LetterRequestMapperTest {
    @Test
    void toDaoFromDao() {
        // Setup
        Address address = new Address();
        address.setAddressLine1("Line1");
        address.setAddressLine2("Line2");
        address.setAddressLine3("Line3");
        address.setAddressLine4("Line4");
        address.setAddressLine5("Line5");
        address.setAddressLine6("Line6");
        address.setAddressLine7("Line7");

        SenderDetails sender = new SenderDetails();
        sender.setAppId("app-id");
        sender.setReference("ref");
        sender.setName("Sender Name");
        sender.setUserId("user-id");
        sender.setEmailAddress("sender@example.com");

        RecipientDetailsLetter recipient = new RecipientDetailsLetter();
        recipient.setName("Recipient Name");
        recipient.setPhysicalAddress(address);

        LetterDetails letterDetails = new LetterDetails();
        letterDetails.setLetterId("letterId");
        letterDetails.setTemplateId("template");
        letterDetails.setPersonalisationDetails("{\"key\":\"value\"}");

        GovUkLetterDetailsRequest request = new GovUkLetterDetailsRequest();
        request.setSenderDetails(sender);
        request.setRecipientDetails(recipient);
        request.setLetterDetails(letterDetails);
        request.setCreatedAt(OffsetDateTime.now());

        // toDao
        LetterRequestDao dao = LetterRequestMapper.toDao(request);
        assertNotNull(dao);
        assertEquals(request.getCreatedAt(), dao.getCreatedAt());
        assertNotNull(dao.getSenderDetails());
        assertEquals(sender.getAppId(), dao.getSenderDetails().getAppId());
        assertEquals(sender.getReference(), dao.getSenderDetails().getReference());
        assertEquals(sender.getName(), dao.getSenderDetails().getName());
        assertEquals(sender.getUserId(), dao.getSenderDetails().getUserId());
        assertEquals(sender.getEmailAddress(), dao.getSenderDetails().getEmailAddress());

        assertNotNull(dao.getRecipientDetails());
        assertEquals(recipient.getName(), dao.getRecipientDetails().getName());
        assertNotNull(dao.getRecipientDetails().getPhysicalAddress());
        AddressDao addressDao = dao.getRecipientDetails().getPhysicalAddress();
        assertEquals(address.getAddressLine1(), addressDao.getAddressLine1());
        assertEquals(address.getAddressLine2(), addressDao.getAddressLine2());
        assertEquals(address.getAddressLine3(), addressDao.getAddressLine3());
        assertEquals(address.getAddressLine4(), addressDao.getAddressLine4());
        assertEquals(address.getAddressLine5(), addressDao.getAddressLine5());
        assertEquals(address.getAddressLine6(), addressDao.getAddressLine6());
        assertEquals(address.getAddressLine7(), addressDao.getAddressLine7());

        assertNotNull(dao.getLetterDetails());
        assertEquals(letterDetails.getLetterId(), dao.getLetterDetails().getLetterId());
        assertEquals(letterDetails.getTemplateId(), dao.getLetterDetails().getTemplateId());
        assertEquals(letterDetails.getPersonalisationDetails(), dao.getLetterDetails().getPersonalisationDetails());

        // fromDao
        GovUkLetterDetailsRequest mappedBack = LetterRequestMapper.fromDao(dao);
        assertNotNull(mappedBack);
        assertEquals(request, mappedBack);
    }

    @Test
    void toDaoNullGovUkLetterDetailsRequest() {
        GovUkLetterDetailsRequest request = null;
        assertNull(LetterRequestMapper.toDao(request));
    }

    @Test
    void fromDaoNullLetterRequestDao() {
        LetterRequestDao dao = null;
        assertNull(LetterRequestMapper.fromDao(dao));
    }

}