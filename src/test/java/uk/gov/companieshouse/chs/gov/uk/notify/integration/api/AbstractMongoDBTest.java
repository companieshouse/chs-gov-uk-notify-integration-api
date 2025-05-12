package uk.gov.companieshouse.chs.gov.uk.notify.integration.api;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterResponseRepository;

@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractMongoDBTest {

    private static final DockerImageName MONGO_IMAGE = DockerImageName.parse("mongo:6.0.19");

    private static final MongoDBContainer mongoDBContainer;

    static {
        mongoDBContainer = new MongoDBContainer(MONGO_IMAGE)
                .withStartupTimeout(Duration.ofMinutes(2));
        mongoDBContainer.start();
    }

    @Autowired
    private NotificationLetterRequestRepository notificationLetterRequestRepository;

    @Autowired
    private NotificationLetterResponseRepository notificationLetterResponseRepository;

    @DynamicPropertySource
    static void mongoDbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @AfterEach
    void tearDown() {
        notificationLetterRequestRepository.deleteAll();
        notificationLetterResponseRepository.deleteAll();
    }
}
