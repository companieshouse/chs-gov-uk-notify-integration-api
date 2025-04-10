package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createSampleLetterRequest;

@SpringBootTest
class NotificationLetterRequestRepositoryTest extends AbstractMongoDBTest {
    
    @Autowired
    private NotificationLetterRequestRepository requestRepository;

    @Test
    void When_NewRequestSaved_Expect_IdAssigned() {
        GovUkLetterDetailsRequest letterRequest = createSampleLetterRequest("123 Main St");
        NotificationLetterRequest savedRequest = requestRepository.save(new NotificationLetterRequest(null, letterRequest));

        assertNotNull(savedRequest);
        assertNotNull(savedRequest.id());
    }

    @Test
    void When_RequestSaved_Expect_DataCanBeRetrievedById() {
        GovUkLetterDetailsRequest letterRequest = createSampleLetterRequest("456 Oak Ave");
        NotificationLetterRequest savedRequest = requestRepository.save(new NotificationLetterRequest(null, letterRequest));

        Optional<NotificationLetterRequest> retrievedRequest = requestRepository.findById(savedRequest.id());

        assertTrue(retrievedRequest.isPresent());
        assertEquals(savedRequest.id(), retrievedRequest.get().id());
        assertEquals("456 Oak Ave", retrievedRequest.get().request().getRecipientDetails().getPhysicalAddress().getAddressLine1());
    }

    @Test
    void When_MultipleRequestsSaved_Expect_AllCanBeRetrieved() {
        requestRepository.save(new NotificationLetterRequest(null, createSampleLetterRequest("123 First St")));
        requestRepository.save(new NotificationLetterRequest(null, createSampleLetterRequest("456 Second Ave")));
        requestRepository.save(new NotificationLetterRequest(null, createSampleLetterRequest("789 Third Blvd")));

        List<NotificationLetterRequest> allRequests = requestRepository.findAll();

        assertTrue(allRequests.size() >= 3);
    }

    @Test
    void When_RequestDeleted_Expect_RequestNotFoundById() {
        NotificationLetterRequest savedRequest = requestRepository.save(new NotificationLetterRequest(null, createSampleLetterRequest("Test Address")));

        requestRepository.deleteById(savedRequest.id());

        Optional<NotificationLetterRequest> deletedRequest = requestRepository.findById(savedRequest.id());
        assertFalse(deletedRequest.isPresent());
    }

    @Test
    void When_RequestUpdated_Expect_ChangesReflectedInDatabase() {
        GovUkLetterDetailsRequest initialRequest = createSampleLetterRequest("Initial Address");
        NotificationLetterRequest savedRequest = requestRepository.save(new NotificationLetterRequest(null, initialRequest));

        GovUkLetterDetailsRequest updatedRequest = createSampleLetterRequest("Updated Address");
        requestRepository.save(new NotificationLetterRequest(savedRequest.id(), updatedRequest));

        NotificationLetterRequest retrievedRequest = requestRepository.findById(savedRequest.id()).orElse(null);

        assertNotNull(retrievedRequest);
        assertEquals("Updated Address", retrievedRequest.request().getRecipientDetails().getPhysicalAddress().getAddressLine1());
    }
    
}
