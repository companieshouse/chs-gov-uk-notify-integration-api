package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Testcontainers(disabledWithoutDocker = true)
class S3ClientServiceTest {

    private static final String BUCKET_NAME = "test-bucket";

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3"))
            .withServices(S3);

    private S3Client s3Client;
    private S3ClientService s3ClientService;

    @BeforeEach
    void setUp() {
        s3Client = S3Client.builder()
                .endpointOverride(localstack.getEndpoint())
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                .region(Region.of(localstack.getRegion()))
                .build();

        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());

        s3ClientService = new S3ClientService(s3Client, BUCKET_NAME);
    }

    @Test
    void When_FileExists_Expect_BytesReturned() {
        byte[] content = "hello world".getBytes();
        s3Client.putObject(
                PutObjectRequest.builder().bucket(BUCKET_NAME).key("my-file-id").build(),
                RequestBody.fromBytes(content));

        byte[] result = s3ClientService.getFile("my-file-id");

        assertArrayEquals(content, result);
    }

    @Test
    void When_FileDoesNotExist_Expect_RuntimeException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> s3ClientService.getFile("nonexistent-id"));

        assertTrue(ex.getMessage().contains("File not found in S3 with id: nonexistent-id"));
    }
}
