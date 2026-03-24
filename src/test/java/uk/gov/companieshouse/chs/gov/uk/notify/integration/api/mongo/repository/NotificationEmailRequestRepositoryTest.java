package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createEmailRequest;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationEmailRequest;

@SpringBootTest
class NotificationEmailRequestRepositoryTest extends AbstractMongoDBTest {

    @Test
    void When_NewRequestSaved_Expect_IdAssigned() {
        EmailRequestDao emailRequest = createEmailRequest("john.doe@example.com");
        NotificationEmailRequest notificationEmailRequest = new NotificationEmailRequest();
        notificationEmailRequest.setRequest(emailRequest);
        notificationEmailRequest.setId(null);
        notificationEmailRequest.setCreatedAt(null);
        notificationEmailRequest.setUpdatedAt(null);
        NotificationEmailRequest savedRequest = notificationEmailRequestRepository.save(notificationEmailRequest);

        assertNotNull(savedRequest.toString());
        assertNotNull(savedRequest.getId());
        assertNotNull(savedRequest.getCreatedAt());
        assertNotNull(savedRequest.getUpdatedAt());
    }

    @Test
    void When_RequestSaved_Expect_DataCanBeRetrievedById() {
        EmailRequestDao emailRequest = createEmailRequest("jane.doe@example.com");
        NotificationEmailRequest savedRequest = notificationEmailRequestRepository.save(new NotificationEmailRequest(emailRequest));

        Optional<NotificationEmailRequest> retrievedRequest = notificationEmailRequestRepository.findById(savedRequest.getId());

        assertTrue(retrievedRequest.isPresent());
        assertEquals(savedRequest.getId(), retrievedRequest.get().getId());
        assertEquals("jane.doe@example.com", retrievedRequest.get().getRequest().getRecipientDetails().getEmailAddress());
    }

    @Test
    void When_MultipleRequestsSaved_Expect_AllCanBeRetrieved() {
        notificationEmailRequestRepository.save(new NotificationEmailRequest(createEmailRequest("user1@example.com")));
        notificationEmailRequestRepository.save(new NotificationEmailRequest(createEmailRequest("user2@example.com")));
        notificationEmailRequestRepository.save(new NotificationEmailRequest(createEmailRequest("user3@example.com")));

        List<NotificationEmailRequest> allRequests = notificationEmailRequestRepository.findAll();

        assertTrue(allRequests.size() >= 3);
    }

    @Test
    void When_RequestDeleted_Expect_RequestNotFoundById() {
        NotificationEmailRequest savedRequest = notificationEmailRequestRepository.save(new NotificationEmailRequest(createEmailRequest("test@example.com")));

        notificationEmailRequestRepository.deleteById(savedRequest.getId());

        Optional<NotificationEmailRequest> deletedRequest = notificationEmailRequestRepository.findById(savedRequest.getId());
        assertFalse(deletedRequest.isPresent());
    }

    @Test
    void When_RequestUpdated_Expect_ChangesReflectedInDatabase() {
        EmailRequestDao initialRequest = createEmailRequest("initial@example.com");
        NotificationEmailRequest savedRequest = notificationEmailRequestRepository.save(new NotificationEmailRequest(initialRequest));

        savedRequest.getRequest().getSenderDetails().setEmailAddress( "updated@example.com" );

        notificationEmailRequestRepository.save(savedRequest);

        NotificationEmailRequest retrievedRequest = notificationEmailRequestRepository.findById(savedRequest.getId()).orElse(null);

        assertNotNull(retrievedRequest);
        assertEquals("updated@example.com", retrievedRequest.getRequest().getSenderDetails().getEmailAddress());
    }


    @Test
    void findByUniqueReference() {
        String appId = "chips";
        String otherAppId = "other-app";
        String reference = "TEST";
        var email1 = saveEmailWithReference(appId, reference);
        saveEmailWithReference(otherAppId, reference);

        var result = notificationEmailRequestRepository.findByUniqueReference(appId, reference);

        assertNotNull(result);
        assertTrue(result.isPresent());
        assertNotEquals(otherAppId, result.get().getRequest().getSenderDetails().getAppId());
        assertEquals(email1.getEmailDetails(), result.get().getRequest().getEmailDetails());
        assertEquals(email1.getRecipientDetails(), result.get().getRequest().getRecipientDetails());
        assertEquals(email1.getSenderDetails(), result.get().getRequest().getSenderDetails());
    }

    @Test
    void findByUniqueReference_notFound() {
        String reference = "TEST";
        saveEmailWithReference("chips", reference);
        saveEmailWithReference("other-app", reference);

        var result = notificationEmailRequestRepository.findByUniqueReference("app-id", reference);

        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    private EmailRequestDao saveEmailWithReference(String appId, String reference) {
        var email = createEmailRequest();
        email.getSenderDetails().setAppId(appId);
        email.getSenderDetails().setReference(reference);
        notificationEmailRequestRepository.save(new NotificationEmailRequest(email));
        return email;
    }
}
