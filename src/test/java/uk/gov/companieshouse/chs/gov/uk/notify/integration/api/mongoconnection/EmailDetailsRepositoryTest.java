package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.testcontainers.utility.DockerImageName;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.EmailDetails;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.RecipientDetailsEmail;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.SenderDetails;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
@Tag("integration-test")
public class EmailDetailsRepositoryTest {

    @Autowired
    private EmailDetailsRepository emailDetailsRepository;

    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:5"));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Test
    public void testStoreEmail() {
        // test just checking we can communicate with docker, will be expanded on later
        DatabaseEmailDetails databaseEmailDetails = new DatabaseEmailDetails(
                new SenderDetails("appId", "reference"),
                new RecipientDetailsEmail("tester", "tester@email.com"),
                new EmailDetails("templateId", new BigDecimal(1), "personalisationDetails")
        );

        DatabaseEmailDetails addedDatabaseEmail = emailDetailsRepository.save(databaseEmailDetails);
        assertNotNull(addedDatabaseEmail);
    }
}
