package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.SharedMongoContainer;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.TestUtils.createSampleEmailRequest;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.TestUtils.createSampleLetterRequest;


@RunWith(SpringRunner.class)
@SpringBootTest
public class NotificationDatabaseServiceTest {

    static {
        SharedMongoContainer.getInstance();
    }

    @Autowired
    private NotificationDatabaseService notificationDatabaseService;

    @Test
    public void When_StoreEmail_ThenEmailStored() {
        GovUkEmailDetailsRequest emailRequest = createSampleEmailRequest("john.doe@example.com");
        NotificationEmailRequest savedRequest = notificationDatabaseService.storeEmail(emailRequest);

        assertNotNull(savedRequest);
        assertNotNull(savedRequest.id());
    }

    @Test
    public void When_GetEmail_ThenEmailRetrieved() {
        GovUkEmailDetailsRequest emailRequest = createSampleEmailRequest("jane.smith@example.com");
        NotificationEmailRequest savedRequest = notificationDatabaseService.storeEmail(emailRequest);
        String id = savedRequest.id();

        Optional<NotificationEmailRequest> retrievedRequest = notificationDatabaseService.getEmail(id);

        assertTrue(retrievedRequest.isPresent());
        assertEquals(id, retrievedRequest.get().id());
        assertEquals("jane.smith@example.com", retrievedRequest.get().request().getRecipientDetails().getEmailAddress());
    }

    @Test
    public void When_GetEmailWithInvalidId_ThenEmptyOptionalReturned() {
        Optional<NotificationEmailRequest> retrievedRequest = notificationDatabaseService.getEmail("nonexistent-id");

        assertFalse(retrievedRequest.isPresent());
    }

    @Test
    public void When_FindAllEmails_ThenAllEmailsRetrieved() {
        notificationDatabaseService.storeEmail(createSampleEmailRequest("user1@example.com"));
        notificationDatabaseService.storeEmail(createSampleEmailRequest("user2@example.com"));

        List<NotificationEmailRequest> allEmails = notificationDatabaseService.findAllEmails();

        assertNotNull(allEmails);
        assertTrue(allEmails.size() >= 2);
    }

    @Test
    public void When_StoreLetter_ThenLetterStored() {
        GovUkLetterDetailsRequest letterRequest = createSampleLetterRequest("123 Main Street");
        NotificationLetterRequest savedRequest = notificationDatabaseService.storeLetter(letterRequest);

        assertNotNull(savedRequest);
        assertNotNull(savedRequest.id());
    }

    @Test
    public void When_GetLetter_ThenLetterRetrieved() {
        GovUkLetterDetailsRequest letterRequest = createSampleLetterRequest("456 High Street");
        NotificationLetterRequest savedRequest = notificationDatabaseService.storeLetter(letterRequest);
        String id = savedRequest.id();

        Optional<NotificationLetterRequest> retrievedRequest = notificationDatabaseService.getLetter(id);

        assertTrue(retrievedRequest.isPresent());
        assertEquals(id, retrievedRequest.get().id());
        assertEquals("456 High Street", retrievedRequest.get().request().getRecipientDetails().getPhysicalAddress().getAddressLine1());
    }

    @Test
    public void When_GetLetterWithInvalidId_ThenEmptyOptionalReturned() {
        Optional<NotificationLetterRequest> retrievedRequest = notificationDatabaseService.getLetter("nonexistent-id");

        assertFalse(retrievedRequest.isPresent());
    }

    @Test
    public void When_FindAllLetters_ThenAllLettersRetrieved() {
        notificationDatabaseService.storeLetter(createSampleLetterRequest("789 Broadway"));
        notificationDatabaseService.storeLetter(createSampleLetterRequest("101 Park Avenue"));

        List<NotificationLetterRequest> allLetters = notificationDatabaseService.findAllLetters();

        assertNotNull(allLetters);
        assertTrue(allLetters.size() >= 2);
    }

    @Test
    public void When_UpdateStatus_ThenStatusUpdated() {
        String requestId = UUID.randomUUID().toString();
        String responseId = UUID.randomUUID().toString();
        Map<String, Object> statusDetails = Map.of(
                "timestamp", System.currentTimeMillis(),
                "message", "Email sent successfully",
                "deliveryChannel", "email"
        );

        NotificationStatus status = new NotificationStatus(
                null,
                requestId,
                responseId,
                "SENT",
                statusDetails
        );

        NotificationStatus savedStatus = notificationDatabaseService.updateStatus(status);

        assertNotNull(savedStatus);
        assertNotNull(savedStatus.id());
        assertEquals(requestId, savedStatus.requestId());
        assertEquals(responseId, savedStatus.responseId());
        assertEquals("SENT", savedStatus.status());
        assertNotNull(savedStatus.statusDetails());
        assertEquals("Email sent successfully", savedStatus.statusDetails().get("message"));
    }
}
