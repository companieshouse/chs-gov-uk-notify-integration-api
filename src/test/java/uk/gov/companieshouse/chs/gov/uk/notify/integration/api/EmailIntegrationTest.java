package uk.gov.companieshouse.chs.gov.uk.notify.integration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.postEmailRequestEntity;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDaoBuilder.emailRequestDaoBuilder;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.gov.companieshouse.api.chs.notification.integration.model.EmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.RequestStatus;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationEmailRequestRepository;

@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmailIntegrationTest {

    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:6.0.19"));

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Test
    void shouldSendEmailWithAttachmentFileUpload(@Autowired NotificationEmailRequestRepository notificationEmailRequestRepository,
                                                 @Autowired TestRestTemplate testRestTemplate) {
        // Given
        EmailRequestDao emailRequestDao = emailRequestDaoBuilder()
                .withRandomMockNotifyReference()
                .withPersonalisationDetails(Map.of(
                        "companyName", "Test Company",
                        "companyNumber", "1298749"))
                .build();

        notificationEmailRequestRepository.save(new NotificationEmailRequest(emailRequestDao));

        EmailRequest emailRequest = new EmailRequest(
                emailRequestDao.getSenderDetails().getAppId(),
                emailRequestDao.getSenderDetails().getReference());

        // When
        ResponseEntity<Void> response = testRestTemplate.exchange(postEmailRequestEntity(emailRequest), Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(notificationEmailRequestRepository.findByUniqueReference(
                emailRequestDao.getSenderDetails().getAppId(),
                emailRequestDao.getSenderDetails().getReference()))
                .isPresent()
                .hasValueSatisfying(notificationEmailRequest ->
                        assertThat(notificationEmailRequest.getStatus()).isEqualTo(RequestStatus.SENT));
    }

    @Test
    void shouldReturn404GivenEmailNotFound(@Autowired TestRestTemplate testRestTemplate,
                                           CapturedOutput capturedOutput) {
        // Given
        String appId = "chips";
        String reference = "non-existent-reference";

        // When
        ResponseEntity<Void> response = testRestTemplate.exchange(postEmailRequestEntity(new EmailRequest(appId, reference)), Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(capturedOutput.getOut()).contains("Email request not found in database for request: X9uND6rXQxfbZNcMVFA7JI4h2KOh " +
                "class EmailRequest {\\n    appId: chips\\n    reference: non-existent-reference\\n}`");
    }

    @Test
    void shouldReturnCreatedGivenRequestHasAlreadyBeenProcessed(@Autowired TestRestTemplate testRestTemplate,
                                                                @Autowired NotificationEmailRequestRepository notificationEmailRequestRepository,
                                                                CapturedOutput capturedOutput) {
        // Given
        EmailRequestDao emailRequestDao = emailRequestDaoBuilder()
                .withRandomMockNotifyReference()
                .withPersonalisationDetails(Map.of(
                        "companyName", "Test Company",
                        "companyNumber", "1298749"))
                .build();

        notificationEmailRequestRepository.save(new NotificationEmailRequest(emailRequestDao));

        EmailRequest emailRequest = new EmailRequest(
                emailRequestDao.getSenderDetails().getAppId(),
                emailRequestDao.getSenderDetails().getReference());
        ResponseEntity<Void> firstResponse = testRestTemplate.exchange(postEmailRequestEntity(emailRequest), Void.class);
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // When
        ResponseEntity<Void> secondResponse = testRestTemplate.exchange(postEmailRequestEntity(emailRequest), Void.class);

        // Then
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(capturedOutput.getOut()).contains("Error in chs-gov-uk-notify-integration-api: Email request already sent");
    }

    @Test
    void shouldReturnBadRequestGivenPersonalisationNull(@Autowired TestRestTemplate testRestTemplate,
                                                        @Autowired NotificationEmailRequestRepository notificationEmailRequestRepository,
                                                        CapturedOutput capturedOutput) {
        // Given
        EmailRequestDao emailRequestDao = emailRequestDaoBuilder()
                .withRandomMockNotifyReference()
                .withPersonalisationDetails(null)
                .build();

        notificationEmailRequestRepository.save(new NotificationEmailRequest(emailRequestDao));

        EmailRequest emailRequest = new EmailRequest(
                emailRequestDao.getSenderDetails().getAppId(),
                emailRequestDao.getSenderDetails().getReference());

        // When
        ResponseEntity<Void> response = testRestTemplate.exchange(postEmailRequestEntity(emailRequest), Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(capturedOutput.getOut()).contains("Error in chs-gov-uk-notify-integration-api: " +
                "Request: X9uND6rXQxfbZNcMVFA7JI4h2KOh Failed to publish Welsh dates: Cannot invoke \\\"java.util.Map.keySet()\\\" because \\\"personalisationDetails\\\" is null, action: welsh_dates_error");
    }
}
