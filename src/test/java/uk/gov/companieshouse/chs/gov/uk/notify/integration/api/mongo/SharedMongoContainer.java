package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

public class SharedMongoContainer {
    private static final DockerImageName MONGO_IMAGE = DockerImageName.parse("mongo:6.0.19");
    private static final MongoDBContainer INSTANCE = new MongoDBContainer(MONGO_IMAGE);

    static {
        INSTANCE.start();
        System.setProperty("spring.data.mongodb.uri", INSTANCE.getReplicaSetUrl());
    }

    public static MongoDBContainer getInstance() {
        return INSTANCE;
    }

    private void SharedMongoDBContainer() {
        // private constructor, to prevent instantiation
    }
}
