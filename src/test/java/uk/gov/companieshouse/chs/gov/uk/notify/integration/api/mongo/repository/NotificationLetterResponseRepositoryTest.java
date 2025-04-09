package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import java.util.Optional;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.SharedMongoContainer;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterResponse;
import uk.gov.service.notify.LetterResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NotificationLetterResponseRepositoryTest {

    static {
        SharedMongoContainer.getInstance();
    }

    @Autowired
    private NotificationLetterResponseRepository responseRepository;

    @Test
    public void When_NewResponseSaved_Expect_IdAssigned() {
        LetterResponse letterResponse = createSampleLetterResponse(UUID.randomUUID());

        NotificationLetterResponse savedResponse = responseRepository.save(new NotificationLetterResponse(null, letterResponse));

        assertNotNull(savedResponse);
        assertNotNull(savedResponse.id());
    }

    @Test
    public void When_ResponseSaved_Expect_DataCanBeRetrievedById() {
        UUID notificationId = UUID.randomUUID();
        LetterResponse letterResponse = createSampleLetterResponse(notificationId);

        NotificationLetterResponse savedResponse = responseRepository.save(
                new NotificationLetterResponse(null, letterResponse));

        Optional<NotificationLetterResponse> retrievedResponse = responseRepository.findById(savedResponse.id());

        assertTrue(retrievedResponse.isPresent());
        assertEquals(savedResponse.id(), retrievedResponse.get().id());
        assertEquals(notificationId, retrievedResponse.get().response().getNotificationId());
    }


    @Test
    public void When_ResponseDeleted_Expect_ResponseNotFoundById() {
        NotificationLetterResponse savedResponse = responseRepository.save(
                new NotificationLetterResponse(null, createSampleLetterResponse(UUID.randomUUID())));

        responseRepository.deleteById(savedResponse.id());

        Optional<NotificationLetterResponse> deletedResponse = responseRepository.findById(savedResponse.id());
        assertFalse(deletedResponse.isPresent());
    }

    @Test
    public void When_ResponseUpdated_Expect_ChangesReflectedInDatabase() {
        UUID initialNotificationId = UUID.randomUUID();
        UUID updatedNotificationId = UUID.randomUUID();

        NotificationLetterResponse savedResponse = responseRepository.save(
                new NotificationLetterResponse(null, createSampleLetterResponse(initialNotificationId)));

        responseRepository.save(new NotificationLetterResponse(savedResponse.id(),
                createSampleLetterResponse(updatedNotificationId)));

        NotificationLetterResponse retrievedResponse = responseRepository.findById(savedResponse.id()).orElse(null);

        assertNotNull(retrievedResponse);
        assertEquals(updatedNotificationId, retrievedResponse.response().getNotificationId());
    }

    private LetterResponse createSampleLetterResponse(UUID notificationId) {
        JSONObject responseJson = new JSONObject()
                .put("id", notificationId.toString())
                .put("reference", "client-reference")
                .put("postage", "first");

        return new LetterResponse(responseJson.toString());
    }
}
