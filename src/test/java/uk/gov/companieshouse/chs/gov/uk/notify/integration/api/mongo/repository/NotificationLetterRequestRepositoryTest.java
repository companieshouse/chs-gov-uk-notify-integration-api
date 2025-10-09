package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createLetterWithReference;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createSampleLetterRequest;

@SpringBootTest
class NotificationLetterRequestRepositoryTest extends AbstractMongoDBTest {
    
    @Autowired
    private NotificationLetterRequestRepository requestRepository;

    @Test
    void When_NewRequestSaved_Expect_IdAssigned() {
        GovUkLetterDetailsRequest letterRequest = createSampleLetterRequest("123 Main St");
        NotificationLetterRequest notificationLetterRequest = new NotificationLetterRequest();
        notificationLetterRequest.setRequest(letterRequest);
        notificationLetterRequest.setId(null);
        notificationLetterRequest.setCreatedAt(null);
        notificationLetterRequest.setUpdatedAt(null);
        NotificationLetterRequest savedRequest = requestRepository.save(notificationLetterRequest);

        assertNotNull(savedRequest.toString());
        assertNotNull(savedRequest.getId());
        assertNotNull(savedRequest.getCreatedAt());
        assertNotNull(savedRequest.getUpdatedAt());
    }

    @Test
    void When_RequestSaved_Expect_DataCanBeRetrievedById() {
        GovUkLetterDetailsRequest letterRequest = createSampleLetterRequest("456 Oak Ave");
        NotificationLetterRequest savedRequest = requestRepository.save(new NotificationLetterRequest(null, null, letterRequest, null));

        Optional<NotificationLetterRequest> retrievedRequest = requestRepository.findById(savedRequest.getId());

        assertTrue(retrievedRequest.isPresent());
        assertEquals(savedRequest.getId(), retrievedRequest.get().getId());
        assertEquals("456 Oak Ave", retrievedRequest.get().getRequest().getRecipientDetails().getPhysicalAddress().getAddressLine1());
    }

    @Test
    void When_MultipleRequestsSaved_Expect_AllCanBeRetrieved() {
        requestRepository.save(new NotificationLetterRequest(null, null, createSampleLetterRequest("123 First St"), null));
        requestRepository.save(new NotificationLetterRequest(null, null, createSampleLetterRequest("456 Second Ave"), null));
        requestRepository.save(new NotificationLetterRequest(null, null, createSampleLetterRequest("789 Third Blvd"), null));

        List<NotificationLetterRequest> allRequests = requestRepository.findAll();

        assertTrue(allRequests.size() >= 3);
    }

    @Test
    void When_RequestDeleted_Expect_RequestNotFoundById() {
        NotificationLetterRequest savedRequest = requestRepository.save(new NotificationLetterRequest(null, null, createSampleLetterRequest("Test Address"), null));

        requestRepository.deleteById(savedRequest.getId());

        Optional<NotificationLetterRequest> deletedRequest = requestRepository.findById(savedRequest.getId());
        assertFalse(deletedRequest.isPresent());
    }

    @Test
    void When_RequestUpdated_Expect_ChangesReflectedInDatabase() {
        GovUkLetterDetailsRequest initialRequest = createSampleLetterRequest("Initial Address");
        NotificationLetterRequest savedRequest = requestRepository.save(new NotificationLetterRequest(null, null, initialRequest, null));

        GovUkLetterDetailsRequest updatedRequest = createSampleLetterRequest("Updated Address");
        requestRepository.save(new NotificationLetterRequest(null, null, updatedRequest, savedRequest.getId()));

        NotificationLetterRequest retrievedRequest = requestRepository.findById(savedRequest.getId()).orElse(null);

        assertNotNull(retrievedRequest);
        assertEquals("Updated Address", retrievedRequest.getRequest().getRecipientDetails().getPhysicalAddress().getAddressLine1());
    }

    @Test
    @DisplayName("Pagination of letter requests sought by reference works as expected")
    void paginationOfLettersByReferenceWorksAsExpected() {

        saveLetterWithReference("Reference 1");
        saveLetterWithReference("Reference 2");
        saveLetterWithReference("Reference 3");

        var firstLetter = requestRepository.findByReference("Reference", PageRequest.of(0, 1));
        var secondLetter = requestRepository.findByReference("Reference", PageRequest.of(1, 1));
        var lastLetter = requestRepository.findByReference("Reference", PageRequest.of(2, 1));
        var noLetter = requestRepository.findByReference("Reference", PageRequest.of(3, 1));

        assertThat(firstLetter.stream().findFirst().isPresent(), is(true));
        assertThat(firstLetter.stream().
                findFirst().get().getRequest().getSenderDetails().getReference(),
                is("Reference 1"));
        assertThat(secondLetter.stream().findFirst().isPresent(), is(true));
        assertThat(secondLetter.stream().
                findFirst().get().getRequest().getSenderDetails().getReference(),
                is("Reference 2"));
        assertThat(lastLetter.stream().findFirst().isPresent(), is(true));
        assertThat(lastLetter.stream().
                findFirst().get().getRequest().getSenderDetails().getReference(),
                is("Reference 3"));

        assertThat(noLetter.stream().findFirst().isPresent(), is(false));

    }

    private void saveLetterWithReference(String reference) {
        var letter = createLetterWithReference(reference);
        requestRepository.save(new NotificationLetterRequest(null, null, letter, null));
    }
    
}
