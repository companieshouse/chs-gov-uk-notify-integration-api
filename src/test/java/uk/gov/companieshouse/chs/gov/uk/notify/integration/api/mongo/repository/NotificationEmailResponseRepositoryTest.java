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
                new NotificationEmailResponse(null, null, null, null));

        assertNotNull(savedResponse);
        assertNotNull(savedResponse.getId());
    }

    @Test
    void When_ResponseSaved_Expect_DataCanBeRetrievedById() {
        UUID notificationId = UUID.randomUUID();
        SendEmailResponse emailResponse = createSampleEmailResponse(UUID.randomUUID(), notificationId);

        NotificationEmailResponse savedResponse = responseRepository.save(
                new NotificationEmailResponse(null, null, null, null));

        Optional<NotificationEmailResponse> retrievedResponse = responseRepository.findById(savedResponse.getId());

        assertTrue(retrievedResponse.isPresent());
        assertEquals(savedResponse.getId(), retrievedResponse.get().getId());
        assertEquals(notificationId, retrievedResponse.get().getResponse().getNotificationId());
    }


    @Test
    void When_ResponseDeleted_Expect_ResponseNotFoundById() {
        NotificationEmailResponse savedResponse = responseRepository.save(
                new NotificationEmailResponse(null, null, createSampleEmailResponse(UUID.randomUUID(),UUID.randomUUID()), null ));

        responseRepository.deleteById(savedResponse.getId());

        Optional<NotificationEmailResponse> deletedResponse = responseRepository.findById(savedResponse.getId());
        assertFalse(deletedResponse.isPresent());
    }

    @Test
    void When_ResponseUpdated_Expect_ChangesReflectedInDatabase() {
        UUID initialNotificationId = UUID.randomUUID();
        UUID updatedNotificationId = UUID.randomUUID();

        NotificationEmailResponse savedResponse = responseRepository.save(
                new NotificationEmailResponse(null, null, createSampleEmailResponse(UUID.randomUUID(),initialNotificationId), null));

        responseRepository.save(new NotificationEmailResponse(null, null, createSampleEmailResponse(UUID.randomUUID(),updatedNotificationId), savedResponse.getId()));

        NotificationEmailResponse retrievedResponse = responseRepository.findById(savedResponse.getId()).orElse(null);

        assertNotNull(retrievedResponse);
        assertEquals(updatedNotificationId, retrievedResponse.getResponse().getNotificationId());
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
