package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.SharedMongoContainer;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.TestUtils.createSampleEmailRequest;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NotificationEmailRequestRepositoryTest {

    static {
        SharedMongoContainer.getInstance();
    }
    
    @Autowired
    private NotificationEmailRequestRepository requestRepository;

    @Test
    public void When_NewRequestSaved_Expect_IdAssigned() {
        GovUkEmailDetailsRequest emailRequest = createSampleEmailRequest("john.doe@example.com");
        NotificationEmailRequest savedRequest = requestRepository.save(new NotificationEmailRequest(null, emailRequest));

        assertNotNull(savedRequest);
        assertNotNull(savedRequest.id());
    }

    @Test
    public void When_RequestSaved_Expect_DataCanBeRetrievedById() {
        GovUkEmailDetailsRequest emailRequest = createSampleEmailRequest("jane.doe@example.com");
        NotificationEmailRequest savedRequest = requestRepository.save(new NotificationEmailRequest(null, emailRequest));

        Optional<NotificationEmailRequest> retrievedRequest = requestRepository.findById(savedRequest.id());

        assertTrue(retrievedRequest.isPresent());
        assertEquals(savedRequest.id(), retrievedRequest.get().id());
        assertEquals("jane.doe@example.com", retrievedRequest.get().request().getRecipientDetails().getEmailAddress());
    }

    @Test
    public void When_MultipleRequestsSaved_Expect_AllCanBeRetrieved() {
        requestRepository.save(new NotificationEmailRequest(null, createSampleEmailRequest("user1@example.com")));
        requestRepository.save(new NotificationEmailRequest(null, createSampleEmailRequest("user2@example.com")));
        requestRepository.save(new NotificationEmailRequest(null, createSampleEmailRequest("user3@example.com")));

        List<NotificationEmailRequest> allRequests = requestRepository.findAll();

        assertTrue(allRequests.size() >= 3);
    }

    @Test
    public void When_RequestDeleted_Expect_RequestNotFoundById() {
        NotificationEmailRequest savedRequest = requestRepository.save(new NotificationEmailRequest(null, createSampleEmailRequest("test@example.com")));

        requestRepository.deleteById(savedRequest.id());

        Optional<NotificationEmailRequest> deletedRequest = requestRepository.findById(savedRequest.id());
        assertFalse(deletedRequest.isPresent());
    }

    @Test
    public void When_RequestUpdated_Expect_ChangesReflectedInDatabase() {
        GovUkEmailDetailsRequest initialRequest = createSampleEmailRequest("initial@example.com");
        NotificationEmailRequest savedRequest = requestRepository.save(new NotificationEmailRequest(null, initialRequest));

        GovUkEmailDetailsRequest updatedRequest = createSampleEmailRequest("updated@example.com");
        requestRepository.save(new NotificationEmailRequest(savedRequest.id(), updatedRequest));

        NotificationEmailRequest retrievedRequest = requestRepository.findById(savedRequest.id()).orElse(null);

        assertNotNull(retrievedRequest);
        assertEquals("updated@example.com", retrievedRequest.request().getRecipientDetails().getEmailAddress());
    }
    
}
