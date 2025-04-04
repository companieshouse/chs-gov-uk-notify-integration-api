package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.TestUtils.createSampleLetterRequest;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.Address;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.LetterDetails;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.RecipientDetailsLetter;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.SenderDetails;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.config.MongoConfig;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.MongoTestExtension;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@DataMongoTest
@Testcontainers
@Tag("integration-test")
@ExtendWith(MongoTestExtension.class)
@Import(MongoConfig.class)
public class NotificationLetterRequestRepositoryTest {

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
