package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.chs.notification.model.EmailDetails;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailDetailsDao;

class EmailDetailsMapperTest {
    @Test
    void toDaoFromDao() {
        EmailDetails emailDetails = new EmailDetails();
        emailDetails.setTemplateId("template");
        emailDetails.setPersonalisationDetails("{\"key\":\"value\"}");

        EmailDetailsDao dao = EmailDetailsMapper.toDao(emailDetails);
        assertNotNull(dao);
        assertEquals(emailDetails.getTemplateId(), dao.getTemplateId());
        assertEquals(emailDetails.getPersonalisationDetails(), dao.getPersonalisationDetails());

        EmailDetails mappedBack = EmailDetailsMapper.fromDao(dao);
        assertNotNull(mappedBack);
        assertEquals(emailDetails, mappedBack);
    }

    @Test
    void toDaoNull() {
        EmailDetails emailDetails = null;
        assertNull(EmailDetailsMapper.toDao(emailDetails));
    }

    @Test
    void fromDaoNull() {
        EmailDetailsDao dao = null;
        assertNull(EmailDetailsMapper.fromDao(dao));
    }
}

