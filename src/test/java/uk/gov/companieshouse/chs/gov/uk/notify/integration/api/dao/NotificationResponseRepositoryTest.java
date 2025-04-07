package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.dao;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.EmailDetails;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.RecipientDetailsEmail;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.SenderDetails;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.SharedMongoContainer;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.NotificationResponse;
import uk.gov.service.notify.SendEmailResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NotificationResponseRepositoryTest {

    static {
        SharedMongoContainer.getInstance();
    }
    
    @Autowired
    private NotificationEmailRequestRepository requestRepository;

    @Autowired
    private NotificationResponseRepository responseRepository;

    @Test
    public void When_NewResponseSaved_Expect_IdAssigned() {
        NotificationEmailRequest savedRequest = requestRepository.save(createSampleNotificationRequest());
        SendEmailResponse emailResponse = createSampleEmailResponse(UUID.randomUUID(), UUID.randomUUID());

        NotificationResponse savedResponse = responseRepository.save(
                new NotificationResponse(null, savedRequest.id(), emailResponse));

        assertNotNull(savedResponse);
        assertNotNull(savedResponse.id());
    }

    @Test
    public void When_ResponseSaved_Expect_DataCanBeRetrievedById() {
        NotificationEmailRequest savedRequest = requestRepository.save(createSampleNotificationRequest());
        UUID notificationId = UUID.randomUUID();
        SendEmailResponse emailResponse = createSampleEmailResponse(UUID.randomUUID(), notificationId);

        NotificationResponse savedResponse = responseRepository.save(
                new NotificationResponse(null, savedRequest.id(), emailResponse));

        Optional<NotificationResponse> retrievedResponse = responseRepository.findById(savedResponse.id());

        assertTrue(retrievedResponse.isPresent());
        assertEquals(savedResponse.id(), retrievedResponse.get().id());
        assertEquals(notificationId, retrievedResponse.get().response().getNotificationId());
    }

    @Test
    public void When_ResponseSaved_Expect_CanBeFoundByRequestId() {
        NotificationEmailRequest savedRequest = requestRepository.save(createSampleNotificationRequest());
        NotificationResponse savedResponse = responseRepository.save(
                new NotificationResponse(null, savedRequest.id(),
                        createSampleEmailResponse(UUID.randomUUID(), UUID.randomUUID())));

        List<NotificationResponse> responses = responseRepository.findByRequestId(savedRequest.id());

        assertEquals(1, responses.size());
        assertEquals(savedResponse.id(), responses.getFirst().id());
    }

    @Test
    public void When_MultipleResponsesForSameRequest_Expect_AllResponsesReturned() {
        NotificationEmailRequest savedRequest = requestRepository.save(createSampleNotificationRequest());

        responseRepository.save(new NotificationResponse(null, savedRequest.id(),
                createSampleEmailResponse(UUID.randomUUID(), UUID.randomUUID())));
        responseRepository.save(new NotificationResponse(null, savedRequest.id(),
                createSampleEmailResponse(UUID.randomUUID(), UUID.randomUUID())));

        List<NotificationResponse> responses = responseRepository.findByRequestId(savedRequest.id());

        assertEquals(2, responses.size());
    }

    @Test
    public void When_ResponseDeleted_Expect_ResponseNotFoundById() {
        NotificationEmailRequest savedRequest = requestRepository.save(createSampleNotificationRequest());
        NotificationResponse savedResponse = responseRepository.save(
                new NotificationResponse(null, savedRequest.id(),
                        createSampleEmailResponse(UUID.randomUUID(), UUID.randomUUID())));

        responseRepository.deleteById(savedResponse.id());

        Optional<NotificationResponse> deletedResponse = responseRepository.findById(savedResponse.id());
        assertFalse(deletedResponse.isPresent());
    }

    @Test
    public void When_ResponseUpdated_Expect_ChangesReflectedInDatabase() {
        NotificationEmailRequest savedRequest = requestRepository.save(createSampleNotificationRequest());
        UUID initialNotificationId = UUID.randomUUID();
        UUID updatedNotificationId = UUID.randomUUID();

        NotificationResponse savedResponse = responseRepository.save(
                new NotificationResponse(null, savedRequest.id(),
                        createSampleEmailResponse(UUID.randomUUID(), initialNotificationId)));

        responseRepository.save(new NotificationResponse(savedResponse.id(), savedRequest.id(),
                createSampleEmailResponse(UUID.randomUUID(), updatedNotificationId)));

        NotificationResponse retrievedResponse = responseRepository.findById(savedResponse.id()).orElse(null);

        assertNotNull(retrievedResponse);
        assertEquals(updatedNotificationId, retrievedResponse.response().getNotificationId());
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
