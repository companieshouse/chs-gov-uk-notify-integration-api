package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.dao;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.SharedMongoContainer;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.NotificationLetterRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createSampleLetterRequest;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NotificationLetterRequestRepositoryTest {

    static {
        SharedMongoContainer.getInstance();
    }
    
    @Autowired
    private NotificationLetterRequestRepository requestRepository;

    @Test
    public void When_NewRequestSaved_Expect_IdAssigned() {
        GovUkLetterDetailsRequest letterRequest = createSampleLetterRequest("123 Main St");
        NotificationLetterRequest savedRequest = requestRepository.save(new NotificationLetterRequest(null, letterRequest));

        assertNotNull(savedRequest);
        assertNotNull(savedRequest.id());
    }

    @Test
    public void When_RequestSaved_Expect_DataCanBeRetrievedById() {
        GovUkLetterDetailsRequest letterRequest = createSampleLetterRequest("456 Oak Ave");
        NotificationLetterRequest savedRequest = requestRepository.save(new NotificationLetterRequest(null, letterRequest));

        Optional<NotificationLetterRequest> retrievedRequest = requestRepository.findById(savedRequest.id());

        assertTrue(retrievedRequest.isPresent());
        assertEquals(savedRequest.id(), retrievedRequest.get().id());
        assertEquals("456 Oak Ave", retrievedRequest.get().request().getRecipientDetails().getPhysicalAddress().getAddressLine1());
    }

    @Test
    public void When_MultipleRequestsSaved_Expect_AllCanBeRetrieved() {
        requestRepository.save(new NotificationLetterRequest(null, createSampleLetterRequest("123 First St")));
        requestRepository.save(new NotificationLetterRequest(null, createSampleLetterRequest("456 Second Ave")));
        requestRepository.save(new NotificationLetterRequest(null, createSampleLetterRequest("789 Third Blvd")));

        List<NotificationLetterRequest> allRequests = requestRepository.findAll();

        assertTrue(allRequests.size() >= 3);
    }

    @Test
    public void When_RequestDeleted_Expect_RequestNotFoundById() {
        NotificationLetterRequest savedRequest = requestRepository.save(new NotificationLetterRequest(null, createSampleLetterRequest("Test Address")));

        requestRepository.deleteById(savedRequest.id());

        Optional<NotificationLetterRequest> deletedRequest = requestRepository.findById(savedRequest.id());
        assertFalse(deletedRequest.isPresent());
    }

    @Test
    public void When_RequestUpdated_Expect_ChangesReflectedInDatabase() {
        GovUkLetterDetailsRequest initialRequest = createSampleLetterRequest("Initial Address");
        NotificationLetterRequest savedRequest = requestRepository.save(new NotificationLetterRequest(null, initialRequest));

        GovUkLetterDetailsRequest updatedRequest = createSampleLetterRequest("Updated Address");
        requestRepository.save(new NotificationLetterRequest(savedRequest.id(), updatedRequest));

        NotificationLetterRequest retrievedRequest = requestRepository.findById(savedRequest.id()).orElse(null);

        assertNotNull(retrievedRequest);
        assertEquals("Updated Address", retrievedRequest.request().getRecipientDetails().getPhysicalAddress().getAddressLine1());
    }
    
}
