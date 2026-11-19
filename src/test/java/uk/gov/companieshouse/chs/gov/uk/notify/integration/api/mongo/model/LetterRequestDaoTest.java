package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

@DisplayName("LetterRequestDao equals method")
class LetterRequestDaoTest {

    @SuppressWarnings({"EqualsWithItself", "SelfEquals"})
    @Test
    @DisplayName("equals returns true when comparing the same LetterRequestDao instance")
    void equalsReturnsTrueForSameInstance() {
        LetterRequestDao letterRequestDao = new LetterRequestDao();

        //noinspection EqualsWithItself
        assertEquals(letterRequestDao, letterRequestDao);
    }

    @Test
    @DisplayName("equals returns false when comparing LetterRequestDao with null")
    void equalsReturnsFalseForNull() {
        LetterRequestDao letterRequestDao = new LetterRequestDao();

        assertNotEquals(null, letterRequestDao);
    }

    @Test
    @DisplayName("equals returns false when comparing LetterRequestDao with a different object type")
    void equalsReturnsFalseForDifferentObjectType() {
        LetterRequestDao letterRequestDao = new LetterRequestDao();

        //noinspection AssertBetweenInconvertibleTypes
        assertNotEquals("not a letter request dao", letterRequestDao);
    }

    @Test
    @DisplayName("equals returns true when all LetterRequestDao fields are equal")
    void equalsReturnsTrueWhenAllFieldsAreEqual() {
        SenderDetailsDao senderDetails = mock(SenderDetailsDao.class);
        LetterRecipientDetailsDao recipientDetails = mock(LetterRecipientDetailsDao.class);
        LetterDetailsDao letterDetails = new LetterDetailsDao();
        letterDetails.setLetterId("letter-id");
        letterDetails.setTemplateId("template-id");
        letterDetails.setPersonalisationDetails("{\"name\":\"Test\"}");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:15:30Z");

        LetterRequestDao firstLetterRequestDao = new LetterRequestDao();
        firstLetterRequestDao.setSenderDetails(senderDetails);
        firstLetterRequestDao.setRecipientDetails(recipientDetails);
        firstLetterRequestDao.setLetterDetails(letterDetails);
        firstLetterRequestDao.setCreatedAt(createdAt);

        LetterRequestDao secondLetterRequestDao = new LetterRequestDao();
        secondLetterRequestDao.setSenderDetails(senderDetails);
        secondLetterRequestDao.setRecipientDetails(recipientDetails);
        secondLetterRequestDao.setLetterDetails(letterDetails);
        secondLetterRequestDao.setCreatedAt(createdAt);

        assertEquals(firstLetterRequestDao, secondLetterRequestDao);
    }

    @Test
    @DisplayName("equals returns false when LetterRequestDao createdAt fields are different")
    void equalsReturnsFalseWhenCreatedAtIsDifferent() {
        SenderDetailsDao senderDetails = mock(SenderDetailsDao.class);
        LetterRecipientDetailsDao recipientDetails = mock(LetterRecipientDetailsDao.class);
        LetterDetailsDao letterDetails = new LetterDetailsDao();
        letterDetails.setLetterId("letter-id");
        letterDetails.setTemplateId("template-id");
        letterDetails.setPersonalisationDetails("{\"name\":\"Test\"}");

        LetterRequestDao firstLetterRequestDao = new LetterRequestDao();
        firstLetterRequestDao.setSenderDetails(senderDetails);
        firstLetterRequestDao.setRecipientDetails(recipientDetails);
        firstLetterRequestDao.setLetterDetails(letterDetails);
        firstLetterRequestDao.setCreatedAt(OffsetDateTime.parse("2026-01-01T10:15:30Z"));

        LetterRequestDao secondLetterRequestDao = new LetterRequestDao();
        secondLetterRequestDao.setSenderDetails(senderDetails);
        secondLetterRequestDao.setRecipientDetails(recipientDetails);
        secondLetterRequestDao.setLetterDetails(letterDetails);
        secondLetterRequestDao.setCreatedAt(OffsetDateTime.parse("2026-01-02T10:15:30Z"));

        assertNotEquals(firstLetterRequestDao, secondLetterRequestDao);
    }

