package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createLetterRequestWithAddressLine1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createLetterRequestWithReference;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationLetterRequest;


@SpringBootTest
class NotificationLetterRequestRepositoryTest extends AbstractMongoDBTest {

    @Test
    void When_NewRequestSaved_Expect_IdAssigned() {
        LetterRequestDao letterRequest = createLetterRequestWithAddressLine1("123 Main St");
        NotificationLetterRequest notificationLetterRequest = new NotificationLetterRequest();
        notificationLetterRequest.setRequest(letterRequest);
        notificationLetterRequest.setId(null);
        notificationLetterRequest.setCreatedAt(null);
        notificationLetterRequest.setUpdatedAt(null);
        NotificationLetterRequest savedRequest = notificationLetterRequestRepository.save(notificationLetterRequest);

        assertNotNull(savedRequest.toString());
        assertNotNull(savedRequest.getId());
        assertNotNull(savedRequest.getCreatedAt());
        assertNotNull(savedRequest.getUpdatedAt());
    }

    @Test
    void When_RequestSaved_Expect_DataCanBeRetrievedById() {
        LetterRequestDao letterRequest = createLetterRequestWithAddressLine1("456 Oak Ave");
        NotificationLetterRequest savedRequest = notificationLetterRequestRepository.save(new NotificationLetterRequest(letterRequest));

        Optional<NotificationLetterRequest> retrievedRequest = notificationLetterRequestRepository.findById(savedRequest.getId());

        assertTrue(retrievedRequest.isPresent());
        assertEquals(savedRequest.getId(), retrievedRequest.get().getId());
        assertEquals("456 Oak Ave", retrievedRequest.get().getRequest().getRecipientDetails().getPhysicalAddress().getAddressLine1());
    }

    @Test
    void When_MultipleRequestsSaved_Expect_AllCanBeRetrieved() {
        notificationLetterRequestRepository.save(new NotificationLetterRequest(createLetterRequestWithAddressLine1("123 First St")));
        notificationLetterRequestRepository.save(new NotificationLetterRequest(createLetterRequestWithAddressLine1("456 Second Ave")));
        notificationLetterRequestRepository.save(new NotificationLetterRequest(createLetterRequestWithAddressLine1("789 Third Blvd")));

        List<NotificationLetterRequest> allRequests = notificationLetterRequestRepository.findAll();

        assertTrue(allRequests.size() >= 3);
    }

    @Test
    void When_RequestDeleted_Expect_RequestNotFoundById() {
        NotificationLetterRequest savedRequest = notificationLetterRequestRepository.save(new NotificationLetterRequest(createLetterRequestWithAddressLine1("Test Address")));

        notificationLetterRequestRepository.deleteById(savedRequest.getId());

        Optional<NotificationLetterRequest> deletedRequest = notificationLetterRequestRepository.findById(savedRequest.getId());
        assertFalse(deletedRequest.isPresent());
    }

    @Test
    void When_RequestUpdated_Expect_ChangesReflectedInDatabase() {
        LetterRequestDao initialRequest = createLetterRequestWithAddressLine1("Initial Address");
        NotificationLetterRequest savedRequest = notificationLetterRequestRepository.save(new NotificationLetterRequest(initialRequest));

        savedRequest.getRequest().getRecipientDetails().getPhysicalAddress().setAddressLine1( "Updated Address" );

        notificationLetterRequestRepository.save(savedRequest);

        NotificationLetterRequest retrievedRequest = notificationLetterRequestRepository.findById(savedRequest.getId()).orElse(null);

        assertNotNull(retrievedRequest);
        assertEquals("Updated Address", retrievedRequest.getRequest().getRecipientDetails().getPhysicalAddress().getAddressLine1());
    }

    @Test
    void findByUniqueReference() {
        String appId = "chips";
        String otherAppId = "other-app";
        String reference = "TEST";
        var letter1 = saveLetterWithReference(appId, reference);
        saveLetterWithReference(otherAppId, reference);

        var result = notificationLetterRequestRepository.findByUniqueReference(appId, reference);

        assertNotNull(result);
        assertTrue(result.isPresent());
        assertNotEquals(otherAppId, result.get().getRequest().getSenderDetails().getAppId());
        assertEquals(letter1.getLetterDetails(), result.get().getRequest().getLetterDetails());
        assertEquals(letter1.getRecipientDetails(), result.get().getRequest().getRecipientDetails());
        assertEquals(letter1.getSenderDetails(), result.get().getRequest().getSenderDetails());
    }

    @Test
    void findByUniqueReference_notFound() {
        String reference = "TEST";
        saveLetterWithReference("chips", reference);
        saveLetterWithReference("other-app", reference);

        var result = notificationLetterRequestRepository.findByUniqueReference("app-id", reference);

        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    private LetterRequestDao saveLetterWithReference(String appId, String reference) {
        var letter = createLetterRequestWithReference(reference);
        letter.getSenderDetails().setAppId(appId);
        notificationLetterRequestRepository.save(new NotificationLetterRequest(letter));
        return letter;
    }
}
