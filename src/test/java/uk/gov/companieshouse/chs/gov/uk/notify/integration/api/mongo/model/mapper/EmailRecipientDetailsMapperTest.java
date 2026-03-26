package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.chs.notification.model.RecipientDetailsEmail;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRecipientDetailsDao;

class EmailRecipientDetailsMapperTest {
    @Test
    void toDaoFromDao() {
        RecipientDetailsEmail recipient = new RecipientDetailsEmail();
        recipient.setName("Recipient Name");
        recipient.setEmailAddress("recipient@example.com");

        EmailRecipientDetailsDao dao = EmailRecipientDetailsMapper.toDao(recipient);
        assertNotNull(dao);
        assertEquals(recipient.getName(), dao.getName());
        assertEquals(recipient.getEmailAddress(), dao.getEmailAddress());

        RecipientDetailsEmail mappedBack = EmailRecipientDetailsMapper.fromDao(dao);
        assertNotNull(mappedBack);
        assertEquals(recipient, mappedBack);
    }

    @Test
    void toDaoNull() {
        RecipientDetailsEmail recipient = null;
        assertNull(EmailRecipientDetailsMapper.toDao(recipient));
    }

    @Test
    void fromDaoNull() {
        EmailRecipientDetailsDao dao = null;
        assertNull(EmailRecipientDetailsMapper.fromDao(dao));
    }
}