    @Test
    @DisplayName("equals returns false when LetterRequestDao letterDetails fields are different")
    void equalsReturnsFalseWhenLetterDetailsIsDifferent() {
        SenderDetailsDao senderDetails = mock(SenderDetailsDao.class);
        LetterRecipientDetailsDao recipientDetails = mock(LetterRecipientDetailsDao.class);
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:15:30Z");

        LetterDetailsDao firstLetterDetails = new LetterDetailsDao();
        firstLetterDetails.setLetterId("letter-id");
        firstLetterDetails.setTemplateId("template-id");
        firstLetterDetails.setPersonalisationDetails("{\"name\":\"Test\"}");

        LetterDetailsDao secondLetterDetails = new LetterDetailsDao();
        secondLetterDetails.setLetterId("different-letter-id");
        secondLetterDetails.setTemplateId("template-id");
        secondLetterDetails.setPersonalisationDetails("{\"name\":\"Test\"}");

        LetterRequestDao firstLetterRequestDao = new LetterRequestDao();
        firstLetterRequestDao.setSenderDetails(senderDetails);
        firstLetterRequestDao.setRecipientDetails(recipientDetails);
        firstLetterRequestDao.setLetterDetails(firstLetterDetails);
        firstLetterRequestDao.setCreatedAt(createdAt);

        LetterRequestDao secondLetterRequestDao = new LetterRequestDao();
        secondLetterRequestDao.setSenderDetails(senderDetails);
        secondLetterRequestDao.setRecipientDetails(recipientDetails);
        secondLetterRequestDao.setLetterDetails(secondLetterDetails);
        secondLetterRequestDao.setCreatedAt(createdAt);

        assertNotEquals(firstLetterRequestDao, secondLetterRequestDao);
    }

    @Test
    @DisplayName("equals returns false when LetterRequestDao recipientDetails fields are different")
    void equalsReturnsFalseWhenRecipientDetailsIsDifferent() {
        SenderDetailsDao senderDetails = mock(SenderDetailsDao.class);
        LetterRecipientDetailsDao firstRecipientDetails = mock(LetterRecipientDetailsDao.class);
        LetterRecipientDetailsDao secondRecipientDetails = mock(LetterRecipientDetailsDao.class);
        LetterDetailsDao letterDetails = new LetterDetailsDao();
        letterDetails.setLetterId("letter-id");
        letterDetails.setTemplateId("template-id");
        letterDetails.setPersonalisationDetails("{\"name\":\"Test\"}");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:15:30Z");

        LetterRequestDao firstLetterRequestDao = new LetterRequestDao();
        firstLetterRequestDao.setSenderDetails(senderDetails);
        firstLetterRequestDao.setRecipientDetails(firstRecipientDetails);
        firstLetterRequestDao.setLetterDetails(letterDetails);
        firstLetterRequestDao.setCreatedAt(createdAt);

        LetterRequestDao secondLetterRequestDao = new LetterRequestDao();
        secondLetterRequestDao.setSenderDetails(senderDetails);
        secondLetterRequestDao.setRecipientDetails(secondRecipientDetails);
        secondLetterRequestDao.setLetterDetails(letterDetails);
        secondLetterRequestDao.setCreatedAt(createdAt);

        assertNotEquals(firstLetterRequestDao, secondLetterRequestDao);
    }

    @Test
    @DisplayName("equals returns false when LetterRequestDao senderDetails fields are different")
    void equalsReturnsFalseWhenSenderDetailsIsDifferent() {
        SenderDetailsDao firstSenderDetails = mock(SenderDetailsDao.class);
        SenderDetailsDao secondSenderDetails = mock(SenderDetailsDao.class);
        LetterRecipientDetailsDao recipientDetails = mock(LetterRecipientDetailsDao.class);
        LetterDetailsDao letterDetails = new LetterDetailsDao();
        letterDetails.setLetterId("letter-id");
        letterDetails.setTemplateId("template-id");
        letterDetails.setPersonalisationDetails("{\"name\":\"Test\"}");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:15:30Z");

        LetterRequestDao firstLetterRequestDao = new LetterRequestDao();
        firstLetterRequestDao.setSenderDetails(firstSenderDetails);
        firstLetterRequestDao.setRecipientDetails(recipientDetails);
        firstLetterRequestDao.setLetterDetails(letterDetails);
        firstLetterRequestDao.setCreatedAt(createdAt);

        LetterRequestDao secondLetterRequestDao = new LetterRequestDao();
        secondLetterRequestDao.setSenderDetails(secondSenderDetails);
        secondLetterRequestDao.setRecipientDetails(recipientDetails);
        secondLetterRequestDao.setLetterDetails(letterDetails);
        secondLetterRequestDao.setCreatedAt(createdAt);

        assertNotEquals(firstLetterRequestDao, secondLetterRequestDao);
    }
}
