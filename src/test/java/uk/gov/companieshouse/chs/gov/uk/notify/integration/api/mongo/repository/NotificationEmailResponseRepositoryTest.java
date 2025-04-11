package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import java.util.Optional;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailResponse;
import uk.gov.service.notify.SendEmailResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class NotificationEmailResponseRepositoryTest extends AbstractMongoDBTest {
    
    @Autowired
    private NotificationEmailResponseRepository responseRepository;

    @Test
    void When_NewResponseSaved_Expect_IdAssigned() {
        SendEmailResponse emailResponse = createSampleEmailResponse(UUID.randomUUID(), UUID.randomUUID());

        NotificationEmailResponse savedResponse = responseRepository.save(
                new NotificationEmailResponse(null, emailResponse));

        assertNotNull(savedResponse);
        assertNotNull(savedResponse.id());
    }

    @Test
    void When_ResponseSaved_Expect_DataCanBeRetrievedById() {
        UUID notificationId = UUID.randomUUID();
        SendEmailResponse emailResponse = createSampleEmailResponse(UUID.randomUUID(), notificationId);

        NotificationEmailResponse savedResponse = responseRepository.save(
                new NotificationEmailResponse(null, emailResponse));

        Optional<NotificationEmailResponse> retrievedResponse = responseRepository.findById(savedResponse.id());

        assertTrue(retrievedResponse.isPresent());
        assertEquals(savedResponse.id(), retrievedResponse.get().id());
        assertEquals(notificationId, retrievedResponse.get().response().getNotificationId());
    }


    @Test
    void When_ResponseDeleted_Expect_ResponseNotFoundById() {
        NotificationEmailResponse savedResponse = responseRepository.save(
                new NotificationEmailResponse(null, createSampleEmailResponse(UUID.randomUUID(), UUID.randomUUID())));

        responseRepository.deleteById(savedResponse.id());

        Optional<NotificationEmailResponse> deletedResponse = responseRepository.findById(savedResponse.id());
        assertFalse(deletedResponse.isPresent());
    }

    @Test
    void When_ResponseUpdated_Expect_ChangesReflectedInDatabase() {
        UUID initialNotificationId = UUID.randomUUID();
        UUID updatedNotificationId = UUID.randomUUID();

        NotificationEmailResponse savedResponse = responseRepository.save(
                new NotificationEmailResponse(null, createSampleEmailResponse(UUID.randomUUID(), initialNotificationId)));

        responseRepository.save(new NotificationEmailResponse(savedResponse.id(),
                createSampleEmailResponse(UUID.randomUUID(), updatedNotificationId)));

        NotificationEmailResponse retrievedResponse = responseRepository.findById(savedResponse.id()).orElse(null);

        assertNotNull(retrievedResponse);
        assertEquals(updatedNotificationId, retrievedResponse.response().getNotificationId());
    }

    private SendEmailResponse createSampleEmailResponse(UUID templateId, UUID notificationId) {
        JSONObject templateJson = new JSONObject()
                .put("id", templateId.toString())
                .put("version", 1)
                .put("uri", "https://api.notifications.service.gov.uk/v2/template/abcdefg");

        JSONObject contentJson = new JSONObject()
                .put("body", "Hello World")
                .put("from_email", "service@example.com")
                .put("subject", "Test Email");

        JSONObject responseJson = new JSONObject()
                .put("id", notificationId.toString())
                .put("reference", "client-reference")
                .put("content", contentJson)
                .put("oneClickUnsubscribeURL", "www.example.com")
                .put("template", templateJson);

        return new SendEmailResponse(responseJson.toString());
    }
}
