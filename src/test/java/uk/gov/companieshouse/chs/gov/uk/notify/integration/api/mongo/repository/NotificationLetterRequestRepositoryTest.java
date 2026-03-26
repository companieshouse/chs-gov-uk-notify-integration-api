package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createLetterRequestWithAddressLine1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createLetterRequestWithReference;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterRequestDao;

@SpringBootTest
class NotificationLetterRequestRepositoryTest extends AbstractMongoDBTest {

    private static final Pageable LETTER_1 = PageRequest.of(0, 1);
    private static final Pageable LETTER_2 = PageRequest.of(1, 1);
    private static final Pageable LETTER_3 = PageRequest.of(2, 1);
    private static final Pageable LETTER_4 = PageRequest.of(3, 1);

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
        NotificationLetterRequest savedRequest = notificationLetterRequestRepository.save(new NotificationLetterRequest(null, null, letterRequest, null));

        Optional<NotificationLetterRequest> retrievedRequest = notificationLetterRequestRepository.findById(savedRequest.getId());

        assertTrue(retrievedRequest.isPresent());
        assertEquals(savedRequest.getId(), retrievedRequest.get().getId());
        assertEquals("456 Oak Ave", retrievedRequest.get().getRequest().getRecipientDetails().getPhysicalAddress().getAddressLine1());
    }

    @Test
    void When_MultipleRequestsSaved_Expect_AllCanBeRetrieved() {
        notificationLetterRequestRepository.save(new NotificationLetterRequest(null, null, createLetterRequestWithAddressLine1("123 First St"), null));
        notificationLetterRequestRepository.save(new NotificationLetterRequest(null, null, createLetterRequestWithAddressLine1("456 Second Ave"), null));
        notificationLetterRequestRepository.save(new NotificationLetterRequest(null, null, createLetterRequestWithAddressLine1("789 Third Blvd"), null));

        List<NotificationLetterRequest> allRequests = notificationLetterRequestRepository.findAll();

        assertTrue(allRequests.size() >= 3);
    }

    @Test
    void When_RequestDeleted_Expect_RequestNotFoundById() {
        NotificationLetterRequest savedRequest = notificationLetterRequestRepository.save(new NotificationLetterRequest(null, null, createLetterRequestWithAddressLine1("Test Address"), null));

        notificationLetterRequestRepository.deleteById(savedRequest.getId());

        Optional<NotificationLetterRequest> deletedRequest = notificationLetterRequestRepository.findById(savedRequest.getId());
        assertFalse(deletedRequest.isPresent());
    }

    @Test
    void When_RequestUpdated_Expect_ChangesReflectedInDatabase() {
        LetterRequestDao initialRequest = createLetterRequestWithAddressLine1("Initial Address");
        NotificationLetterRequest savedRequest = notificationLetterRequestRepository.save(new NotificationLetterRequest(null, null, initialRequest, null));

        savedRequest.getRequest().getRecipientDetails().getPhysicalAddress().setAddressLine1( "Updated Address" );

        notificationLetterRequestRepository.save(savedRequest);

        NotificationLetterRequest retrievedRequest = notificationLetterRequestRepository.findById(savedRequest.getId()).orElse(null);

        assertNotNull(retrievedRequest);
        assertEquals("Updated Address", retrievedRequest.getRequest().getRecipientDetails().getPhysicalAddress().getAddressLine1());
    }

    @Test
    @DisplayName("Able to paginate through letter requests sought by reference")
    void ableToPaginateThroughLettersSoughtByReference() {

        saveLetterWithReference("Reference 1");
        saveLetterWithReference("Reference 2");
        saveLetterWithReference("Reference 3");

        var firstLetter = notificationLetterRequestRepository.findByReference("Reference", LETTER_1);
        var secondLetter = notificationLetterRequestRepository.findByReference("Reference", LETTER_2);
        var lastLetter = notificationLetterRequestRepository.findByReference("Reference", LETTER_3);
        var noLetter = notificationLetterRequestRepository.findByReference("Reference", LETTER_4);

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

    @Test
    @DisplayName("Not able to paginate through letter requests sought by reference when none found")
    void notAbleToPaginateThroughLettersWhenNoneFoundByReference() {

        saveThreeLettersWithReferences();

        var firstLetter = notificationLetterRequestRepository.findByReference("Not The Reference", LETTER_1);
        assertThat(firstLetter.isEmpty(), is(true));
    }

    private void saveThreeLettersWithReferences() {
        saveLetterWithReference("Reference 1");
        saveLetterWithReference("Reference 2");
        saveLetterWithReference("Reference 3");
    }

    private void saveLetterWithReference(String reference) {
        var letter = createLetterRequestWithReference(reference);
        letter.setCreatedAt(OffsetDateTime.now());
        notificationLetterRequestRepository.save(new NotificationLetterRequest(null, null, letter, null));
    }

    private LetterRequestDao saveLetterWithReference(String appId, String reference) {
        var letter = createLetterRequestWithReference(reference);
        letter.getSenderDetails().setAppId(appId);
        notificationLetterRequestRepository.save(new NotificationLetterRequest(null, null, letter, null));
        return letter;
    }

}
