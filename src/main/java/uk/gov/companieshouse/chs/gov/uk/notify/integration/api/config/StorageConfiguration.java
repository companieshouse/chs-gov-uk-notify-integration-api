package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class StorageConfiguration {

    @Bean
    public S3Client s3Client(AwsProperties awsProperties) {
        return S3Client.builder()
                .endpointOverride(awsProperties.s3Endpoint())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(awsProperties.pathStyleAccessEnabled())
                        .build())
                .region(Region.of(awsProperties.region()))
                .credentialsProvider(() -> AwsBasicCredentials.create(awsProperties.accessKeyId(), awsProperties.secretAccessKey()))
                .build();
    }

}
