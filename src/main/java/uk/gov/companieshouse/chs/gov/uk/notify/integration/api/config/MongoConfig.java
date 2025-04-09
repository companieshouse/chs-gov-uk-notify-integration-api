package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.config;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.UuidRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter.DateToOffsetDateTimeConverter;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter.DocumentToLetterResponseConverter;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter.DocumentToSendEmailResponseConverter;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter.OffsetDateTimeToDateConverter;

@Configuration
@EnableMongoRepositories("uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository")
@EnableMongoAuditing(dateTimeProviderRef = "mongodbDatetimeProvider")
public class MongoConfig {

    private final String databaseUri;

    public MongoConfig(@Value("${spring.data.mongodb.uri}") String databaseUri) {
        this.databaseUri = databaseUri;
    }

    @Bean(name = "mongodbDatetimeProvider")
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now());
    }

    @Bean
    public MongoClient mongoClient() {
        final ConnectionString connectionString =
                new ConnectionString(databaseUri);
        final MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build();
        return MongoClients.create(mongoClientSettings);
    }

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new DateToOffsetDateTimeConverter(),
                new OffsetDateTimeToDateConverter(),
                new DocumentToSendEmailResponseConverter(),
                new DocumentToLetterResponseConverter()
        ));
    }
    
}
