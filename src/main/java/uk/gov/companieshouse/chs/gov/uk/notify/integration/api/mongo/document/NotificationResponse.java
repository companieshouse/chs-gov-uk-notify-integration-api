package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.service.notify.SendEmailResponse;


@Document(collection = "responses")
public record NotificationResponse(
        @Id String id,
        @Field("requestId") String requestId,
        @Field("response") SendEmailResponse response
) {}

