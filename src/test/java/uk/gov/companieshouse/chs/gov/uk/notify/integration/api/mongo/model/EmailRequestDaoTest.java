package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("EmailRequestDao equals method")
class EmailRequestDaoTest {

    @SuppressWarnings({"EqualsWithItself", "ConstantValue", "SelfEquals"})
    @Test
    @DisplayName("EmailRequestDao equals returns true when compared with itself")
    void equalsReturnsTrueWhenComparedWithItself() {
        EmailRequestDao emailRequestDao = new EmailRequestDao();
        emailRequestDao.setSenderDetails(mock(SenderDetailsDao.class));
        emailRequestDao.setRecipientDetails(mock(EmailRecipientDetailsDao.class));
        emailRequestDao.setEmailDetails(mock(EmailDetailsDao.class));
        emailRequestDao.setCreatedAt(OffsetDateTime.parse("2026-01-01T10:15:30Z"));

        boolean result = emailRequestDao.equals(emailRequestDao);

        assertTrue(result);
    }

    @SuppressWarnings("ConstantValue")
    @Test
    @DisplayName("EmailRequestDao equals returns false when compared with null")
    void equalsReturnsFalseWhenComparedWithNull() {
        EmailRequestDao emailRequestDao = new EmailRequestDao();
        emailRequestDao.setSenderDetails(mock(SenderDetailsDao.class));
        emailRequestDao.setRecipientDetails(mock(EmailRecipientDetailsDao.class));
        emailRequestDao.setEmailDetails(mock(EmailDetailsDao.class));
        emailRequestDao.setCreatedAt(OffsetDateTime.parse("2026-01-01T10:15:30Z"));

        boolean result = emailRequestDao.equals(null);

        assertFalse(result);
    }

    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    @Test
    @DisplayName("EmailRequestDao equals returns false when compared with a different object type")
    void equalsReturnsFalseWhenComparedWithDifferentObjectType() {
        EmailRequestDao emailRequestDao = new EmailRequestDao();
        emailRequestDao.setSenderDetails(mock(SenderDetailsDao.class));
        emailRequestDao.setRecipientDetails(mock(EmailRecipientDetailsDao.class));
        emailRequestDao.setEmailDetails(mock(EmailDetailsDao.class));
        emailRequestDao.setCreatedAt(OffsetDateTime.parse("2026-01-01T10:15:30Z"));

        boolean result = emailRequestDao.equals("not an EmailRequestDao");

        assertFalse(result);
    }

    @Test
    @DisplayName("EmailRequestDao equals returns true when all fields are equal")
    void equalsReturnsTrueWhenAllFieldsAreEqual() {
        SenderDetailsDao senderDetails = mock(SenderDetailsDao.class);
        EmailRecipientDetailsDao recipientDetails = mock(EmailRecipientDetailsDao.class);
        EmailDetailsDao emailDetails = mock(EmailDetailsDao.class);
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:15:30Z");

        EmailRequestDao firstEmailRequestDao = new EmailRequestDao();
        firstEmailRequestDao.setSenderDetails(senderDetails);
        firstEmailRequestDao.setRecipientDetails(recipientDetails);
        firstEmailRequestDao.setEmailDetails(emailDetails);
        firstEmailRequestDao.setCreatedAt(createdAt);

        EmailRequestDao secondEmailRequestDao = new EmailRequestDao();
        secondEmailRequestDao.setSenderDetails(senderDetails);
        secondEmailRequestDao.setRecipientDetails(recipientDetails);
        secondEmailRequestDao.setEmailDetails(emailDetails);
        secondEmailRequestDao.setCreatedAt(createdAt);

        boolean result = firstEmailRequestDao.equals(secondEmailRequestDao);

        assertTrue(result);
    }

    @Test
    @DisplayName("EmailRequestDao equals returns true when all fields are null")
    void equalsReturnsTrueWhenAllFieldsAreNull() {
        EmailRequestDao firstEmailRequestDao = new EmailRequestDao();
        EmailRequestDao secondEmailRequestDao = new EmailRequestDao();

        boolean result = firstEmailRequestDao.equals(secondEmailRequestDao);

        assertTrue(result);
    }

