package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;

@Document(collection = "letter_details")
public record NotificationLetterRequest(
        @Id String id,
        @Field("request") GovUkLetterDetailsRequest request
) {
    // Empty: using only auto-generated methods
}
