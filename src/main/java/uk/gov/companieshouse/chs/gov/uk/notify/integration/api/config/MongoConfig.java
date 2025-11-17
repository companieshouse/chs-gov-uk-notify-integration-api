package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.config;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter.DateToOffsetDateTimeConverter;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter.DocumentToLetterResponseConverter;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter.DocumentToSendEmailResponseConverter;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter.OffsetDateTimeToDateConverter;

@Configuration
@EnableMongoRepositories("uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository")
@EnableMongoAuditing(dateTimeProviderRef = "mongodbDatetimeProvider")
@EnableTransactionManagement
public class MongoConfig {

    @Bean( name = "mongodbDatetimeProvider" )
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of( LocalDateTime.now() );
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
