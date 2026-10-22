package uk.gov.companieshouse.chs.gov.uk.notify.integration.api;


import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

@ExtendWith(SystemStubsExtension.class)
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ApplicationIntegrationTest {

    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:6.0.19"));

    @Container
    private static final S3MockContainer s3Mock = new S3MockContainer();

    @SystemStub
    static final SystemProperties awsCredentials = new SystemProperties(
            "aws.accessKeyId", s3Mock.getAccessKeyId(),
            "aws.secretAccessKey", s3Mock.getSecretAccessKey());

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("chs.notification.aws.s3-endpoint", s3Mock::getS3MockEndpoint);
        registry.add("chs.notification.aws.region", s3Mock::getRegion);
        registry.add("chs.notification.aws.bucket-name", s3Mock::getBucket);
        registry.add("chs.notification.aws.path-style-access-enabled", () -> true);
    }

}
