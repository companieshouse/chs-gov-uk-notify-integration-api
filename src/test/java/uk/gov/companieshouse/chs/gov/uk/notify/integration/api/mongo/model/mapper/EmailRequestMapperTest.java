package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.chs.notification.model.EmailDetails;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.RecipientDetailsEmail;
import uk.gov.companieshouse.api.chs.notification.model.SenderDetails;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDao;

class EmailRequestMapperTest {
    @Test
    void toDaoFromDao() {
        // Setup
        SenderDetails sender = new SenderDetails();
        sender.setAppId("app");
        sender.setReference("ref");
        sender.setName("Sender Name");
        sender.setUserId("user");
        sender.setEmailAddress("sender@example.com");

        RecipientDetailsEmail recipient = new RecipientDetailsEmail();
        recipient.setName("Recipient Name");
        recipient.setEmailAddress("recipient@example.com");

        EmailDetails emailDetails = new EmailDetails();
        emailDetails.setTemplateId("template");
        emailDetails.setPersonalisationDetails("{\"key\":\"value\"}");

        GovUkEmailDetailsRequest request = new GovUkEmailDetailsRequest();
        request.setSenderDetails(sender);
        request.setRecipientDetails(recipient);
        request.setEmailDetails(emailDetails);
        request.setCreatedAt(OffsetDateTime.now());

        // toDao
        EmailRequestDao dao = EmailRequestMapper.toDao(request);
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
        assertEquals(recipient.getEmailAddress(), dao.getRecipientDetails().getEmailAddress());

        assertNotNull(dao.getEmailDetails());
        assertEquals(emailDetails.getTemplateId(), dao.getEmailDetails().getTemplateId());
        assertEquals(emailDetails.getPersonalisationDetails(), dao.getEmailDetails().getPersonalisationDetails());

        // fromDao
        GovUkEmailDetailsRequest mappedBack = EmailRequestMapper.fromDao(dao);
        assertEquals(request, mappedBack);
    }

    @Test
    void toDaoNullGovUkEmailDetailsRequest() {
        GovUkEmailDetailsRequest request = null;
        assertNull(EmailRequestMapper.toDao(request));
    }

    @Test
    void fromDaoNullEmailRequestDao() {
        EmailRequestDao dao = null;
        assertNull(EmailRequestMapper.fromDao(dao));
    }

}