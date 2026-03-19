package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.chs.notification.model.SenderDetails;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.SenderDetailsDao;

class SenderDetailsMapperTest {
    @Test
    void toDaoFromDao() {
        SenderDetails sender = new SenderDetails();
        sender.setAppId("app-id");
        sender.setReference("ref");
        sender.setName("Sender Name");
        sender.setUserId("user-id");
        sender.setEmailAddress("sender@example.com");

        SenderDetailsDao dao = SenderDetailsMapper.toDao(sender);
        assertNotNull(dao);
        assertEquals(sender.getAppId(), dao.getAppId());
        assertEquals(sender.getReference(), dao.getReference());
        assertEquals(sender.getName(), dao.getName());
        assertEquals(sender.getUserId(), dao.getUserId());
        assertEquals(sender.getEmailAddress(), dao.getEmailAddress());

        SenderDetails mappedBack = SenderDetailsMapper.fromDao(dao);
        assertNotNull(mappedBack);
        assertEquals(sender, mappedBack);
    }

    @Test
    void toDaoNull() {
        SenderDetails sender = null;
        assertNull(SenderDetailsMapper.toDao(sender));
    }

    @Test
    void fromDaoNull() {
        SenderDetailsDao dao = null;
        assertNull(SenderDetailsMapper.fromDao(dao));
    }
}

