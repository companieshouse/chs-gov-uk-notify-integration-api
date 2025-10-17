package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import java.util.Optional;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterResponse;
import uk.gov.service.notify.LetterResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration-test")
@SpringBootTest
class NotificationLetterResponseRepositoryTest extends AbstractMongoDBTest {
    
    @Autowired
    private NotificationLetterResponseRepository responseRepository;

    @Test
    void When_NewResponseSaved_Expect_IdAssigned() {
        LetterResponse letterResponse = createSampleLetterResponse(UUID.randomUUID());

        NotificationLetterResponse notificationLetterResponse = new NotificationLetterResponse();
        notificationLetterResponse.setCreatedAt(null);
        notificationLetterResponse.setUpdatedAt(null);
        notificationLetterResponse.setResponse(letterResponse);
        notificationLetterResponse.setId(null);

        NotificationLetterResponse savedResponse = responseRepository.save(notificationLetterResponse);

        assertNotNull(savedResponse.toString());
        assertNotNull(savedResponse.getId());
        assertNotNull(savedResponse.getCreatedAt());
        assertNotNull(savedResponse.getUpdatedAt());
    }

    @Test
    void When_ResponseSaved_Expect_DataCanBeRetrievedById() {
        UUID notificationId = UUID.randomUUID();
        LetterResponse letterResponse = createSampleLetterResponse(notificationId);

        NotificationLetterResponse savedResponse = responseRepository.save(
                new NotificationLetterResponse(null, null, letterResponse, null));

        Optional<NotificationLetterResponse> retrievedResponse = responseRepository.findById(savedResponse.getId());

        assertTrue(retrievedResponse.isPresent());
        assertEquals(savedResponse.getId(), retrievedResponse.get().getId());
        assertEquals(notificationId, retrievedResponse.get().getResponse().getNotificationId());
    }


    @Test
    void When_ResponseDeleted_Expect_ResponseNotFoundById() {
        NotificationLetterResponse savedResponse = responseRepository.save(
                new NotificationLetterResponse(null, null, createSampleLetterResponse(UUID.randomUUID()), null));

        responseRepository.deleteById(savedResponse.getId());

        Optional<NotificationLetterResponse> deletedResponse = responseRepository.findById(savedResponse.getId());
        assertFalse(deletedResponse.isPresent());
    }

    @Test
    void When_ResponseUpdated_Expect_ChangesReflectedInDatabase() {
        UUID initialNotificationId = UUID.randomUUID();
        UUID updatedNotificationId = UUID.randomUUID();

        NotificationLetterResponse savedResponse = responseRepository.save(
                new NotificationLetterResponse(null, null, createSampleLetterResponse(initialNotificationId), null));

        responseRepository.save(new NotificationLetterResponse(null, null,
                createSampleLetterResponse(updatedNotificationId), savedResponse.getId()));

        NotificationLetterResponse retrievedResponse = responseRepository.findById(savedResponse.getId()).orElse(null);

        assertNotNull(retrievedResponse);
        assertEquals(updatedNotificationId, retrievedResponse.getResponse().getNotificationId());
    }

    private LetterResponse createSampleLetterResponse(UUID notificationId) {
        JSONObject responseJson = new JSONObject()
                .put("id", notificationId.toString())
                .put("reference", "client-reference")
                .put("postage", "first");

        return new LetterResponse(responseJson.toString());
    }
}
