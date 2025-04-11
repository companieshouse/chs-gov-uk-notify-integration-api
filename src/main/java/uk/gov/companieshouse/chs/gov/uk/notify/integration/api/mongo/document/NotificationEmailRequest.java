package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;

@Document(collection = "email_details")
public record NotificationEmailRequest(
        @Id String id,
        @Field("request") GovUkEmailDetailsRequest request
) {
    // Empty: using only auto-generated methods
}
