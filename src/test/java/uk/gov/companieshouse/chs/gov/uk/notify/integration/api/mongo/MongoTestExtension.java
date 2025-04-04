package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.MongoDBContainer;

public class MongoTestExtension implements BeforeAllCallback {
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0.19");

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!mongoDBContainer.isRunning()) {
            mongoDBContainer.start();
            System.setProperty("spring.data.mongodb.uri", mongoDBContainer.getReplicaSetUrl());
        }
    }
}
