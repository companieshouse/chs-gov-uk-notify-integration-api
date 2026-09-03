package uk.gov.companieshouse.chs.gov.uk.notify.integration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.postEmailRequestEntity;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDaoBuilder.emailRequestDaoBuilder;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationIntegrationTest {

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

}
