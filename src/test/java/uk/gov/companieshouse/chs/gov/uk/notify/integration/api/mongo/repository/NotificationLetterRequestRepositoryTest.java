package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createLetterRequestWithAddressLine1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createLetterRequestWithReference;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationLetterRequest;

@SpringBootTest
class NotificationLetterRequestRepositoryTest extends AbstractMongoDBTest {

    private static final String NULL_LETTER_ID = null;

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

    @Test
    @DisplayName("Able to paginate through letter requests sought by selection criteria")
    void ableToPaginateThroughLettersSoughtBySelectionCriteria() {

        saveThreeLettersWithReferences();

        var firstLetter = findByPscNameOrLetterAndCompanyTemplateDate(LETTER_1);
        var secondLetter = findByPscNameOrLetterAndCompanyTemplateDate(LETTER_2);
        var lastLetter = findByPscNameOrLetterAndCompanyTemplateDate(LETTER_3);
        var noLetter = findByPscNameOrLetterAndCompanyTemplateDate(LETTER_4);

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
    @DisplayName("Not able to paginate through letter requests sought by selection criteria when none found")
    void notAbleToPaginateThroughLettersWhenNoneFoundBySelectionCriteria() {

        saveThreeLettersWithReferences();

        var firstLetter = findByPscNameOrLetterAndCompanyTemplateDateWithWrongTemplateId(
                PageRequest.of(0, 1));
        assertThat(firstLetter.isEmpty(), is(true));
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

    private Page<NotificationLetterRequest>
    findByPscNameOrLetterAndCompanyTemplateDate(Pageable letter) {
        return notificationLetterRequestRepository.findByPscNameOrLetterAndCompanyTemplateDate(
                "Joe Bloggs",
                "00006400",
                "IDVPSCDIRNEW",
                "v1.0",
                LocalDate.now().toString(),
                LocalDate.now().plusDays(1).toString(),
                letter);
    }

    private Page<NotificationLetterRequest>
    findByPscNameOrLetterAndCompanyTemplateDateWithWrongTemplateId(Pageable letter) {
        return notificationLetterRequestRepository.findByPscNameOrLetterAndCompanyTemplateDate(
                "Joe Bloggs",
                "00006400",
                NULL_LETTER_ID,
                "unknown_template",
                LocalDate.now().toString(),
                LocalDate.now().plusDays(1).toString(),
                letter);
    }

    private void saveThreeLettersWithReferences() {
        saveLetterWithReference("Reference 1");
        saveLetterWithReference("Reference 2");
        saveLetterWithReference("Reference 3");
    }

    private void saveLetterWithReference(String reference) {
        var letter = createLetterRequestWithReference(reference);
        letter.setCreatedAt(OffsetDateTime.now());
        notificationLetterRequestRepository.save(new NotificationLetterRequest(letter));
    }

    private LetterRequestDao saveLetterWithReference(String appId, String reference) {
        var letter = createLetterRequestWithReference(reference);
        letter.getSenderDetails().setAppId(appId);
        notificationLetterRequestRepository.save(new NotificationLetterRequest(letter));
        return letter;
    }

}
