package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;

@Service
public class S3ClientService {

    private final S3Client s3Client;
    private final String bucketName;

    public S3ClientService(
            S3Client s3Client,
            @Value("${aws.s3.bucket-name}") String bucketName
    ) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public byte[] getFile(String fileId) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileId)
                .build();

        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
            return response.readAllBytes();
        } catch (NoSuchKeyException e) {
            throw new RuntimeException("File not found in S3 with id: " + fileId, e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file from S3 with id: " + fileId, e);
        }
    }
}
