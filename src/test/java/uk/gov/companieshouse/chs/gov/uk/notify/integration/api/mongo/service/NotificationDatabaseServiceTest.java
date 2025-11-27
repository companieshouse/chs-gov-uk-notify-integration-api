package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailResponse;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterResponse;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationStatus;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.*;

@Tag("integration-test")
@SpringBootTest
class NotificationDatabaseServiceTest extends AbstractMongoDBTest {
    
    @Autowired
    private NotificationDatabaseService notificationDatabaseService;

    @Test
    void When_StoreEmail_ThenEmailStored() {
        GovUkEmailDetailsRequest emailRequest = createSampleEmailRequest("john.doe@example.com");
        NotificationEmailRequest savedRequest = notificationDatabaseService.storeEmail(emailRequest);

        assertNotNull(savedRequest);
        assertNotNull(savedRequest.getId());
    }

    @Test
    void When_GetEmail_ThenEmailRetrieved() {
        GovUkEmailDetailsRequest emailRequest = createSampleEmailRequest("jane.smith@example.com");
        NotificationEmailRequest savedRequest = notificationDatabaseService.storeEmail(emailRequest);
        String id = savedRequest.getId();

        Optional<NotificationEmailRequest> retrievedRequest = notificationDatabaseService.getEmail(id);

        assertTrue(retrievedRequest.isPresent());
        assertEquals(id, retrievedRequest.get().getId());
        assertEquals("jane.smith@example.com", retrievedRequest.get().getRequest().getRecipientDetails().getEmailAddress());
    }

    @Test
    void When_GetEmailWithInvalidId_ThenEmptyOptionalReturned() {
        Optional<NotificationEmailRequest> retrievedRequest = notificationDatabaseService.getEmail("nonexistent-id");

        assertFalse(retrievedRequest.isPresent());
    }

    @Test
    void When_FindAllEmails_ThenAllEmailsRetrieved() {
        notificationDatabaseService.storeEmail(createSampleEmailRequest("user1@example.com"));
        notificationDatabaseService.storeEmail(createSampleEmailRequest("user2@example.com"));

        List<NotificationEmailRequest> allEmails = notificationDatabaseService.findAllEmails();

        assertNotNull(allEmails);
        assertTrue(allEmails.size() >= 2);
    }

    @Test
    void When_StoreLetter_ThenLetterStored() {
        GovUkLetterDetailsRequest letterRequest = createSampleLetterRequest("123 Main Street");
        NotificationLetterRequest savedRequest = notificationDatabaseService.storeLetter(letterRequest);

        assertNotNull(savedRequest);
        assertNotNull(savedRequest.getId());
    }

    @Test
    void When_GetLetter_ThenLetterRetrieved() {
        GovUkLetterDetailsRequest letterRequest = createSampleLetterRequest("456 High Street");
        NotificationLetterRequest savedRequest = notificationDatabaseService.storeLetter(letterRequest);
        String id = savedRequest.getId();

        Optional<NotificationLetterRequest> retrievedRequest = notificationDatabaseService.getLetter(id);

        assertTrue(retrievedRequest.isPresent());
        assertEquals(id, retrievedRequest.get().getId());
        assertEquals("456 High Street", retrievedRequest.get().getRequest().getRecipientDetails().getPhysicalAddress().getAddressLine1());
    }

    @Test
    void When_GetLetterWithInvalidId_ThenEmptyOptionalReturned() {
        Optional<NotificationLetterRequest> retrievedRequest = notificationDatabaseService.getLetter("nonexistent-id");

        assertFalse(retrievedRequest.isPresent());
    }

    @Test
    void When_FindAllLetters_ThenAllLettersRetrieved() {
        notificationDatabaseService.storeLetter(createSampleLetterRequest("789 Broadway"));
        notificationDatabaseService.storeLetter(createSampleLetterRequest("101 Park Avenue"));

        List<NotificationLetterRequest> allLetters = notificationDatabaseService.findAllLetters();

        assertNotNull(allLetters);
        assertTrue(allLetters.size() >= 2);
    }

    @Test
    void When_UpdateStatus_ThenStatusUpdated() {
        String requestId = UUID.randomUUID().toString();
        String responseId = UUID.randomUUID().toString();
        Map<String, Object> statusDetails = Map.of(
                "timestamp", System.currentTimeMillis(),
                "message", "Email sent successfully",
                "deliveryChannel", "email"
        );

        NotificationStatus status = new NotificationStatus(
                null,
                null,
                requestId,
                responseId,
                "SENT",
                statusDetails,
                null
        );

        NotificationStatus savedStatus = notificationDatabaseService.updateStatus(status);

        assertNotNull(savedStatus);
        assertNotNull(savedStatus.getId());
        assertEquals(requestId, savedStatus.getRequestId());
        assertEquals(responseId, savedStatus.getResponseId());
        assertEquals("SENT", savedStatus.getStatus());
        assertNotNull(savedStatus.getStatusDetails());
        assertEquals("Email sent successfully", savedStatus.getStatusDetails().get("message"));
    }

