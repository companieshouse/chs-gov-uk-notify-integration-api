package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.service.notify.LetterResponse;


@Document(collection = "responses")
public record NotificationLetterResponse(
        @Id String id,
        @Field("response") LetterResponse response
) {
    // Empty: using only auto-generated methods
}

