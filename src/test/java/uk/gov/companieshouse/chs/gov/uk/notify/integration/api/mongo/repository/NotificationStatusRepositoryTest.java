package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import static org.junit.jupiter.api.Assertions.*;

import org.json.JSONObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.EmailDetails;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.RecipientDetailsEmail;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.SenderDetails;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.config.MongoConfig;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.MongoTestExtension;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationResponse;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationStatus;
import uk.gov.service.notify.SendEmailResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@DataMongoTest
@Testcontainers
@Tag("integration-test")
@ExtendWith(MongoTestExtension.class)
@Import(MongoConfig.class)
public class NotificationStatusRepositoryTest {

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

    private NotificationEmailRequest createSampleNotificationRequest() {
        SenderDetails senderDetails = new SenderDetails("test-app-id", "test-reference");
        RecipientDetailsEmail recipientDetails = new RecipientDetailsEmail("Test User", "test@example.com");
        EmailDetails emailDetails = new EmailDetails("template-123", new BigDecimal("1.0"), "Hello {{name}}");

        GovUkEmailDetailsRequest emailRequest = new GovUkEmailDetailsRequest()
                .senderDetails(senderDetails)
                .recipientDetails(recipientDetails)
                .emailDetails(emailDetails)
                .createdAt(OffsetDateTime.now());

        return new NotificationEmailRequest(null, emailRequest);
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
                .put("template", templateJson);

        return new SendEmailResponse(responseJson.toString());
    }
}