    @Test
    void When_GetEmailByReference_ThenEmailsRetrieved() {
        String reference = "REF-123-EMAIL";
        GovUkEmailDetailsRequest emailRequest = createSampleEmailRequestWithReference("user1@example.com", reference);
        notificationDatabaseService.storeEmail(emailRequest);

        List<NotificationEmailRequest> retrievedEmails = notificationDatabaseService.getEmailByReference(reference);

        assertNotNull(retrievedEmails);
        assertFalse(retrievedEmails.isEmpty());
        assertEquals(reference, retrievedEmails.get(0).getRequest().getSenderDetails().getReference());
    }

    @Test
    void When_GetEmailByNonexistentReference_ThenEmptyListReturned() {
        List<NotificationEmailRequest> retrievedEmails = notificationDatabaseService.getEmailByReference("NONEXISTENT-REF");

        assertNotNull(retrievedEmails);
        assertTrue(retrievedEmails.isEmpty());
    }

    @Test
    void When_GetLetterByReference_ThenLettersRetrieved() {
        String reference = "REF-456-LETTER";
        GovUkLetterDetailsRequest letterRequest = createSampleLetterRequestWithReference("123 Main Street", reference);
        notificationDatabaseService.storeLetter(letterRequest);

        List<NotificationLetterRequest> retrievedLetters = notificationDatabaseService.getLetterByReference(reference);

        assertNotNull(retrievedLetters);
        assertFalse(retrievedLetters.isEmpty());
        assertEquals(reference, retrievedLetters.get(0).getRequest().getSenderDetails().getReference());
    }

    @Test
    void When_GetLetterByNonexistentReference_ThenEmptyListReturned() {
        List<NotificationLetterRequest> retrievedLetters = notificationDatabaseService.getLetterByReference("NONEXISTENT-REF");

        assertNotNull(retrievedLetters);
        assertTrue(retrievedLetters.isEmpty());
    }

    @Test
    void When_StoreEmailResponse_ThenResponseStored() {
        GovUkNotifyService.EmailResp emailResp = new GovUkNotifyService.EmailResp(true, createSampleEmailResponse());
        NotificationEmailResponse savedResponse = notificationDatabaseService.storeResponse(emailResp);
        assertNotNull(savedResponse);
    }

    @Test
    void When_StoreLetterResponse_ThenResponseStored() {
        GovUkNotifyService.LetterResp letterResp = new GovUkNotifyService.LetterResp(true, createSampleLetterResponse());
        NotificationLetterResponse savedResponse = notificationDatabaseService.storeResponse(letterResp);
        assertNotNull(savedResponse);
    }

    @Test
    void When_MultipleEmailsWithSameReference_ThenAllRetrieved() {
        String reference = "MULTI-EMAIL-REF";
        GovUkEmailDetailsRequest email1 = createSampleEmailRequestWithReference("user1@example.com", reference);
        GovUkEmailDetailsRequest email2 = createSampleEmailRequestWithReference("user2@example.com", reference);

        notificationDatabaseService.storeEmail(email1);
        notificationDatabaseService.storeEmail(email2);

        List<NotificationEmailRequest> retrievedEmails = notificationDatabaseService.getEmailByReference(reference);

        assertNotNull(retrievedEmails);
        assertEquals(2, retrievedEmails.size());
        assertEquals(reference, retrievedEmails.get(0).getRequest().getSenderDetails().getReference());
        assertEquals(reference, retrievedEmails.get(1).getRequest().getSenderDetails().getReference());
    }

    @Test
    void When_MultipleLettersWithSameReference_ThenAllRetrieved() {
        String reference = "MULTI-LETTER-REF";
        GovUkLetterDetailsRequest letter1 = createSampleLetterRequestWithReference("123 Main St", reference);
        GovUkLetterDetailsRequest letter2 = createSampleLetterRequestWithReference("456 High St", reference);

        notificationDatabaseService.storeLetter(letter1);
        notificationDatabaseService.storeLetter(letter2);

        List<NotificationLetterRequest> retrievedLetters = notificationDatabaseService.getLetterByReference(reference);

        assertNotNull(retrievedLetters);
        assertEquals(2, retrievedLetters.size());
        assertEquals(reference, retrievedLetters.get(0).getRequest().getSenderDetails().getReference());
        assertEquals(reference, retrievedLetters.get(1).getRequest().getSenderDetails().getReference());
    }
}
