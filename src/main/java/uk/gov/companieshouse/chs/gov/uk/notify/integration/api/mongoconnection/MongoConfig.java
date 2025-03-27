package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Configuration
@EnableMongoRepositories("uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection")
@EnableMongoAuditing(dateTimeProviderRef = "mongodbDatetimeProvider")
class MongoConfig {

    private String mongoDbHost;
    private String mongoDbPort;
    private String mongoDbUserName;
    private String mongoDbPassword;
    private String mongoDbDatabase;

    public MongoConfig(@Value("spring.data.mongodb.host") String mongoDbHost, @Value("spring.data.mongodb.port") String mongoDbPort, @Value("spring.data.mongodb.username") String mongoDbUserName, @Value("spring.data.mongodb.password") String mongoDbPassword, @Value("spring.data.mongodb.database") String mongoDbDatabase) {
        this.mongoDbHost = mongoDbHost;
        this.mongoDbPort = mongoDbPort;
        this.mongoDbUserName = mongoDbUserName;
        this.mongoDbPassword = mongoDbPassword;
        this.mongoDbDatabase = mongoDbDatabase;
    }

    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener(final LocalValidatorFactoryBean factory) {
        return new ValidatingMongoEventListener(factory);
    }

    @Bean(name = "mongodbDatetimeProvider")
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now());
    }

    @Bean()
    public MongoClient mongoClient() {
        MongoCredential credential = MongoCredential.createCredential(mongoDbUserName, mongoDbDatabase, mongoDbPassword.toCharArray());

        return MongoClients.create(MongoClientSettings.builder()
                                                      .applyToClusterSettings(builder -> builder.hosts(Collections.singletonList(new ServerAddress(mongoDbHost + ":" + mongoDbPort))))
                                                      .credential(credential).build());
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory() {
        return new SimpleMongoClientDatabaseFactory(mongoClient(), mongoDbDatabase);
    }

}
