package uk.gov.companieshouse.chs.gov.uk.notify.integration.api;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.postEmailRequestEntity;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailDetailsBuilder.emailDetailsBuilder;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDaoBuilder.emailRequestDaoBuilder;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.SenderDetailsBuilder.senderDetailsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import uk.gov.companieshouse.api.chs.notification.integration.model.EmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.config.AwsProperties;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.RequestStatus;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationEmailRequestRepository;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class EmailIntegrationTest extends ApplicationIntegrationTest{

    @MockitoBean
    private NotificationClient notificationClient;

    @Captor
    private ArgumentCaptor<Map<String, Object>> personalisationDetailsCaptor;

    @Test
    void shouldSendEmail(@Autowired NotificationEmailRequestRepository notificationEmailRequestRepository,
                         @Autowired TestRestTemplate testRestTemplate) throws Exception {
        // Given
        EmailRequestDao emailRequestDao = emailRequestDaoBuilder()
                .withSenderDetails(senderDetailsBuilder()
                        .withAppId("chips")
                        .withReference(UUID.randomUUID().toString())
                        .build())
                .build();

        notificationEmailRequestRepository.save(new NotificationEmailRequest(emailRequestDao));

        EmailRequest emailRequest = new EmailRequest(
                emailRequestDao.getSenderDetails().getAppId(),
                emailRequestDao.getSenderDetails().getReference());

        givenSentEmailIsSuccessful(emailRequestDao);

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
    void shouldSendEmailWithAttachment(@Autowired NotificationEmailRequestRepository notificationEmailRequestRepository,
                                       @Autowired TestRestTemplate testRestTemplate,
                                       @Autowired AwsProperties awsProperties,
                                       @Autowired S3Client s3Client) throws Exception {
        // Given
        String reference = UUID.randomUUID().toString();
        EmailRequestDao emailRequestDao = emailRequestDaoBuilder()
                .withSenderDetails(senderDetailsBuilder()
                        .withAppId("chips")
                        .withReference(reference)
                        .build())
                .withEmailDetails(emailDetailsBuilder()
                        .withAttachmentId(reference)
                        .build())
                .build();

        notificationEmailRequestRepository.save(new NotificationEmailRequest(emailRequestDao));

        ClassPathResource attachmentResource = givenAttachmentIsStoredInS3(awsProperties, s3Client, emailRequestDao);

        EmailRequest emailRequest = new EmailRequest(
                emailRequestDao.getSenderDetails().getAppId(),
                emailRequestDao.getSenderDetails().getReference());

        given(notificationClient.sendEmail(
                eq(emailRequestDao.getEmailDetails().getTemplateId()),
                eq(emailRequestDao.getRecipientDetails().getEmailAddress()),
                anyMap(),
                eq(emailRequestDao.getSenderDetails().getReference())))
                .willReturn(successfulResponseFor(emailRequestDao));

        // When
        ResponseEntity<Void> response = testRestTemplate.exchange(postEmailRequestEntity(emailRequest), Void.class);

        // Then
        String expectedContentBytes = attachmentResource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(notificationEmailRequestRepository.findByUniqueReference(
                emailRequestDao.getSenderDetails().getAppId(),
                emailRequestDao.getSenderDetails().getReference()))
                .isPresent()
                .hasValueSatisfying(notificationEmailRequest ->
                        assertThat(notificationEmailRequest.getStatus()).isEqualTo(RequestStatus.SENT));
        then(notificationClient).should().sendEmail(
                eq(emailRequestDao.getEmailDetails().getTemplateId()),
                eq(emailRequestDao.getRecipientDetails().getEmailAddress()),
                personalisationDetailsCaptor.capture(),
                eq(emailRequestDao.getSenderDetails().getReference()));

        Map<String, Object> personalisationDetails = personalisationDetailsCaptor.getValue();
        assertThat(personalisationDetails)
                .hasEntrySatisfying("file_download_link", value -> assertThat(value)
                        .isInstanceOfSatisfying(JSONObject.class,
                                json -> {
                                    assertThat(json.get("file")).isEqualTo(Base64.getEncoder().encodeToString(expectedContentBytes.getBytes()));
                                    assertThat(json.get("filename")).isEqualTo(attachmentResource.getFilename());
                                    assertThat(json.get("confirm_email_before_download")).isEqualTo(JSONObject.NULL);
                                    assertThat(json.get("retention_period")).isEqualTo(JSONObject.NULL);
                                }));
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
                                                                CapturedOutput capturedOutput) throws Exception {
        // Given
        EmailRequestDao emailRequestDao = emailRequestDaoBuilder().build();

        notificationEmailRequestRepository.save(new NotificationEmailRequest(emailRequestDao));

        givenSentEmailIsSuccessful(emailRequestDao);

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
                .withEmailDetails(emailDetailsBuilder()
                        .withPersonalisationDetails(null)
                        .build())
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

    @Test
    void shouldReturnBadRequestGivenAppIdIsNull(@Autowired TestRestTemplate testRestTemplate,
                                                @Autowired NotificationEmailRequestRepository notificationEmailRequestRepository) {
        // Given
        EmailRequestDao emailRequestDao = emailRequestDaoBuilder()
                .withSenderDetails(senderDetailsBuilder()
                        .withAppId(null)
                        .build())
                .build();

        notificationEmailRequestRepository.save(new NotificationEmailRequest(emailRequestDao));

        EmailRequest emailRequest = new EmailRequest(
                emailRequestDao.getSenderDetails().getAppId(),
                emailRequestDao.getSenderDetails().getReference());

        // When
        ResponseEntity<Void> response = testRestTemplate.exchange(postEmailRequestEntity(emailRequest), Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnBadRequestGivenReferenceIsNull(@Autowired TestRestTemplate testRestTemplate,
                                                    @Autowired NotificationEmailRequestRepository notificationEmailRequestRepository) {
        // Given
        EmailRequestDao emailRequestDao = emailRequestDaoBuilder()
                .withSenderDetails(senderDetailsBuilder()
                        .withReference(null)
                        .build())
                .build();

        notificationEmailRequestRepository.save(new NotificationEmailRequest(emailRequestDao));

        EmailRequest emailRequest = new EmailRequest(
                emailRequestDao.getSenderDetails().getAppId(),
                emailRequestDao.getSenderDetails().getReference());

        // When
        ResponseEntity<Void> response = testRestTemplate.exchange(postEmailRequestEntity(emailRequest), Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnInternalServerErrorGivenExceptionThrownFromNotificationClient(@Autowired TestRestTemplate testRestTemplate,
                                                                                   @Autowired NotificationEmailRequestRepository notificationEmailRequestRepository) throws Exception {
        // Given
        EmailRequestDao emailRequestDao = emailRequestDaoBuilder().build();

        notificationEmailRequestRepository.save(new NotificationEmailRequest(emailRequestDao));

        EmailRequest emailRequest = new EmailRequest(
                emailRequestDao.getSenderDetails().getAppId(),
                emailRequestDao.getSenderDetails().getReference());

        given(notificationClient.sendEmail(
                emailRequestDao.getEmailDetails().getTemplateId(),
                emailRequestDao.getRecipientDetails().getEmailAddress(),
                emailRequestDao.getEmailDetails().getPersonalisationDetails(),
                emailRequestDao.getSenderDetails().getReference()))
                .willThrow(new NotificationClientException("Notification client exception"));

        // When
        ResponseEntity<Void> response = testRestTemplate.exchange(postEmailRequestEntity(emailRequest), Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void proveValidationAnnotationsNotWorkingForEmailAddress(@Autowired TestRestTemplate testRestTemplate,
                                                             @Autowired NotificationEmailRequestRepository notificationEmailRequestRepository) throws Exception {
        // Given
        EmailRequestDao emailRequestDao = emailRequestDaoBuilder()
                .withSenderDetails(senderDetailsBuilder()
                        .withEmailAddress("invalid-email-address")
                        .build())
                .build();

        notificationEmailRequestRepository.save(new NotificationEmailRequest(emailRequestDao));

        givenSentEmailIsSuccessful(emailRequestDao);

        EmailRequest emailRequest = new EmailRequest(
                emailRequestDao.getSenderDetails().getAppId(),
                emailRequestDao.getSenderDetails().getReference());

        // When
        ResponseEntity<Void> response = testRestTemplate.exchange(postEmailRequestEntity(emailRequest), Void.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private void givenSentEmailIsSuccessful(EmailRequestDao emailRequestDao) throws NotificationClientException {
        given(notificationClient.sendEmail(
                emailRequestDao.getEmailDetails().getTemplateId(),
                emailRequestDao.getRecipientDetails().getEmailAddress(),
                emailRequestDao.getEmailDetails().getPersonalisationDetails(),
                emailRequestDao.getSenderDetails().getReference()))
                .willReturn(successfulResponseFor(emailRequestDao));
    }

    private ClassPathResource givenAttachmentIsStoredInS3(AwsProperties awsProperties, S3Client s3Client, EmailRequestDao emailRequestDao) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("attachments/attachment.txt");
        String attachmentContentType = Files.probeContentType(classPathResource.getFile().toPath());
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(awsProperties.bucketName())
                        .key(emailRequestDao.getSenderDetails().getReference())
                        .contentEncoding(attachmentContentType)
                        .metadata(Map.of(
                                "content-type", attachmentContentType,
                                "filename", classPathResource.getFilename()))
                        .build(),
                RequestBody.fromFile(classPathResource.getFile()));
        return classPathResource;
    }

    private SendEmailResponse successfulResponseFor(EmailRequestDao emailRequestDao) {
        return new SendEmailResponse(format("""
                        {
                            "id": "%s",
                            "reference": "%s",
                            "content": {
                                "subject": "Test Email Subject",
                                "body": "Test Email Body"
                            },
                            "template": {
                                "id": "%s",
                                "version": 1,
                                "uri": "https://api.notifications.service.gov.uk/v2/template/%s"
                            }
                        }""",
                UUID.randomUUID(),
                emailRequestDao.getSenderDetails().getReference(),
                emailRequestDao.getEmailDetails().getTemplateId(),
                emailRequestDao.getEmailDetails().getTemplateId()));
    }
}
