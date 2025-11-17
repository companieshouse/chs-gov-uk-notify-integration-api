package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createSampleEmailRequest;

@Tag("integration-test")
@SpringBootTest
class NotificationEmailRequestRepositoryTest extends AbstractMongoDBTest {
    
    @Autowired
    private NotificationEmailRequestRepository requestRepository;

    @Test
    void When_NewRequestSaved_Expect_IdAssigned() {
        GovUkEmailDetailsRequest emailRequest = createSampleEmailRequest("john.doe@example.com");
        NotificationEmailRequest notificationEmailRequest = new NotificationEmailRequest();
        notificationEmailRequest.setRequest(emailRequest);
        notificationEmailRequest.setId(null);
        notificationEmailRequest.setCreatedAt(null);
        notificationEmailRequest.setUpdatedAt(null);
        NotificationEmailRequest savedRequest = requestRepository.save(notificationEmailRequest);

        assertNotNull(savedRequest.toString());
        assertNotNull(savedRequest.getId());
        assertNotNull(savedRequest.getCreatedAt());
        assertNotNull(savedRequest.getUpdatedAt());
    }

    @Test
    void When_RequestSaved_Expect_DataCanBeRetrievedById() {
        GovUkEmailDetailsRequest emailRequest = createSampleEmailRequest("jane.doe@example.com");
        NotificationEmailRequest savedRequest = requestRepository.save(new NotificationEmailRequest(null, null, emailRequest, null));

        Optional<NotificationEmailRequest> retrievedRequest = requestRepository.findById(savedRequest.getId());

        assertTrue(retrievedRequest.isPresent());
        assertEquals(savedRequest.getId(), retrievedRequest.get().getId());
        assertEquals("jane.doe@example.com", retrievedRequest.get().getRequest().getRecipientDetails().getEmailAddress());
    }

    @Test
    void When_MultipleRequestsSaved_Expect_AllCanBeRetrieved() {
        requestRepository.save(new NotificationEmailRequest(null, null, createSampleEmailRequest("user1@example.com"), null));
        requestRepository.save(new NotificationEmailRequest(null, null, createSampleEmailRequest("user2@example.com"), null));
        requestRepository.save(new NotificationEmailRequest(null, null, createSampleEmailRequest("user3@example.com"), null));

        List<NotificationEmailRequest> allRequests = requestRepository.findAll();

        assertTrue(allRequests.size() >= 3);
    }

    @Test
    void When_RequestDeleted_Expect_RequestNotFoundById() {
        NotificationEmailRequest savedRequest = requestRepository.save(new NotificationEmailRequest(null, null, createSampleEmailRequest("test@example.com"), null));

        requestRepository.deleteById(savedRequest.getId());

        Optional<NotificationEmailRequest> deletedRequest = requestRepository.findById(savedRequest.getId());
        assertFalse(deletedRequest.isPresent());
    }

    @Test
    void When_RequestUpdated_Expect_ChangesReflectedInDatabase() {
        GovUkEmailDetailsRequest initialRequest = createSampleEmailRequest("initial@example.com");
        NotificationEmailRequest savedRequest = requestRepository.save(new NotificationEmailRequest(null, null, initialRequest, null));

        savedRequest.getRequest().getSenderDetails().setEmailAddress( "updated@example.com" );

        requestRepository.save(savedRequest);

        NotificationEmailRequest retrievedRequest = requestRepository.findById(savedRequest.getId()).orElse(null);

        assertNotNull(retrievedRequest);
        assertEquals("updated@example.com", retrievedRequest.getRequest().getSenderDetails().getEmailAddress());
    }
    
}
