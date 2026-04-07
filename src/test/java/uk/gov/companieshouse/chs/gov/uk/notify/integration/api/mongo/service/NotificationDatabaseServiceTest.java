package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createEmailRequest;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createLetterRequestWithAddressLine1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createSampleEmailResponse;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createSampleLetterResponse;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationEmailResponse;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationLetterResponse;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;

@SpringBootTest
class NotificationDatabaseServiceTest extends AbstractMongoDBTest {

    @Autowired
    private NotificationDatabaseService notificationDatabaseService;

    @Test
    void When_GetEmailByUniqueReference_ThenEmailRetrieved() {
        String appId = "chips";
        String otherAppId = "other-app";
        String reference = "TEST";

        EmailRequestDao email1 = createEmailRequest();
        email1.getSenderDetails().setAppId(appId);
        email1.getSenderDetails().setReference(reference);
        saveEmail(email1);

        EmailRequestDao email2 = createEmailRequest();
        email2.getSenderDetails().setAppId(otherAppId);
        email2.getSenderDetails().setReference(reference);
        saveEmail(email2);

        Optional<NotificationEmailRequest> retrievedRequest = notificationDatabaseService
                .getEmail(appId, reference);

        assertTrue(retrievedRequest.isPresent());
        assertNotEquals(otherAppId, retrievedRequest.get().getRequest().getSenderDetails().getAppId());
        assertEquals(email1.getEmailDetails(), retrievedRequest.get().getRequest().getEmailDetails());
        assertEquals(email1.getRecipientDetails(), retrievedRequest.get().getRequest().getRecipientDetails());
        assertEquals(email1.getSenderDetails(), retrievedRequest.get().getRequest().getSenderDetails());
    }

    @Test
    void When_GetEmailByUniqueReferenceNotMatching_ThenEmptyOptionalReturned() {
        EmailRequestDao email = createEmailRequest();
        saveEmail(email);

        Optional<NotificationEmailRequest> retrievedRequest = notificationDatabaseService
                .getEmail("invalid", email.getSenderDetails().getReference());

        assertFalse(retrievedRequest.isPresent());
    }

    @Test
    void When_GetLetterByUniqueReference_ThenEmailRetrieved() {
        String appId = "chips";
        String otherAppId = "other-app";
        String reference = "TEST";

        LetterRequestDao letter1 = TestUtils.createLetterRequest();
        letter1.getSenderDetails().setAppId(appId);
        letter1.getSenderDetails().setReference(reference);
        saveLetter(letter1);

        LetterRequestDao letter2 = TestUtils.createLetterRequest();
        letter2.getSenderDetails().setAppId(otherAppId);
        letter2.getSenderDetails().setReference(reference);
        saveLetter(letter2);

        Optional<NotificationLetterRequest> retrievedRequest = notificationDatabaseService
                .getLetter(appId, reference);

        assertTrue(retrievedRequest.isPresent());
        assertNotEquals(otherAppId, retrievedRequest.get().getRequest().getSenderDetails().getAppId());
        assertEquals(letter1.getLetterDetails(), retrievedRequest.get().getRequest().getLetterDetails());
        assertEquals(letter1.getRecipientDetails(), retrievedRequest.get().getRequest().getRecipientDetails());
        assertEquals(letter1.getSenderDetails(), retrievedRequest.get().getRequest().getSenderDetails());
    }

    @Test
    void When_GetLetterByUniqueReferenceNotMatching_ThenEmptyOptionalReturned() {
        LetterRequestDao letter = TestUtils.createLetterRequest();
        saveLetter(letter);

        Optional<NotificationEmailRequest> retrievedRequest = notificationDatabaseService
                .getEmail("invalid", letter.getSenderDetails().getReference());

        assertFalse(retrievedRequest.isPresent());
    }

    @Test
    void When_GetLetterByReference_ThenLettersRetrieved() {
        LetterRequestDao letterRequest = createLetterRequestWithAddressLine1("123 Main Street");
        String reference = letterRequest.getSenderDetails().getReference();
        saveLetter(letterRequest);

        List<NotificationLetterRequest> retrievedLetters = notificationDatabaseService.getLetterByReference(reference);

        assertNotNull(retrievedLetters);
        assertFalse(retrievedLetters.isEmpty());
        LetterRequestDao mongoLetter = retrievedLetters.get(0).getRequest();
        assertEquals(reference, mongoLetter.getSenderDetails().getReference());
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
    void When_MultipleLettersWithSameReference_ThenAllRetrieved() {
        LetterRequestDao letter1 = createLetterRequestWithAddressLine1("123 Main St");
        LetterRequestDao letter2 = createLetterRequestWithAddressLine1("456 High St");
        String reference = letter1.getSenderDetails().getReference();
        letter2.getSenderDetails().setReference(reference); // Set same reference for both letters

        saveLetter(letter1);
        saveLetter(letter2);

        List<NotificationLetterRequest> retrievedLetters = notificationDatabaseService.getLetterByReference(reference);

        assertNotNull(retrievedLetters);
        assertEquals(2, retrievedLetters.size());
        for (NotificationLetterRequest req : retrievedLetters) {
            LetterRequestDao mongoLetter = req.getRequest();
            assertEquals(reference, mongoLetter.getSenderDetails().getReference());
        }
    }

    private NotificationEmailRequest saveEmail(EmailRequestDao emailRequest) {
        return notificationDatabaseService.saveEmail(new NotificationEmailRequest(emailRequest));
    }

    private NotificationLetterRequest saveLetter(LetterRequestDao letterRequest) {
        return notificationDatabaseService.saveLetter(new NotificationLetterRequest(letterRequest));
    }
}