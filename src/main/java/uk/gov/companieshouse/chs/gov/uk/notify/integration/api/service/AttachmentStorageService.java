package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;


import static com.google.common.base.MoreObjects.firstNonNull;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.config.AwsProperties;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.EmailClientException;

@Service
public class AttachmentStorageService {

    private static final String DEFAULT_ATTACHMENT_FILENAME = "attachment";
    private static final String FILENAME_METADATA_KEY = "filename";

    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    public AttachmentStorageService(S3Client s3Client,
                                    AwsProperties awsProperties) {
        this.s3Client = s3Client;
        this.awsProperties = awsProperties;
    }

    public AttachmentFile getAttachment(String contextId,
                                        String attachmentId) {
        String key = getObjectKey(attachmentId);
        try(ResponseInputStream<GetObjectResponse> attachmentObject = s3Client.getObject(builder -> builder
                .bucket(awsProperties.bucketName())
                .key(key)
                .build())) {
            HeadObjectResponse headObject = getHeadObject(key);

            return new AttachmentFile(
                    firstNonNull(headObject.metadata().get(FILENAME_METADATA_KEY), DEFAULT_ATTACHMENT_FILENAME),
                    attachmentObject.readAllBytes());
        } catch (IOException | S3Exception e) {
            throw new EmailClientException(String.format("Failed to read attachment content from S3 for request: %s attachmentId: %s", contextId, attachmentId), e);
        }
    }

    private String getObjectKey(String attachmentId) {
        String prefix = StringUtils.strip(awsProperties.environmentPrefix(), "/");
        return StringUtils.isBlank(prefix) ? attachmentId : prefix + "/" + attachmentId;
    }

    private HeadObjectResponse getHeadObject(String key) {
        return s3Client.headObject(builder -> builder
                .bucket(awsProperties.bucketName())
                .key(key)
                .build());
    }
}
