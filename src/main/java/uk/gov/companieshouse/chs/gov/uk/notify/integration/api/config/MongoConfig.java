package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
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

    @Value("${spring.data.mongodb.uri}")
    private String databaseUri;

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
        final ConnectionString connectionString =
                new ConnectionString(getDatabaseUri());
        final MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString).build();
        return MongoClients.create(mongoClientSettings);
    }

    @Bean
    MongoTemplate mongoTemplate(MongoDatabaseFactory factory) {
        MongoTemplate mongoTemplate = new MongoTemplate(factory);
        mongoTemplate.setWriteConcern(WriteConcern.ACKNOWLEDGED);
        return mongoTemplate;
    }

    public String getDatabaseUri() {
        return databaseUri;
    }

}
