package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ApplicationIntegrationTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.config.AwsProperties;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.EmailClientException;


class AttachmentStorageServiceTest extends ApplicationIntegrationTest {

    @Test
    void shouldGetAttachmentContentAndFilename(@Autowired AttachmentStorageService attachmentStorageService,
                                               @Autowired AwsProperties awsProperties,
                                               @Autowired S3Client s3Client) throws Exception {
        // Given
        String attachmentId = RandomStringUtils.randomNumeric(10);
        byte[] attachmentContent = "Attachment content".getBytes();
        s3Client.putObject(builder -> builder
                        .bucket(awsProperties.bucketName())
                        .metadata(Map.of("filename", "test.txt" ))
                        .key(attachmentId).build(),
                RequestBody.fromBytes(attachmentContent));

        // When
        AttachmentFile attachmentFile = attachmentStorageService.getAttachment(
                "x-header-id",
                attachmentId);

        // Then
        assertThat(attachmentFile.getContent()).isEqualTo(attachmentContent);
        assertThat(attachmentFile.getFilename()).isEqualTo("test.txt");
    }

    @Test
    void shouldUseDefaultFilenameGivenMetadataDoesNOTExist(@Autowired AttachmentStorageService attachmentStorageService,
                                                           @Autowired AwsProperties awsProperties,
                                                           @Autowired S3Client s3Client) {
        // Given
        String attachmentId = RandomStringUtils.randomNumeric(10);
        s3Client.putObject(builder -> builder
                        .bucket(awsProperties.bucketName())
                        .key(attachmentId).build(),
                RequestBody.fromBytes(RandomStringUtils.randomAlphanumeric(10).getBytes()));

        // When
        AttachmentFile attachmentFile = attachmentStorageService.getAttachment(
                "x-header-id",
                attachmentId);

        // Then
        assertThat(attachmentFile.getFilename()).isEqualTo("attachment");
    }

    @Test
    void shouldHandleEmptyAttachment(@Autowired AttachmentStorageService attachmentStorageService) {
        // Given
        String attachmentId = RandomStringUtils.randomNumeric(10);

        // When & Then
        assertThatThrownBy(() -> attachmentStorageService.getAttachment(
                "x-header-id",
                attachmentId))
                .isInstanceOf(EmailClientException.class)
                .hasMessage("Failed to read attachment content from S3 for request: x-header-id attachmentId: %s", attachmentId)
                .hasCauseInstanceOf(NoSuchKeyException.class);
    }
}
