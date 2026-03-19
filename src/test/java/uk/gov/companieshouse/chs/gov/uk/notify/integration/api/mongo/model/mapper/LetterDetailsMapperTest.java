package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.chs.notification.model.LetterDetails;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterDetailsDao;

class LetterDetailsMapperTest {
    @Test
    void toDaoFromDao() {
        LetterDetails letterDetails = new LetterDetails();
        letterDetails.setLetterId("letterId");
        letterDetails.setTemplateId("template");
        letterDetails.setPersonalisationDetails("{\"key\":\"value\"}");

        LetterDetailsDao dao = LetterDetailsMapper.toDao(letterDetails);
        assertNotNull(dao);
        assertEquals(letterDetails.getLetterId(), dao.getLetterId());
        assertEquals(letterDetails.getTemplateId(), dao.getTemplateId());
        assertEquals(letterDetails.getPersonalisationDetails(), dao.getPersonalisationDetails());

        LetterDetails mappedBack = LetterDetailsMapper.fromDao(dao);
        assertNotNull(mappedBack);
        assertEquals(letterDetails, mappedBack);
    }

    @Test
    void toDaoNull() {
        LetterDetails letterDetails = null;
        assertNull(LetterDetailsMapper.toDao(letterDetails));
    }

    @Test
    void fromDaoNull() {
        LetterDetailsDao dao = null;
        assertNull(LetterDetailsMapper.fromDao(dao));
    }
}