    @Test
    @DisplayName("EmailRequestDao equals returns false when createdAt differs")
    void equalsReturnsFalseWhenCreatedAtDiffers() {
        SenderDetailsDao senderDetails = mock(SenderDetailsDao.class);
        EmailRecipientDetailsDao recipientDetails = mock(EmailRecipientDetailsDao.class);
        EmailDetailsDao emailDetails = mock(EmailDetailsDao.class);

        EmailRequestDao firstEmailRequestDao = new EmailRequestDao();
        firstEmailRequestDao.setSenderDetails(senderDetails);
        firstEmailRequestDao.setRecipientDetails(recipientDetails);
        firstEmailRequestDao.setEmailDetails(emailDetails);
        firstEmailRequestDao.setCreatedAt(OffsetDateTime.parse("2026-01-01T10:15:30Z"));

        EmailRequestDao secondEmailRequestDao = new EmailRequestDao();
        secondEmailRequestDao.setSenderDetails(senderDetails);
        secondEmailRequestDao.setRecipientDetails(recipientDetails);
        secondEmailRequestDao.setEmailDetails(emailDetails);
        secondEmailRequestDao.setCreatedAt(OffsetDateTime.parse("2026-01-02T10:15:30Z"));

        boolean result = firstEmailRequestDao.equals(secondEmailRequestDao);

        assertFalse(result);
    }

    @Test
    @DisplayName("EmailRequestDao equals returns false when emailDetails differs")
    void equalsReturnsFalseWhenEmailDetailsDiffers() {
        SenderDetailsDao senderDetails = mock(SenderDetailsDao.class);
        EmailRecipientDetailsDao recipientDetails = mock(EmailRecipientDetailsDao.class);
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:15:30Z");

        EmailRequestDao firstEmailRequestDao = new EmailRequestDao();
        firstEmailRequestDao.setSenderDetails(senderDetails);
        firstEmailRequestDao.setRecipientDetails(recipientDetails);
        firstEmailRequestDao.setEmailDetails(mock(EmailDetailsDao.class));
        firstEmailRequestDao.setCreatedAt(createdAt);

        EmailRequestDao secondEmailRequestDao = new EmailRequestDao();
        secondEmailRequestDao.setSenderDetails(senderDetails);
        secondEmailRequestDao.setRecipientDetails(recipientDetails);
        secondEmailRequestDao.setEmailDetails(mock(EmailDetailsDao.class));
        secondEmailRequestDao.setCreatedAt(createdAt);

        boolean result = firstEmailRequestDao.equals(secondEmailRequestDao);

        assertFalse(result);
    }

    @Test
    @DisplayName("EmailRequestDao equals returns false when recipientDetails differs")
    void equalsReturnsFalseWhenRecipientDetailsDiffers() {
        SenderDetailsDao senderDetails = mock(SenderDetailsDao.class);
        EmailDetailsDao emailDetails = mock(EmailDetailsDao.class);
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:15:30Z");

        EmailRequestDao firstEmailRequestDao = new EmailRequestDao();
        firstEmailRequestDao.setSenderDetails(senderDetails);
        firstEmailRequestDao.setRecipientDetails(mock(EmailRecipientDetailsDao.class));
        firstEmailRequestDao.setEmailDetails(emailDetails);
        firstEmailRequestDao.setCreatedAt(createdAt);

        EmailRequestDao secondEmailRequestDao = new EmailRequestDao();
        secondEmailRequestDao.setSenderDetails(senderDetails);
        secondEmailRequestDao.setRecipientDetails(mock(EmailRecipientDetailsDao.class));
        secondEmailRequestDao.setEmailDetails(emailDetails);
        secondEmailRequestDao.setCreatedAt(createdAt);

        boolean result = firstEmailRequestDao.equals(secondEmailRequestDao);

        assertFalse(result);
    }

    @Test
    @DisplayName("EmailRequestDao equals returns false when senderDetails differs")
    void equalsReturnsFalseWhenSenderDetailsDiffers() {
        EmailRecipientDetailsDao recipientDetails = mock(EmailRecipientDetailsDao.class);
        EmailDetailsDao emailDetails = mock(EmailDetailsDao.class);
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:15:30Z");

        EmailRequestDao firstEmailRequestDao = new EmailRequestDao();
        firstEmailRequestDao.setSenderDetails(mock(SenderDetailsDao.class));
        firstEmailRequestDao.setRecipientDetails(recipientDetails);
        firstEmailRequestDao.setEmailDetails(emailDetails);
        firstEmailRequestDao.setCreatedAt(createdAt);

        EmailRequestDao secondEmailRequestDao = new EmailRequestDao();
        secondEmailRequestDao.setSenderDetails(mock(SenderDetailsDao.class));
        secondEmailRequestDao.setRecipientDetails(recipientDetails);
        secondEmailRequestDao.setEmailDetails(emailDetails);
        secondEmailRequestDao.setCreatedAt(createdAt);

        boolean result = firstEmailRequestDao.equals(secondEmailRequestDao);

        assertFalse(result);
    }
}
