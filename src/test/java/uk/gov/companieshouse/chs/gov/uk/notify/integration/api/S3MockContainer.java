package uk.gov.companieshouse.chs.gov.uk.notify.integration.api;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class S3MockContainer extends GenericContainer<S3MockContainer> {

    private static final String DEFAULT_ACCESS_KEY_ID = "foo";
    private static final String DEFAULT_SECRET_ACCESS_KEY = "bar";
    private static final String DEFAULT_REGION = "us-east-1";
    private static final int HTTP_PORT = 9090;
    private static final String NOTIFICATION_ATTACHMENTS_LOCAL = "notification-attachments-local";

    public S3MockContainer() {
        super(DockerImageName.parse("adobe/s3mock:5.2.0"));
        withExposedPorts(HTTP_PORT);
        withEnv("COM_ADOBE_TESTING_S3MOCK_STORE_INITIAL_BUCKETS", NOTIFICATION_ATTACHMENTS_LOCAL);
        waitingFor(Wait.forHttp("/favicon.ico").forPort(HTTP_PORT).withMethod("GET").forStatusCode(200));
    }

    public String getS3MockEndpoint() {
        return String.format("http://%s:%d", getHost(), getMappedPort(HTTP_PORT));
    }

    public String getAccessKeyId() {
        return DEFAULT_ACCESS_KEY_ID;
    }

    public String getSecretAccessKey() {
        return DEFAULT_SECRET_ACCESS_KEY;
    }

    public String getRegion() {
        return DEFAULT_REGION;
    }

    public String getBucket() {
        return NOTIFICATION_ATTACHMENTS_LOCAL;
    }

}
