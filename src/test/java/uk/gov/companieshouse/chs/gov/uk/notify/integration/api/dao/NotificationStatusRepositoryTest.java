package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.SharedMongoContainer;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.NotificationResponse;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.NotificationStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createSampleEmailResponse;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createSampleNotificationRequest;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NotificationStatusRepositoryTest {

    static {
        SharedMongoContainer.getInstance();
    }
    
    @Autowired
    private NotificationEmailRequestRepository requestRepository;

    @Autowired
    private NotificationResponseRepository responseRepository;

    @Autowired
    private NotificationStatusRepository statusRepository;

    @Test
    public void When_NewStatusSaved_Expect_IdAssigned() {
        TestData testData = setupTestDataWithRequestAndResponse();

        NotificationStatus status = new NotificationStatus(
                null,
                testData.requestId,
                testData.responseId,
                "SENT",
                Map.of("sentAt", "2023-03-15T14:30:00Z")
        );

        NotificationStatus savedStatus = statusRepository.save(status);

        assertNotNull(savedStatus);
        assertNotNull(savedStatus.id());
    }

    @Test
    public void When_StatusSaved_Expect_DataCanBeRetrievedById() {
        TestData testData = setupTestDataWithRequestAndResponse();

        NotificationStatus status = new NotificationStatus(
                null,
                testData.requestId,
                testData.responseId,
                "DELIVERED",
                Map.of("deliveredAt", "2023-03-15T14:32:00Z")
        );

        NotificationStatus savedStatus = statusRepository.save(status);

        Optional<NotificationStatus> retrievedStatus = statusRepository.findById(savedStatus.id());

        assertTrue(retrievedStatus.isPresent());
        assertEquals(savedStatus.id(), retrievedStatus.get().id());
        assertEquals("DELIVERED", retrievedStatus.get().status());
    }

    @Test
    public void When_StatusSaved_Expect_CanBeFoundByRequestId() {
        TestData testData = setupTestDataWithRequestAndResponse();

        NotificationStatus status = new NotificationStatus(
                null,
                testData.requestId,
                testData.responseId,
                "SENT",
                Map.of("sentAt", "2023-03-15T14:30:00Z")
        );

        statusRepository.save(status);

        List<NotificationStatus> statuses = statusRepository.findByRequestId(testData.requestId);

        assertEquals(1, statuses.size());
        assertEquals("SENT", statuses.getFirst().status());
    }

    @Test
    public void When_StatusSaved_Expect_CanBeFoundByResponseId() {
        TestData testData = setupTestDataWithRequestAndResponse();

        NotificationStatus status = new NotificationStatus(
                null,
                testData.requestId,
                testData.responseId,
                "SENT",
                Map.of("sentAt", "2023-03-15T14:30:00Z")
        );

        statusRepository.save(status);

        List<NotificationStatus> statuses = statusRepository.findByResponseId(testData.responseId);

        assertEquals(1, statuses.size());
        assertEquals("SENT", statuses.getFirst().status());
    }

    @Test
    public void When_MultipleStatusesForSameResponse_Expect_AllStatusesReturned() {
        TestData testData = setupTestDataWithRequestAndResponse();

        statusRepository.save(new NotificationStatus(
                null, testData.requestId, testData.responseId, "SENT",
                Map.of("sentAt", "2023-03-15T14:30:00Z")));
        statusRepository.save(new NotificationStatus(
                null, testData.requestId, testData.responseId, "DELIVERED",
                Map.of("deliveredAt", "2023-03-15T14:32:00Z")));

        List<NotificationStatus> statuses = statusRepository.findByResponseId(testData.responseId);

        assertEquals(2, statuses.size());
    }

    @Test
    public void When_StatusDeleted_Expect_StatusNotFoundById() {
        TestData testData = setupTestDataWithRequestAndResponse();

        NotificationStatus savedStatus = statusRepository.save(new NotificationStatus(
                null, testData.requestId, testData.responseId, "SENT",
                Map.of("sentAt", "2023-03-15T14:30:00Z")));

        statusRepository.deleteById(savedStatus.id());

        Optional<NotificationStatus> deletedStatus = statusRepository.findById(savedStatus.id());
        assertFalse(deletedStatus.isPresent());
    }

    @Test
    public void When_StatusUpdated_Expect_ChangesReflectedInDatabase() {
        TestData testData = setupTestDataWithRequestAndResponse();

        NotificationStatus savedStatus = statusRepository.save(new NotificationStatus(
                null, testData.requestId, testData.responseId, "SENT",
                Map.of("sentAt", "2023-03-15T14:30:00Z")));

        statusRepository.save(new NotificationStatus(
                savedStatus.id(), testData.requestId, testData.responseId, "FAILED",
                Map.of("failedAt", "2023-03-15T14:35:00Z", "reason", "Invalid email")));

        NotificationStatus retrievedStatus = statusRepository.findById(savedStatus.id()).orElse(null);

        assertNotNull(retrievedStatus);
        assertEquals("FAILED", retrievedStatus.status());
        assertEquals("Invalid email", retrievedStatus.statusDetails().get("reason"));
    }

    private TestData setupTestDataWithRequestAndResponse() {
        NotificationEmailRequest savedRequest = requestRepository.save(createSampleNotificationRequest());

        NotificationResponse savedResponse = responseRepository.save(
                new NotificationResponse(null, savedRequest.id(),
                        createSampleEmailResponse(UUID.randomUUID(), UUID.randomUUID())));

        return new TestData(savedRequest.id(), savedResponse.id());
    }

    private static class TestData {
        final String requestId;
        final String responseId;

        TestData(String requestId, String responseId) {
            this.requestId = requestId;
            this.responseId = responseId;
        }
    }
    
}
