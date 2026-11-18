package uk.gov.companieshouse.chs.gov.uk.notify.integration.api;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationEmailRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationEmailResponseRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterResponseRepository;

@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractMongoDBTest {

    private static final DockerImageName MONGO_IMAGE = DockerImageName.parse("mongo:8.0.34");

    @ServiceConnection
    protected static final org.testcontainers.mongodb.MongoDBContainer mongoDBContainer =
            new MongoDBContainer(MONGO_IMAGE)
                    .withStartupTimeout(Duration.ofMinutes(2));

    static {
        mongoDBContainer.start();
    }

    @Autowired
    protected NotificationLetterRequestRepository notificationLetterRequestRepository;

    @Autowired
    protected NotificationLetterResponseRepository notificationLetterResponseRepository;

    @Autowired
    protected NotificationEmailRequestRepository notificationEmailRequestRepository;

    @Autowired
    protected NotificationEmailResponseRepository notificationEmailResponseRepository;

    @DynamicPropertySource
    static void mongoDbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @AfterEach
    void tearDown() {
        notificationLetterRequestRepository.deleteAll();
        notificationLetterResponseRepository.deleteAll();
        notificationEmailRequestRepository.deleteAll();
        notificationEmailResponseRepository.deleteAll();
    }
}
