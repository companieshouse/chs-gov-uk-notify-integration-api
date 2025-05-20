package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailResponse;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createSampleEmailResponse;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.createSampleNotificationRequest;

@SpringBootTest
class NotificationStatusRepositoryTest extends AbstractMongoDBTest {

    @Autowired
    private NotificationEmailRequestRepository requestRepository;

    @Autowired
    private NotificationEmailResponseRepository responseRepository;

    @Autowired
    private NotificationStatusRepository statusRepository;

    @Test
    void When_NewStatusSaved_Expect_IdAssigned() {
        TestData testData = setupTestDataWithRequestAndResponse();

        NotificationStatus notificationStatus = new NotificationStatus();
        notificationStatus.setCreatedAt(null);
        notificationStatus.setUpdatedAt(null);
        notificationStatus.setRequestId(testData.requestId);
        notificationStatus.setResponseId(testData.responseId);
        notificationStatus.setStatus("SENT");
        notificationStatus.setStatusDetails(Map.of("sentAt", "2023-03-15T14:30:00Z"));
        notificationStatus.setId(null);


        NotificationStatus savedStatus = statusRepository.save(notificationStatus);

        assertNotNull(savedStatus.toString());
        assertNotNull(savedStatus.getId());
        assertNotNull(savedStatus.getCreatedAt());
        assertNotNull(savedStatus.getUpdatedAt());
    }

    @Test
    void When_StatusSaved_Expect_DataCanBeRetrievedById() {
        TestData testData = setupTestDataWithRequestAndResponse();

        NotificationStatus status = new NotificationStatus(
                null,
                null,
                testData.requestId,
                testData.responseId,
                "DELIVERED",
                Map.of("deliveredAt", "2023-03-15T14:32:00Z"),
                null
        );

        NotificationStatus savedStatus = statusRepository.save(status);

        Optional<NotificationStatus> retrievedStatus = statusRepository.findById(savedStatus.getId());

        assertTrue(retrievedStatus.isPresent());
        assertEquals(savedStatus.getId(), retrievedStatus.get().getId());
        assertEquals("DELIVERED", retrievedStatus.get().getStatus());
    }

    @Test
    void When_StatusSaved_Expect_CanBeFoundByRequestId() {
        TestData testData = setupTestDataWithRequestAndResponse();

        NotificationStatus status = new NotificationStatus(
                null,
                null,
                testData.requestId,
                testData.responseId,
                "SENT",
                Map.of("sentAt", "2023-03-15T14:30:00Z"),
                null
        );

        statusRepository.save(status);

        List<NotificationStatus> statuses = statusRepository.findByRequestId(testData.requestId);

        assertEquals(1, statuses.size());
        assertEquals("SENT", statuses.getFirst().getStatus());
    }

    @Test
    void When_StatusSaved_Expect_CanBeFoundByResponseId() {
        TestData testData = setupTestDataWithRequestAndResponse();

        NotificationStatus status = new NotificationStatus(
                null,
                null,
                testData.requestId,
                testData.responseId,
                "SENT",
                Map.of("sentAt", "2023-03-15T14:30:00Z"),
                null
        );

        statusRepository.save(status);

        List<NotificationStatus> statuses = statusRepository.findByResponseId(testData.responseId);

        assertEquals(1, statuses.size());
        assertEquals("SENT", statuses.getFirst().getStatus());
    }

    @Test
    void When_MultipleStatusesForSameResponse_Expect_AllStatusesReturned() {
        TestData testData = setupTestDataWithRequestAndResponse();

        statusRepository.save(new NotificationStatus(
                null, null, testData.requestId, testData.responseId, "SENT",
                Map.of("sentAt", "2023-03-15T14:30:00Z"), null));
        statusRepository.save(new NotificationStatus(
                null, null, testData.requestId, testData.responseId, "DELIVERED",
                Map.of("deliveredAt", "2023-03-15T14:32:00Z"), null));

        List<NotificationStatus> statuses = statusRepository.findByResponseId(testData.responseId);

        assertEquals(2, statuses.size());
    }

    @Test
    void When_StatusDeleted_Expect_StatusNotFoundById() {
        TestData testData = setupTestDataWithRequestAndResponse();

        NotificationStatus savedStatus = statusRepository.save(new NotificationStatus(
                null, null, testData.requestId, testData.responseId, "SENT",
                Map.of("sentAt", "2023-03-15T14:30:00Z"), null));

        statusRepository.deleteById(savedStatus.getId());

        Optional<NotificationStatus> deletedStatus = statusRepository.findById(savedStatus.getId());
        assertFalse(deletedStatus.isPresent());
    }

    @Test
    void When_StatusUpdated_Expect_ChangesReflectedInDatabase() {
        TestData testData = setupTestDataWithRequestAndResponse();

        NotificationStatus savedStatus = statusRepository.save(new NotificationStatus(
                null, null, testData.requestId, testData.responseId, "SENT",
                Map.of("sentAt", "2023-03-15T14:30:00Z"), null));

        statusRepository.save(new NotificationStatus(
                null, null, testData.requestId, testData.responseId, "FAILED",
                Map.of("failedAt", "2023-03-15T14:35:00Z", "reason", "Invalid email"), savedStatus.getId()));

        NotificationStatus retrievedStatus = statusRepository.findById(savedStatus.getId()).orElse(null);

        assertNotNull(retrievedStatus);
        assertEquals("FAILED", retrievedStatus.getStatus());
        assertEquals("Invalid email", retrievedStatus.getStatusDetails().get("reason"));
    }

    private TestData setupTestDataWithRequestAndResponse() {
        NotificationEmailRequest savedRequest = requestRepository.save(createSampleNotificationRequest());

        NotificationEmailResponse savedResponse = responseRepository.save(
                new NotificationEmailResponse(null, null, createSampleEmailResponse(), null)
        );

        return new TestData(savedRequest.getId(), savedResponse.getId());
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
